package ar.edu.uade.miti.presentation.hoy

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Formatos de fecha y hora de Hoy. La app está solo en español (preentrega, sección 4), así que el
// idioma es fijo y no depende de la configuración del teléfono.

private val espaniol: Locale = Locale.forLanguageTag("es-AR")
private val formatoHora = DateTimeFormatter.ofPattern("H:mm", espaniol)
private val formatoFechaLarga = DateTimeFormatter.ofPattern("d 'de' MMMM", espaniol)
private val formatoFechaCorta = DateTimeFormatter.ofPattern("d/M", espaniol)

/** "8:15", "21:03". */
internal fun LocalTime.comoHora(): String = format(formatoHora)

/** "1 de octubre". */
internal fun LocalDate.comoFechaLarga(): String = format(formatoFechaLarga)

/** "28/9". */
internal fun LocalDate.comoFechaCorta(): String = format(formatoFechaCorta)

/** "Miércoles". */
internal fun DayOfWeek.comoNombre(): String =
    getDisplayName(TextStyle.FULL, espaniol).replaceFirstChar { it.titlecase(espaniol) }
