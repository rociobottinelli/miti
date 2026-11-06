package ar.edu.uade.miti.dominio.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RutinaTest {

    private fun rutina(
        vecesPorDia: Int = 1,
        frecuencia: Frecuencia = Frecuencia.DIARIA,
        dias: Set<DayOfWeek> = emptySet(),
        titulo: String = "Darle de comer a Pancho",
    ) = Rutina(
        id = "r1",
        hogarId = "h1",
        titulo = titulo,
        frecuencia = frecuencia,
        dias = dias,
        vecesPorDia = vecesPorDia,
    )

    @Test
    fun `acepta de 1 a 3 veces por dia`() {
        (1..3).forEach { assertEquals(it, rutina(vecesPorDia = it).vecesPorDia) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza 0 veces por dia`() {
        rutina(vecesPorDia = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza mas de 3 veces por dia`() {
        rutina(vecesPorDia = 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un titulo vacio`() {
        rutina(titulo = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza dias de la semana sin ningun dia`() {
        rutina(frecuencia = Frecuencia.DIAS_DE_LA_SEMANA)
    }

    @Test
    fun `una rutina diaria corresponde a cualquier dia`() {
        assertTrue(rutina().correspondeA(LocalDate.of(2026, 11, 5)))
    }

    @Test
    fun `una rutina de los lunes solo corresponde a los lunes`() {
        val deLosLunes = rutina(frecuencia = Frecuencia.DIAS_DE_LA_SEMANA, dias = setOf(DayOfWeek.MONDAY))
        assertTrue(deLosLunes.correspondeA(LocalDate.of(2026, 11, 2))) // lunes
        assertFalse(deLosLunes.correspondeA(LocalDate.of(2026, 11, 3))) // martes
    }
}
