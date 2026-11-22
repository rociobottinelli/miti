package ar.edu.uade.miti.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.remote.CLAVE_DE_PRUEBA
import ar.edu.uade.miti.data.remote.CODIGO_INVALIDO
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.FakeHogarActivo
import ar.edu.uade.miti.data.remote.FakeSesionLocal
import ar.edu.uade.miti.data.remote.HOGAR_COMPLETO
import ar.edu.uade.miti.data.remote.HogarApiService
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.SESION_VENCIDA
import ar.edu.uade.miti.data.remote.SIN_CONEXION_HOGAR
import ar.edu.uade.miti.data.remote.SyncApiService
import ar.edu.uade.miti.data.remote.YA_TENES_HOGAR
import ar.edu.uade.miti.data.remote.authApiContra
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.domain.model.Hogar
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
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
class IngresoAHogarRepositoryImplTest {

    /** Responde según la ruta; guarda los pedidos para revisar qué se mandó. */
    private class ServidorFalso : Dispatcher() {
        val respuestas = mutableMapOf<String, MockResponse>()
        val pedidos = mutableListOf<RecordedRequest>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            pedidos += request
            return respuestas[request.url.encodedPath] ?: MockResponse(code = 404)
        }

        fun cuerpoDe(ruta: String) = pedidos.single { it.url.encodedPath == ruta }.body?.utf8()
    }

    private val ahora = 1_800_000_000_000L
    private val reloj = Clock.fixed(Instant.ofEpochMilli(ahora), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().build()
    private val servidor = MockWebServer()
    private val falso = ServidorFalso()
    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val sesion = FakeSesionLocal(SesionGuardada("u-ana", "ana@mail.com", "token-ana", "refresh", ahora + 3_600_000))
    private val hogarActivo = FakeHogarActivo(inicial = null)
    private var sincronizacionesPedidas = 0

    private val hogarJson = """{"id":"h1","nombre":"Nuestra casa","codigo_union":"K7P2QX","updated_at":$ahora,"deleted":false}"""

    @Before
    fun arrancar() {
        servidor.dispatcher = falso
        servidor.start()
        falso.respuestas["/auth/v1/user"] =
            MockResponse(code = 200, body = """{"id":"u-ana","email":"ana@mail.com","user_metadata":{"nombre":"Ana"}}""")
    }

    @After
    fun parar() {
        servidor.close()
        base.close()
    }

    private fun repositorio(): IngresoAHogarRepositoryImpl {
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        val authApi = authApiContra(servidor, moshi)
        return IngresoAHogarRepositoryImpl(
            api = retrofit.create(HogarApiService::class.java),
            syncApi = retrofit.create(SyncApiService::class.java),
            configuracion = ConfiguracionSupabase(servidor.url("/").toString(), CLAVE_DE_PRUEBA),
            proveedorDeToken = ProveedorDeToken(authApi, sesion, reloj),
            dao = base.hogarDao(),
            hogarActivo = hogarActivo,
            avisoDeCambios = AvisoDeCambios { sincronizacionesPedidas++ },
            moshi = moshi,
        )
    }

    private fun errorDelServidor(codigo: String) =
        MockResponse(code = 400, body = """{"code":"$codigo","message":"x","details":null,"hint":null}""")

    @Test
    fun `crear un hogar lo deja activo y guardado sin esperar a sincronizar`() = runTest {
        falso.respuestas["/rest/v1/rpc/crear_hogar"] = MockResponse(code = 200, body = hogarJson)

        val resultado = repositorio().crearHogar("Nuestra casa")

        assertEquals(Resource.Success(Hogar("h1", "Nuestra casa", "K7P2QX")), resultado)
        assertEquals("h1", hogarActivo.id.first())
        assertEquals("K7P2QX", base.hogarDao().observarHogar("h1").first()?.codigoUnion)
        val yo = base.hogarDao().observarIntegrantes("h1").first().single()
        assertEquals("Ana", yo.nombre)
        // Ya está en el servidor: no hay que volver a subirlo.
        assertEquals(EstadoSync.SINCRONIZADO, yo.sync.syncState)
        assertEquals(1, sincronizacionesPedidas)
    }

    @Test
    fun `manda el token y el nombre que la persona puso al crear la cuenta`() = runTest {
        falso.respuestas["/rest/v1/rpc/unirse_a_hogar"] = MockResponse(code = 200, body = hogarJson)

        repositorio().unirseAHogar("K7P2QX")

        val pedido = falso.pedidos.single { it.url.encodedPath == "/rest/v1/rpc/unirse_a_hogar" }
        assertEquals("Bearer token-ana", pedido.headers["Authorization"])
        assertEquals("""{"codigo":"K7P2QX","mi_nombre":"Ana"}""", falso.cuerpoDe("/rest/v1/rpc/unirse_a_hogar"))
    }

    @Test
    fun `los errores propios del servidor llegan con el mensaje de la preentrega`() = runTest {
        val casos = mapOf("MITI1" to YA_TENES_HOGAR, "MITI2" to CODIGO_INVALIDO, "MITI3" to HOGAR_COMPLETO)
        for ((codigo, mensaje) in casos) {
            falso.respuestas["/rest/v1/rpc/unirse_a_hogar"] = errorDelServidor(codigo)

            val resultado = repositorio().unirseAHogar("ZZZZZZ")

            assertEquals(mensaje, (resultado as Resource.Error).message)
        }
        assertNull(hogarActivo.id.first())
        assertEquals(0, sincronizacionesPedidas)
    }

    @Test
    fun `sin conexion avisa que hace falta internet y no deja nada a medias`() = runTest {
        servidor.close()

        val resultado = repositorio().crearHogar("Nuestra casa")

        assertEquals(SIN_CONEXION_HOGAR, (resultado as Resource.Error).message)
        assertNull(hogarActivo.id.first())
    }

    @Test
    fun `sin sesion pide volver a ingresar`() = runTest {
        sesion.borrar()

        val resultado = repositorio().crearHogar("Nuestra casa")

        assertEquals(SESION_VENCIDA, (resultado as Resource.Error).message)
    }

    @Test
    fun `recuperar mi hogar lo activa si la persona ya tenia uno`() = runTest {
        falso.respuestas["/rest/v1/integrantes"] = MockResponse(
            code = 200,
            body = """[{"id":"u-ana","hogar_id":"h1","nombre":"Ana","email":"ana@mail.com","updated_at":$ahora}]""",
        )

        val resultado = repositorio().recuperarMiHogar()

        assertEquals(Resource.Success(true), resultado)
        assertEquals("h1", hogarActivo.id.first())
        assertTrue(sincronizacionesPedidas > 0)
    }

    @Test
    fun `recuperar mi hogar sin hogar en el servidor no activa nada`() = runTest {
        falso.respuestas["/rest/v1/integrantes"] = MockResponse(code = 200, body = "[]")

        assertEquals(Resource.Success(false), repositorio().recuperarMiHogar())
        assertNull(hogarActivo.id.first())
    }
}
