package ar.edu.uade.miti.dominio.casouso.rutinas

import ar.edu.uade.miti.dominio.modelo.Frecuencia
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepositorio
import ar.edu.uade.miti.dominio.repositorio.RutinaRepositorio
import ar.edu.uade.miti.dominio.repositorio.requerirHogarActivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Una rutina con las marcas que recibió en un día. El progreso se calcula, no se guarda (D09). */
data class RutinaDelDia(val rutina: Rutina, val registros: List<RegistroRutina>) {
    val hechas: Int get() = minOf(registros.size, rutina.vecesPorDia)
    val completa: Boolean get() = registros.size >= rutina.vecesPorDia
}

class RegistrarRutina @Inject constructor(
    private val hogares: HogarRepositorio,
    private val rutinas: RutinaRepositorio,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(
        titulo: String,
        frecuencia: Frecuencia,
        dias: Set<DayOfWeek>,
        vecesPorDia: Int,
    ): Rutina {
        val rutina = Rutina(
            id = ids.nuevoId(),
            hogarId = hogares.requerirHogarActivo(),
            titulo = titulo.trim(),
            frecuencia = frecuencia,
            dias = if (frecuencia == Frecuencia.DIARIA) emptySet() else dias,
            vecesPorDia = vecesPorDia,
        )
        rutinas.guardarRutina(rutina)
        return rutina
    }
}

/**
 * Rutinas que corresponden a [fecha] con su progreso. Se recalcula sola cada vez que
 * cambia una rutina o llega una marca nueva, venga de la app, del widget o de la sincronización.
 */
class ObservarRutinasDelDia @Inject constructor(private val rutinas: RutinaRepositorio) {
    operator fun invoke(fecha: LocalDate): Flow<List<RutinaDelDia>> =
        combine(rutinas.observarRutinas(), rutinas.observarRegistrosDelDia(fecha)) { todas, registros ->
            val registrosPorRutina = registros.groupBy { it.rutinaId }
            todas
                .filter { it.correspondeA(fecha) }
                .map { rutina ->
                    RutinaDelDia(rutina, registrosPorRutina[rutina.id].orEmpty().sortedBy { it.fechaHora })
                }
        }
}

/**
 * Agrega una marca de la rutina por [integranteId] con la hora actual. Nunca edita marcas
 * existentes (D08). Si la rutina ya estaba completa hoy no agrega nada y devuelve false,
 * para no repetir una tarea que ya se hizo.
 */
class MarcarRutina @Inject constructor(
    private val rutinas: RutinaRepositorio,
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(rutinaId: String, integranteId: String): Boolean {
        val ahora = LocalDateTime.now(reloj)
        val rutina = requireNotNull(rutinas.observarRutinas().first().find { it.id == rutinaId }) {
            "No existe la rutina $rutinaId"
        }
        val marcasDeHoy = rutinas.observarRegistrosDelDia(ahora.toLocalDate()).first().count { it.rutinaId == rutinaId }
        if (marcasDeHoy >= rutina.vecesPorDia) return false

        rutinas.agregarRegistro(RegistroRutina(ids.nuevoId(), rutinaId, integranteId, ahora))
        return true
    }
}
