package ar.edu.uade.miti.dominio.modelo

import org.junit.Assert.assertEquals
import org.junit.Test

class RutinaTest {

    private fun rutina(vecesPorDia: Int) = Rutina(
        id = "r1",
        hogarId = "h1",
        titulo = "Darle de comer a Pancho",
        frecuencia = Frecuencia.DIARIA,
        vecesPorDia = vecesPorDia,
    )

    @Test
    fun `acepta de 1 a 3 veces por dia`() {
        (1..3).forEach { assertEquals(it, rutina(it).vecesPorDia) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza 0 veces por dia`() {
        rutina(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza mas de 3 veces por dia`() {
        rutina(4)
    }
}
