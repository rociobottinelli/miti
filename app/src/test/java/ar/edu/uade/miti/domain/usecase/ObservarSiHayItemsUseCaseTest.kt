package ar.edu.uade.miti.domain.usecase

import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.FakeRutinaRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.model.TipoPendiente
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservarSiHayItemsUseCaseTest {

    private val rutinas = FakeRutinaRepository()
    private val pendientes = FakePendienteRepository()
    private val hayItems = ObservarSiHayItemsUseCase(rutinas, pendientes)

    @Test
    fun `un hogar recien creado no tiene items`() = runTest {
        assertFalse(hayItems().first())
    }

    @Test
    fun `una rutina de otro dia de la semana cuenta`() = runTest {
        rutinas.rutinas.value = listOf(
            Rutina("r", HOGAR, "Sacar la basura", Frecuencia.DIAS_DE_LA_SEMANA, setOf(DayOfWeek.MONDAY), 1),
        )
        assertTrue(hayItems().first())
    }

    @Test
    fun `una tarea ya resuelta cuenta`() = runTest {
        pendientes.pendientes.value = listOf(
            Pendiente("t", HOGAR, TipoPendiente.TAREA, "Pagar ABL", ANA, LocalDateTime.of(2026, 11, 5, 18, 0), resuelto = true),
        )
        assertTrue(hayItems().first())
    }

    @Test
    fun `las compras no cuentan porque no se muestran en Hoy`() = runTest {
        pendientes.pendientes.value = listOf(Pendiente("c", HOGAR, TipoPendiente.COMPRA, "Leche"))
        assertFalse(hayItems().first())
    }
}
