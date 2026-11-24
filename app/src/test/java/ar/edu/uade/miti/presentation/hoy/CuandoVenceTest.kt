package ar.edu.uade.miti.presentation.hoy

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CuandoVenceTest {

    /** Miércoles. */
    private val hoy = LocalDate.of(2026, 9, 30)
    private val seis = LocalTime.of(18, 0)

    @Test
    fun `una tarea de ayer sin resolver esta atrasada`() {
        assertEquals(CuandoVence.Atrasada(hoy.minusDays(1), seis), cuandoVence(hoy.minusDays(1).atTime(seis), hoy))
    }

    @Test
    fun `hoy y maniana se nombran asi aunque la hora ya haya pasado`() {
        assertEquals(CuandoVence.Hoy(LocalTime.of(7, 0)), cuandoVence(hoy.atTime(7, 0), hoy))
        assertEquals(CuandoVence.Maniana(seis), cuandoVence(hoy.plusDays(1).atTime(seis), hoy))
    }

    @Test
    fun `dentro de la semana alcanza con el dia`() {
        assertEquals(CuandoVence.EstaSemana(DayOfWeek.FRIDAY, seis), cuandoVence(hoy.plusDays(2).atTime(seis), hoy))
        assertEquals(CuandoVence.EstaSemana(DayOfWeek.TUESDAY, seis), cuandoVence(hoy.plusDays(6).atTime(seis), hoy))
    }

    @Test
    fun `a partir de los 7 dias se muestra la fecha para no confundir el miercoles que viene con hoy`() {
        assertEquals(CuandoVence.MasAdelante(hoy.plusDays(7), seis), cuandoVence(hoy.plusDays(7).atTime(seis), hoy))
    }
}
