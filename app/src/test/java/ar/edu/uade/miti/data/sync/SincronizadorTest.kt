package ar.edu.uade.miti.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RutinaEntity
import ar.edu.uade.miti.data.remote.CLAVE_DE_PRUEBA
import ar.edu.uade.miti.data.remote.ClavePublicableInterceptor
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.FakeHogarActivo
import ar.edu.uade.miti.data.remote.FakeSesionLocal
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.SyncApiService
import ar.edu.uade.miti.data.remote.authApiContra
import ar.edu.uade.miti.domain.model.Frecuencia
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SincronizadorTest {

    private class FakeEstadoDeSync : EstadoDeSyncLocal {
        val cursores = mutableMapOf<String, String>()
        override suspend fun cursor(tabla: String) = cursores[tabla]
        override suspend fun guardarCursor(tabla: String, valor: String) { cursores[tabla] = valor }
        override val ultimaSincronizacion = MutableStateFlow<Long?>(null)
        override suspend fun registrarSincronizacion(momento: Long) { ultimaSincronizacion.value = momento }
        override suspend fun borrar() { cursores.clear(); ultimaSincronizacion.value = null }
    }

    /** Servidor falso: responde según método y tabla; por defecto, listas vacías y subidas aceptadas. */
    private inner class ServidorFalso : Dispatcher() {
        val respuestasGet = mutableMapOf<String, ArrayDeque<MockResponse>>()
        val respuestasPost = mutableMapOf<String, (String) -> MockResponse>()
        val pedidos = mutableListOf<RecordedRequest>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            pedidos += request
            val tabla = request.url.encodedPath.removePrefix("/rest/v1/")
            return when (request.method) {
                "GET" -> respuestasGet[tabla]?.removeFirstOrNull() ?: MockResponse(code = 200, body = "[]")
                "POST" -> respuestasPost[tabla]?.invoke(request.body?.utf8().orEmpty()) ?: MockResponse(code = 201)
                else -> MockResponse(code = 405)
            }
        }

        fun cuandoPidan(tabla: String, vararg respuestas: MockResponse) {
            respuestasGet.getOrPut(tabla) { ArrayDeque() }.addAll(respuestas)
        }

        fun posts(tabla: String) = pedidos.filter { it.method == "POST" && it.url.encodedPath == "/rest/v1/$tabla" }
        fun gets(tabla: String) = pedidos.filter { it.method == "GET" && it.url.encodedPath == "/rest/v1/$tabla" }
    }

    private val ahora = 1_800_000_000_000L
    private val reloj = Clock.fixed(Instant.ofEpochMilli(ahora), ZoneOffset.UTC)
    private val servidor = MockWebServer()
    private val falso = ServidorFalso()
    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val sesion = FakeSesionLocal(SesionGuardada("u-ana", "ana@mail.com", "token-ana", "refresh", ahora + 3_600_000))
    private val hogarActivo = FakeHogarActivo("h1")
    private val estado = FakeEstadoDeSync()
    private lateinit var sincronizador: Sincronizador

    @Before
    fun arrancar() {
        servidor.dispatcher = falso
        servidor.start()
        val moshi = Moshi.Builder().build()
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(ClavePublicableInterceptor(CLAVE_DE_PRUEBA))
                    .readTimeout(2, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi).withNullSerialization())
            .build()
            .create(SyncApiService::class.java)
        val config = ConfiguracionSupabase(servidor.url("/").toString(), CLAVE_DE_PRUEBA)
        val proveedor = ProveedorDeToken(authApiContra(servidor), sesion, reloj)
        sincronizador = Sincronizador(api, config, proveedor, sesion, hogarActivo, estado, base, reloj)
    }

    @After
    fun cerrar() {
        base.close()
        servidor.close()
    }

    private fun rutina(id: String, titulo: String, updatedAt: Long, estado: EstadoSync, deleted: Boolean = false) =
        RutinaEntity(id, "h1", titulo, Frecuencia.DIARIA, emptySet(), 2, MetadatosSync(updatedAt, deleted, estado))

    private fun rutinaJson(id: String, titulo: String, updatedAt: Long, server: String, deleted: Boolean = false) =
        """{"id":"$id","hogar_id":"h1","titulo":"$titulo","frecuencia":"DIARIA","dias":"","veces_por_dia":2,""" +
            """"updated_at":$updatedAt,"deleted":$deleted,"server_updated_at":"$server"}"""

    @Test
    fun `sin sesion no toca la red`() = runTest {
        sesion.borrar()

        assertEquals(ResultadoDeSync.SinSesion, sincronizador.sincronizar())
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `sube lo pendiente con el token, en snake_case y lo marca sincronizado`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Pancho", 100, EstadoSync.PENDIENTE))

        val resultado = sincronizador.sincronizar()

        assertEquals(ResultadoDeSync.Completa(subidas = 1, descargadas = 0, rechazadas = 0), resultado)
        val post = falso.posts("rutinas").single()
        assertEquals("Bearer token-ana", post.headers["Authorization"])
        assertEquals(CLAVE_DE_PRUEBA, post.headers["apikey"])
        assertTrue(post.headers["Prefer"]!!.contains("merge-duplicates"))
        assertEquals("id,hogar_id,titulo,frecuencia,dias,veces_por_dia,updated_at,deleted", post.url.queryParameter("columns"))
        val cuerpo = post.body!!.utf8()
        assertTrue(cuerpo, cuerpo.contains("\"veces_por_dia\":2") && cuerpo.contains("\"hogar_id\":\"h1\""))
        assertEquals(EstadoSync.SINCRONIZADO, base.rutinaDao().rutinasParaSync("r1")!!.sync.syncState)
        assertEquals(ahora, estado.ultimaSincronizacion.value)
    }

    @Test
    fun `los nulls viajan explicitos para que el servidor no ignore la columna`() = runTest {
        base.pendienteDao().guardarPendientes(
            listOf(
                ar.edu.uade.miti.data.local.entity.PendienteEntity(
                    "c1", "h1", ar.edu.uade.miti.domain.model.TipoPendiente.COMPRA, "Leche", null, null,
                    avisar = false, enCarrito = false, resuelto = false, gastoId = null,
                    sync = MetadatosSync(100, false, EstadoSync.PENDIENTE),
                ),
            ),
        )

        sincronizador.sincronizar()

        val cuerpo = falso.posts("pendientes").single().body!!.utf8()
        assertTrue(cuerpo, cuerpo.contains("\"gasto_id\":null") && cuerpo.contains("\"responsable_id\":null"))
    }

    @Test
    fun `descarga lo nuevo, lo guarda sincronizado y avanza el cursor`() = runTest {
        falso.cuandoPidan(
            "rutinas",
            MockResponse(code = 200, body = "[" + rutinaJson("r9", "Regar", 300, "2026-11-21T20:00:00.5+00:00") + "]"),
        )

        val resultado = sincronizador.sincronizar() as ResultadoDeSync.Completa

        assertEquals(1, resultado.descargadas)
        val guardada = base.rutinaDao().rutinasParaSync("r9")!!
        assertEquals("Regar", guardada.titulo)
        assertEquals(EstadoSync.SINCRONIZADO, guardada.sync.syncState)
        assertEquals("2026-11-21T20:00:00.5+00:00", estado.cursores["rutinas"])
        assertEquals("gte.1970-01-01T00:00:00Z", falso.gets("rutinas").single().url.queryParameter("server_updated_at"))
    }

    @Test
    fun `la siguiente descarga pide desde el cursor con un minuto de margen`() = runTest {
        estado.cursores["rutinas"] = "2026-11-21T20:00:00Z"

        sincronizador.sincronizar()

        assertEquals("gte.2026-11-21T19:59Z", falso.gets("rutinas").single().url.queryParameter("server_updated_at"))
    }

    @Test
    fun `una version remota mas vieja no pisa un cambio local mas nuevo`() = runTest {
        // La subida falla por datos, así que el cambio local queda PENDIENTE; la descarga trae una versión vieja.
        base.rutinaDao().guardarRutina(rutina("r1", "Local nueva", 500, EstadoSync.PENDIENTE))
        falso.respuestasPost["rutinas"] = { MockResponse(code = 400, body = """{"code":"23514"}""") }
        falso.cuandoPidan("rutinas", MockResponse(code = 200, body = "[" + rutinaJson("r1", "Remota vieja", 100, "2026-11-21T20:00:00Z") + "]"))

        val resultado = sincronizador.sincronizar() as ResultadoDeSync.Completa

        assertEquals(1, resultado.rechazadas)
        val local = base.rutinaDao().rutinasParaSync("r1")!!
        assertEquals("Local nueva", local.titulo)
        assertEquals(EstadoSync.PENDIENTE, local.sync.syncState)
    }

    @Test
    fun `una version remota mas nueva pisa la local, incluido un borrado`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Pancho", 100, EstadoSync.SINCRONIZADO))
        falso.cuandoPidan(
            "rutinas",
            MockResponse(code = 200, body = "[" + rutinaJson("r1", "Pancho", 200, "2026-11-21T20:00:00Z", deleted = true) + "]"),
        )

        sincronizador.sincronizar()

        assertTrue(base.rutinaDao().rutinasParaSync("r1")!!.sync.deleted)
    }

    @Test
    fun `una fila rechazada por el servidor no frena a las demas`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Buena", 100, EstadoSync.PENDIENTE))
        base.rutinaDao().guardarRutina(rutina("r2", "Mala", 100, EstadoSync.PENDIENTE))
        falso.respuestasPost["rutinas"] = { cuerpo ->
            if (cuerpo.contains("\"r2\"")) MockResponse(code = 400, body = """{"code":"23514"}""") else MockResponse(code = 201)
        }

        val resultado = sincronizador.sincronizar() as ResultadoDeSync.Completa

        assertEquals(1, resultado.subidas)
        assertEquals(1, resultado.rechazadas)
        assertEquals(EstadoSync.SINCRONIZADO, base.rutinaDao().rutinasParaSync("r1")!!.sync.syncState)
        assertEquals(EstadoSync.PENDIENTE, base.rutinaDao().rutinasParaSync("r2")!!.sync.syncState)
    }

    @Test
    fun `si el servidor falla, lo pendiente queda para reintentar`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Pancho", 100, EstadoSync.PENDIENTE))
        falso.respuestasPost["rutinas"] = { MockResponse(code = 503) }

        val resultado = sincronizador.sincronizar()

        assertTrue(resultado is ResultadoDeSync.Reintentar)
        assertEquals(EstadoSync.PENDIENTE, base.rutinaDao().rutinasParaSync("r1")!!.sync.syncState)
        assertNull(estado.ultimaSincronizacion.value)
    }

    @Test
    fun `sin conexion pide reintentar`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Pancho", 100, EstadoSync.PENDIENTE))
        servidor.close()

        assertEquals(ResultadoDeSync.Reintentar("Sin conexión"), sincronizador.sincronizar())
        assertEquals(EstadoSync.PENDIENTE, base.rutinaDao().rutinasParaSync("r1")!!.sync.syncState)
    }

    @Test
    fun `un cambio hecho mientras se subia queda pendiente para la proxima`() = runTest {
        base.rutinaDao().guardarRutina(rutina("r1", "Pancho", 100, EstadoSync.PENDIENTE))
        falso.respuestasPost["rutinas"] = {
            // Mientras viaja la subida, la persona edita la rutina otra vez.
            kotlinx.coroutines.runBlocking { base.rutinaDao().guardarRutina(rutina("r1", "Pancho editada", 150, EstadoSync.PENDIENTE)) }
            MockResponse(code = 201)
        }

        sincronizador.sincronizar()

        val local = base.rutinaDao().rutinasParaSync("r1")!!
        assertEquals("Pancho editada", local.titulo)
        assertEquals(EstadoSync.PENDIENTE, local.sync.syncState)
    }

    @Test
    fun `pagina cuando hay mas filas que el limite`() = runTest {
        sincronizador.limite = 2
        falso.cuandoPidan(
            "rutinas",
            MockResponse(code = 200, body = "[" + rutinaJson("r1", "A", 1, "2026-11-21T20:00:01Z") + "," + rutinaJson("r2", "B", 1, "2026-11-21T20:00:02Z") + "]"),
            MockResponse(code = 200, body = "[" + rutinaJson("r3", "C", 1, "2026-11-21T20:00:03Z") + "]"),
        )

        val resultado = sincronizador.sincronizar() as ResultadoDeSync.Completa

        assertEquals(3, resultado.descargadas)
        assertEquals(listOf("0", "2"), falso.gets("rutinas").map { it.url.queryParameter("offset") })
        assertEquals("2026-11-21T20:00:03Z", estado.cursores["rutinas"])
    }

    @Test
    fun `sin hogar activo lo busca en el servidor`() = runTest {
        hogarActivo.establecer(null)
        falso.cuandoPidan(
            "integrantes",
            MockResponse(code = 200, body = """[{"id":"u-ana","hogar_id":"h7","nombre":"Ana","email":"ana@mail.com","updated_at":1}]"""),
        )

        sincronizador.sincronizar()

        assertEquals("h7", hogarActivo.id.value)
        assertEquals("eq.u-ana", falso.gets("integrantes").first().url.queryParameter("id"))
    }

    @Test
    fun `si todavia no tiene hogar no sigue`() = runTest {
        hogarActivo.establecer(null)

        assertEquals(ResultadoDeSync.SinHogar, sincronizador.sincronizar())
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `el worker reintenta solo cuando tiene sentido`() {
        assertEquals(androidx.work.ListenableWorker.Result.success(), SyncWorker.resultadoPara(ResultadoDeSync.SinSesion, 0))
        assertEquals(androidx.work.ListenableWorker.Result.retry(), SyncWorker.resultadoPara(ResultadoDeSync.Reintentar("x"), 0))
        assertEquals(
            androidx.work.ListenableWorker.Result.failure(),
            SyncWorker.resultadoPara(ResultadoDeSync.Reintentar("x"), SyncWorker.MAXIMO_DE_INTENTOS),
        )
    }

    @Test
    fun `sube la version base de un gasto editado`() = runTest {
        base.gastoDao().guardarGasto(
            ar.edu.uade.miti.data.local.entity.GastoEntity(
                "g1", "h1", "Heladera", 100, "u-ana", ar.edu.uade.miti.domain.model.Reparto.A_MEDIAS, null, 1, 1,
                java.time.YearMonth.of(2026, 11), MetadatosSync(900, false, EstadoSync.PENDIENTE), baseUpdatedAt = 500,
            ),
        )

        sincronizador.sincronizar()

        val post = falso.posts("gastos").single()
        assertTrue(post.body!!.utf8().contains("\"base_updated_at\":500"))
        assertTrue(post.url.queryParameter("columns")!!.endsWith(",base_updated_at"))
    }

    @Test
    fun `baja los conflictos propios y uno resuelto no se reabre al volver a bajarlo`() = runTest {
        val conflicto = """[{"id":"c1","gasto_id":"g1","updated_at":800,"deleted":false,"server_updated_at":"2026-11-23T12:00:00Z",""" +
            """"version_rechazada":{"id":"g1","hogar_id":"h1","descripcion":"Heladera","monto_total_centavos":2900000,""" +
            """"pagador_id":"u-ana","reparto":"A_MEDIAS","beneficiario_id":null,"cuotas_totales":1,"cuota_inicial":1,""" +
            """"mes_inicio":"2026-11","updated_at":5000,"deleted":false}}]"""
        falso.cuandoPidan("conflictos_gasto", MockResponse(code = 200, body = conflicto), MockResponse(code = 200, body = conflicto))

        sincronizador.sincronizar()
        val guardado = base.conflictoGastoDao().conflictosParaSync("c1")!!
        assertEquals(2_900_000L, guardado.montoTotalCentavos)

        base.conflictoGastoDao().marcarResuelto("c1", ahora)
        sincronizador.sincronizar()

        assertTrue(base.conflictoGastoDao().conflictosParaSync("c1")!!.resuelto)
    }
}
