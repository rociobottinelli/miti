package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Agrega una marca de la rutina por [integranteId] con la hora actual. Nunca edita marcas
 * existentes (D08). Si la rutina ya estaba completa hoy no agrega nada y devuelve false,
 * para no repetir una tarea que ya se hizo.
 */
class MarcarRutinaUseCase @Inject constructor(
    private val rutinas: RutinaRepository,
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
