package ar.edu.uade.miti.datos.local

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * SQLite solo guarda números y texto. Estos convertidores le enseñan a Room a guardar los tipos
 * de fecha como texto ISO, que además se ordena bien alfabéticamente: "2026-11-05T08:15:00" va
 * antes que "2026-11-05T20:00:00", así las consultas por rango de fechas funcionan.
 * Los enums (Frecuencia, Reparto...) Room ya los guarda por su nombre sin ayuda.
 */
class Convertidores {

    @TypeConverter
    fun desdeFechaHora(valor: LocalDateTime?): String? =
        // Se descartan los milisegundos para que todas las fechas tengan el mismo largo de texto.
        valor?.truncatedTo(ChronoUnit.SECONDS)?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun aFechaHora(valor: String?): LocalDateTime? = valor?.let(LocalDateTime::parse)

    @TypeConverter
    fun desdeFecha(valor: LocalDate?): String? = valor?.toString()

    @TypeConverter
    fun aFecha(valor: String?): LocalDate? = valor?.let(LocalDate::parse)

    @TypeConverter
    fun desdeMes(valor: YearMonth?): String? = valor?.toString()

    @TypeConverter
    fun aMes(valor: String?): YearMonth? = valor?.let(YearMonth::parse)

    /** Los días se guardan como "MONDAY,THURSDAY"; un conjunto vacío, como texto vacío. */
    @TypeConverter
    fun desdeDias(valor: Set<DayOfWeek>): String = valor.sorted().joinToString(",") { it.name }

    @TypeConverter
    fun aDias(valor: String): Set<DayOfWeek> =
        if (valor.isEmpty()) emptySet() else valor.split(",").map(DayOfWeek::valueOf).toSet()
}
