package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.HogarEntity
import ar.edu.uade.miti.data.local.entity.IntegranteEntity
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Deuda
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Pago
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerBalanceDelMesUseCase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prueba la capa de datos contra una base Room real, en memoria. Corre en la PC gracias a
 * Robolectric, que simula Android, así que no hace falta emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryImplTest {

    private class HogarActivoEnMemoria(inicial: String?) : HogarActivo {
        private val actual = MutableStateFlow(inicial)
        override val id: Flow<String?> = actual
        override suspend fun establecer(hogarId: String?) { actual.value = hogarId }
    }

    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val hogarActivo = HogarActivoEnMemoria(HOGAR)
    private val ahora = LocalDateTime.of(2026, 11, 5, 18, 30)
    private val reloj = relojEn(ahora)

    private val hogares = HogarRepositoryImpl(base.hogarDao(), hogarActivo)
    private var avisos = 0
    private val avisoDeCambios = AvisoDeCambios { avisos++ }
    private val rutinas = RutinaRepositoryImpl(base.rutinaDao(), hogarActivo, reloj, avisoDeCambios)
    private val pendientes = PendienteRepositoryImpl(base.pendienteDao(), hogarActivo, reloj, avisoDeCambios)
    private val gastos = GastoRepositoryImpl(base.gastoDao(), hogarActivo, reloj, avisoDeCambios, base.conflictoGastoDao())

    @After
    fun cerrar() = base.close()

    private fun estadoDe(tabla: String, id: String): Pair<Boolean, String> =
        base.query("SELECT deleted, syncState FROM $tabla WHERE id = ?", arrayOf(id)).use { cursor ->
            cursor.moveToFirst()
            (cursor.getInt(0) == 1) to cursor.getString(1)
        }

    @Test
    fun `guarda y lee una rutina con sus dias`() = runTest {
        val basura = Rutina("basura", HOGAR, "Sacar la basura", Frecuencia.DIAS_DE_LA_SEMANA, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), 1)

        rutinas.guardarRutina(basura)

        assertEquals(listOf(basura), rutinas.observarRutinas().first())
        assertEquals(false to EstadoSync.PENDIENTE.name, estadoDe("rutinas", "basura"))
    }

    @Test
    fun `solo trae las marcas del dia pedido`() = runTest {
        val pancho = Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2)
        rutinas.guardarRutina(pancho)
        val medianoche = RegistroRutina("r1", "pancho", ANA, LocalDateTime.of(2026, 11, 5, 0, 0))
        val ultimoMinuto = RegistroRutina("r2", "pancho", TOMAS, LocalDateTime.of(2026, 11, 5, 23, 59, 59))
        val delDiaSiguiente = RegistroRutina("r3", "pancho", ANA, LocalDateTime.of(2026, 11, 6, 0, 0))
        listOf(delDiaSiguiente, ultimoMinuto, medianoche).forEach { rutinas.agregarRegistro(it) }

        assertEquals(listOf(medianoche, ultimoMinuto), rutinas.observarRegistrosDelDia(LocalDate.of(2026, 11, 5)).first())
    }

    @Test
    fun `borrar es un borrado logico que queda pendiente de sincronizar`() = runTest {
        rutinas.guardarRutina(Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2))

        rutinas.eliminarRutina("pancho")

        assertEquals(emptyList<Rutina>(), rutinas.observarRutinas().first())
        assertEquals(true to EstadoSync.PENDIENTE.name, estadoDe("rutinas", "pancho"))
    }

    @Test
    fun `guarda y lee tareas y compras`() = runTest {
        val tarea = Pendiente("t1", HOGAR, TipoPendiente.TAREA, "Pagar ABL", TOMAS, ahora, avisar = true)
        val compra = Pendiente("c1", HOGAR, TipoPendiente.COMPRA, "Leche", enCarrito = true)

        pendientes.guardarPendientes(listOf(tarea, compra))

        assertEquals(setOf(tarea, compra), pendientes.observarPendientes().first().toSet())
        assertEquals(Resource.Success(compra), pendientes.obtenerPendiente("c1"))
        pendientes.eliminarPendiente("c1")
        assertEquals(Resource.Success(null), pendientes.obtenerPendiente("c1"))
    }

    @Test
    fun `no muestra datos de otro hogar y se actualiza al cambiar de hogar`() = runTest {
        val leche = Pendiente("c1", HOGAR, TipoPendiente.COMPRA, "Leche")
        val ajena = Pendiente("c2", "otro", TipoPendiente.COMPRA, "Yerba")
        pendientes.guardarPendientes(listOf(leche, ajena))

        assertEquals(listOf(leche), pendientes.observarPendientes().first())
        hogarActivo.establecer("otro")
        assertEquals(listOf(ajena), pendientes.observarPendientes().first())
        hogarActivo.establecer(null)
        assertEquals(emptyList<Pendiente>(), pendientes.observarPendientes().first())
    }

    @Test
    fun `el balance funciona de punta a punta sobre Room`() = runTest {
        val sync = MetadatosSync(updatedAt = 0, syncState = EstadoSync.SINCRONIZADO)
        base.hogarDao().guardarHogar(HogarEntity(HOGAR, "Casa", "ABC123", sync))
        base.hogarDao().guardarIntegrante(IntegranteEntity(ANA, HOGAR, "Ana", "ana@mail.com", sync))
        base.hogarDao().guardarIntegrante(IntegranteEntity(TOMAS, HOGAR, "Tomás", "tomas@mail.com", sync))
        val noviembre = YearMonth.of(2026, 11)
        gastos.guardarGasto(Gasto("heladera", HOGAR, "Heladera", 3_000_000, TOMAS, Reparto.A_MEDIAS, cuotasTotales = 3, mesInicio = noviembre))
        gastos.guardarPago(Pago("p1", HOGAR, ANA, TOMAS, 200_000, LocalDate.of(2026, 11, 20)))

        val balance = ObtenerBalanceDelMesUseCase(hogares, gastos)(noviembre).first()

        assertEquals(Deuda(ANA, TOMAS, 300_000), balance?.resultado)
    }

    @Test
    fun `cada escritura avisa que hay un cambio para sincronizar`() = runTest {
        rutinas.guardarRutina(Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2))
        rutinas.eliminarRutina("pancho")

        assertEquals(2, avisos)
    }
}
