package ar.edu.uade.miti.datos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.datos.local.EstadoSync
import ar.edu.uade.miti.datos.local.HogarEntidad
import ar.edu.uade.miti.datos.local.IntegranteEntidad
import ar.edu.uade.miti.datos.local.MetadatosSync
import ar.edu.uade.miti.datos.local.MitiDatabase
import ar.edu.uade.miti.datos.repositorio.GastoRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.HogarRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.PendienteRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.RutinaRepositorioRoom
import ar.edu.uade.miti.datos.sesion.HogarActivo
import ar.edu.uade.miti.dominio.casouso.gastos.ObtenerBalanceDelMes
import ar.edu.uade.miti.dominio.fakes.ANA
import ar.edu.uade.miti.dominio.fakes.HOGAR
import ar.edu.uade.miti.dominio.fakes.TOMAS
import ar.edu.uade.miti.dominio.fakes.relojEn
import ar.edu.uade.miti.dominio.modelo.Deuda
import ar.edu.uade.miti.dominio.modelo.Frecuencia
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Reparto
import ar.edu.uade.miti.dominio.modelo.Rutina
import ar.edu.uade.miti.dominio.modelo.TipoPendiente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Prueba la capa de datos contra una base Room real, en memoria. Corre en la PC gracias a
 * Robolectric, que simula Android, así que no hace falta emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoriosRoomTest {

    private class HogarActivoEnMemoria(inicial: String?) : HogarActivo {
        private val actual = MutableStateFlow(inicial)
        override val id: Flow<String?> = actual
        override suspend fun establecer(hogarId: String?) { actual.value = hogarId }
    }

    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MitiDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val hogarActivo = HogarActivoEnMemoria(HOGAR)
    private val ahora = LocalDateTime.of(2026, 11, 5, 18, 30)
    private val reloj = relojEn(ahora)

    private val hogares = HogarRepositorioRoom(base.hogarDao(), hogarActivo)
    private val rutinas = RutinaRepositorioRoom(base.rutinaDao(), hogarActivo, reloj)
    private val pendientes = PendienteRepositorioRoom(base.pendienteDao(), hogarActivo, reloj)
    private val gastos = GastoRepositorioRoom(base.gastoDao(), hogarActivo, reloj)

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
        assertEquals(compra, pendientes.obtenerPendiente("c1"))
        pendientes.eliminarPendiente("c1")
        assertNull(pendientes.obtenerPendiente("c1"))
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
        base.hogarDao().guardarHogar(HogarEntidad(HOGAR, "Casa", "ABC123", sync))
        base.hogarDao().guardarIntegrante(IntegranteEntidad(ANA, HOGAR, "Ana", "ana@mail.com", sync))
        base.hogarDao().guardarIntegrante(IntegranteEntidad(TOMAS, HOGAR, "Tomás", "tomas@mail.com", sync))
        val noviembre = YearMonth.of(2026, 11)
        gastos.guardarGasto(Gasto("heladera", HOGAR, "Heladera", 3_000_000, TOMAS, Reparto.A_MEDIAS, cuotasTotales = 3, mesInicio = noviembre))
        gastos.guardarPago(Pago("p1", HOGAR, ANA, TOMAS, 200_000, LocalDate.of(2026, 11, 20)))

        val balance = ObtenerBalanceDelMes(hogares, gastos)(noviembre).first()

        assertEquals(Deuda(ANA, TOMAS, 300_000), balance?.resultado)
    }
}
