package ar.edu.uade.miti.presentation.hoy

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Cómo se lee la fecha de una tarea en Hoy ("Jueves 18:00", "Mañana 9:30"). Se decide acá, sin Android,
 * para poder probarlo; la pantalla solo elige el texto de cada caso.
 */
sealed interface CuandoVence {
    val hora: LocalTime

    /** Era para un día anterior y todavía nadie la resolvió. */
    data class Atrasada(val fecha: LocalDate, override val hora: LocalTime) : CuandoVence
    data class Hoy(override val hora: LocalTime) : CuandoVence
    data class Maniana(override val hora: LocalTime) : CuandoVence

    /** Dentro de los próximos 6 días: alcanza con el nombre del día. */
    data class EstaSemana(val dia: DayOfWeek, override val hora: LocalTime) : CuandoVence
    data class MasAdelante(val fecha: LocalDate, override val hora: LocalTime) : CuandoVence
}

fun cuandoVence(fechaHora: LocalDateTime, hoy: LocalDate): CuandoVence {
    val fecha = fechaHora.toLocalDate()
    val hora = fechaHora.toLocalTime()
    return when {
        fecha.isBefore(hoy) -> CuandoVence.Atrasada(fecha, hora)
        fecha == hoy -> CuandoVence.Hoy(hora)
        fecha == hoy.plusDays(1) -> CuandoVence.Maniana(hora)
        fecha.isBefore(hoy.plusDays(7)) -> CuandoVence.EstaSemana(fecha.dayOfWeek, hora)
        else -> CuandoVence.MasAdelante(fecha, hora)
    }
}
