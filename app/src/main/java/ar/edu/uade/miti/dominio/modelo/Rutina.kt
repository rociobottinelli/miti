package ar.edu.uade.miti.dominio.modelo

import java.time.DayOfWeek
import java.time.LocalDateTime

enum class Frecuencia { DIARIA, DIAS_DE_LA_SEMANA }

/** Rutina sin responsable fijo: cualquiera la marca y se reinicia cada día. */
data class Rutina(
    val id: String,
    val hogarId: String,
    val titulo: String,
    val frecuencia: Frecuencia,
    /** Solo se usa con [Frecuencia.DIAS_DE_LA_SEMANA]. */
    val dias: Set<DayOfWeek> = emptySet(),
    val vecesPorDia: Int,
) {
    init {
        require(vecesPorDia in 1..3) { "vecesPorDia debe estar entre 1 y 3" }
    }
}

/**
 * Cada marca de una rutina. Los registros solo se agregan, nunca se editan (D08);
 * el progreso del día se calcula a partir de los registros de hoy (D09).
 */
data class RegistroRutina(
    val id: String,
    val rutinaId: String,
    val integranteId: String,
    val fechaHora: LocalDateTime,
)
