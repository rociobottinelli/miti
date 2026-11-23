package ar.edu.uade.miti.e2e

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.BuildConfig
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import ar.edu.uade.miti.data.remote.AuthApiService
import ar.edu.uade.miti.data.remote.ClavePublicableInterceptor
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.FakeHogarActivo
import ar.edu.uade.miti.data.remote.FakeSesionLocal
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.SyncApiService
import ar.edu.uade.miti.data.repository.GastoRepositoryImpl
import ar.edu.uade.miti.data.repository.HogarRepositoryImpl
import ar.edu.uade.miti.data.repository.PendienteRepositoryImpl
import ar.edu.uade.miti.data.repository.RutinaRepositoryImpl
import ar.edu.uade.miti.data.repository.SesionRepositoryImpl
import ar.edu.uade.miti.data.sync.ResultadoDeSync
import ar.edu.uade.miti.data.sync.Sincronizador
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarCompraUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.MarcarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.RegistrarRutinaUseCase
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Dos celulares contra el Supabase real (C3, #25): cada uno con su propia base Room, su sesión y su
 * sincronizador, igual que en la app. No corre en los builds normales: hace falta
 *
 *   MITI_E2E=1 PRUEBA_ANA_PASSWORD=… PRUEBA_TOMAS_PASSWORD=… ./gradlew testDebugUnitTest --tests '*E2E*'
 *
 * y que las cuentas ana.prueba@miti.test y tomas.prueba@miti.test compartan un hogar en el proyecto
 * configurado en local.properties.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SincronizacionDosCelularesE2ETest {

    private class EstadoEnMemoria : EstadoDeSyncLocal {
        private val cursores = mutableMapOf<String, String>()
        override suspend fun cursor(tabla: String) = cursores[tabla]
        override suspend fun guardarCursor(tabla: String, valor: String) { cursores[tabla] = valor }
        override val ultimaSincronizacion = MutableStateFlow<Long?>(null)
        override suspend fun registrarSincronizacion(momento: Long) { ultimaSincronizacion.value = momento }
        override suspend fun borrar() = cursores.clear()
    }

    /** Todo lo que tiene un celular: su base, su sesión y la app armada como en producción. */
    private class Celular(email: String, private val contrasenia: String, reloj: Clock = Clock.systemUTC()) {
        val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val sesionLocal = FakeSesionLocal()
        val hogarActivo = FakeHogarActivo(null)
        private val config = ConfiguracionSupabase(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        private val moshi = Moshi.Builder().build()
        private val retrofit = Retrofit.Builder()
            .baseUrl(config.urlBase)
            .client(
                OkHttpClient.Builder().addInterceptor(ClavePublicableInterceptor(config.clavePublicable))
                    .readTimeout(15, TimeUnit.SECONDS).build(),
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi).withNullSerialization())
            .build()
        private val auth = retrofit.create(AuthApiService::class.java)
        private val proveedor = ProveedorDeToken(auth, sesionLocal, reloj)
        val sesion = SesionRepositoryImpl(auth, config, sesionLocal, proveedor, hogarActivo, {}, moshi)
        val sincronizador = Sincronizador(
            retrofit.create(SyncApiService::class.java), config, proveedor, sesionLocal, hogarActivo,
            EstadoEnMemoria(), base, reloj, {},
        )
        private val ids = GeneradorDeIds { UUID.randomUUID().toString() }
        val hogares = HogarRepositoryImpl(base.hogarDao(), hogarActivo)
        val rutinas = RutinaRepositoryImpl(base.rutinaDao(), hogarActivo, reloj) {}
        val pendientes = PendienteRepositoryImpl(base.pendienteDao(), hogarActivo, reloj) {}
        val gastos = GastoRepositoryImpl(base.gastoDao(), hogarActivo, reloj) {}
        val registrarRutina = RegistrarRutinaUseCase(hogares, rutinas, ids)
        val marcarRutina = MarcarRutinaUseCase(rutinas, reloj, ids)
        val registrarCompra = RegistrarCompraUseCase(hogares, pendientes, ids)
        val registrarGasto = RegistrarGastoUseCase(hogares, gastos, reloj, ids)
        val email = email

        suspend fun ingresar() = assertTrue(sesion.iniciarSesion(email, contrasenia) is Resource.Success)
        suspend fun sincronizar() = sincronizador.sincronizar().also {
            assertTrue("sincronizar $email: $it", it is ResultadoDeSync.Completa)
        }
        suspend fun usuarioId() = sesionLocal.sesion.first()!!.usuarioId
    }

    private val celulares = mutableListOf<Celular>()

    private fun celular(email: String, variable: String, reloj: Clock = Clock.systemUTC()): Celular {
        val contrasenia = System.getenv(variable)
        assumeTrue("Falta $variable", !contrasenia.isNullOrBlank())
        return Celular(email, contrasenia, reloj).also { celulares += it }
    }

    @After
    fun cerrar() = runBlocking {
        celulares.forEach { it.sesion.cerrarSesion(); it.base.close() }
    }

    @Test
    fun `dos celulares comparten rutinas, compras, gastos y marcas`() = runBlocking {
        assumeTrue("Prueba contra el servidor real: correr con MITI_E2E=1", System.getenv("MITI_E2E") == "1")
        val ana = celular("ana.prueba@miti.test", "PRUEBA_ANA_PASSWORD")
        val tomas = celular("tomas.prueba@miti.test", "PRUEBA_TOMAS_PASSWORD")
        ana.ingresar()
        tomas.ingresar()

        // Primer uso en cada celular: descubre el hogar y descarga lo que ya había.
        ana.sincronizar()
        tomas.sincronizar()
        assertEquals(ana.hogarActivo.id.value, tomas.hogarActivo.id.value)
        assertEquals(2, tomas.hogares.observarIntegrantes().first().size)

        // RNF02: tres cambios en un celular aparecen en el otro.
        val sufijo = UUID.randomUUID().toString().take(8)
        val rutina = (ana.registrarRutina("Pancho $sufijo", Frecuencia.DIARIA, emptySet(), 2) as Resource.Success).data
        val leche = (ana.registrarCompra("Leche $sufijo") as Resource.Success).data
        val gasto = (ana.registrarGasto(DatosDeGasto("Super $sufijo", 2_400_000, ana.usuarioId(), Reparto.A_MEDIAS)) as Resource.Success).data
        ana.sincronizar()
        tomas.sincronizar()
        assertTrue(tomas.rutinas.observarRutinas().first().any { it.id == rutina.id })
        assertTrue(tomas.pendientes.observarPendientes().first().any { it.id == leche.id })
        assertTrue(tomas.gastos.observarGastos().first().any { it.id == gasto.id })

        // D08: los dos marcan la misma rutina "a la vez" y quedan las dos marcas en los dos celulares.
        assertEquals(Resource.Success(true), ana.marcarRutina(rutina.id, ana.usuarioId()))
        assertEquals(Resource.Success(true), tomas.marcarRutina(rutina.id, tomas.usuarioId()))
        ana.sincronizar()
        tomas.sincronizar()
        ana.sincronizar()
        val hoy = java.time.LocalDate.now()
        assertEquals(2, ana.rutinas.observarRegistrosDelDia(hoy).first().count { it.rutinaId == rutina.id })
        assertEquals(2, tomas.rutinas.observarRegistrosDelDia(hoy).first().count { it.rutinaId == rutina.id })

        // Borrado lógico (D07): Ana borra la compra y en el celular de Tomás desaparece.
        ana.pendientes.eliminarPendiente(leche.id)
        ana.sincronizar()
        tomas.sincronizar()
        assertFalse(tomas.pendientes.observarPendientes().first().any { it.id == leche.id })
    }

    @Test
    fun `gana la ultima modificacion aunque la vieja sincronice despues`() = runBlocking {
        assumeTrue("Prueba contra el servidor real: correr con MITI_E2E=1", System.getenv("MITI_E2E") == "1")
        // El celular de Ana atrasa una hora: sus ediciones "son más viejas" que las de Tomás.
        val ana = celular("ana.prueba@miti.test", "PRUEBA_ANA_PASSWORD", Clock.offset(Clock.systemUTC(), Duration.ofHours(-1)))
        val tomas = celular("tomas.prueba@miti.test", "PRUEBA_TOMAS_PASSWORD")
        ana.ingresar()
        tomas.ingresar()
        ana.sincronizar()
        tomas.sincronizar()

        val rutina = (tomas.registrarRutina("Basura ${UUID.randomUUID().toString().take(8)}", Frecuencia.DIARIA, emptySet(), 1) as Resource.Success).data
        tomas.sincronizar()
        ana.sincronizar()

        // Tomás edita (más nuevo) y sincroniza; Ana editó "antes" sin conexión y sincroniza después.
        tomas.rutinas.guardarRutina(rutina.copy(titulo = "Edición de Tomás"))
        ana.rutinas.guardarRutina(rutina.copy(titulo = "Edición de Ana"))
        tomas.sincronizar()
        ana.sincronizar()
        tomas.sincronizar()

        // La de Tomás gana en los dos. (La de Ana se pierde sin aviso: es lo que corrige C2 para gastos, #20.)
        assertEquals("Edición de Tomás", tomas.rutinas.observarRutinas().first().single { it.id == rutina.id }.titulo)
        assertEquals("Edición de Tomás", ana.rutinas.observarRutinas().first().single { it.id == rutina.id }.titulo)
    }
}
