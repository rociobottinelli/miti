package ar.edu.uade.miti.dominio.modelo

import org.junit.Test
import java.time.LocalDateTime

class PendienteTest {

    private val jueves18 = LocalDateTime.of(2026, 11, 5, 18, 0)

    @Test
    fun `acepta una tarea completa y una compra simple`() {
        Pendiente("p1", "h1", TipoPendiente.TAREA, "Pagar ABL", responsableId = "tomas", fechaHora = jueves18, avisar = true)
        Pendiente("p2", "h1", TipoPendiente.COMPRA, "Leche", enCarrito = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un titulo vacio`() {
        Pendiente("p1", "h1", TipoPendiente.COMPRA, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una tarea sin responsable`() {
        Pendiente("p1", "h1", TipoPendiente.TAREA, "Pagar ABL", fechaHora = jueves18)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una tarea sin fecha`() {
        Pendiente("p1", "h1", TipoPendiente.TAREA, "Pagar ABL", responsableId = "tomas")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una compra con responsable`() {
        Pendiente("p1", "h1", TipoPendiente.COMPRA, "Leche", responsableId = "tomas")
    }
}
