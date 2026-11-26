package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Agrega una marca de la rutina por [integranteId] con la hora actual. Nunca edita marcas
 * existentes (D08). Si la rutina ya estaba completa hoy no agrega nada y devuelve Success(false),
 * para no repetir una tarea que ya se hizo.
 */
class MarcarRutinaUseCase @Inject constructor(
    private val rutinas: RutinaRepository,
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(rutinaId: String, integranteId: String): Resource<Boolean> {
        val ahora = LocalDateTime.now(reloj)
        val rutina = rutinas.observarRutinas().first().find { it.id == rutinaId }
            ?: return Resource.Error("La rutina ya no existe")
        val marcasDeHoy = rutinas.observarRegistrosDelDia(ahora.toLocalDate()).first().count { it.rutinaId == rutinaId }
        if (marcasDeHoy >= rutina.vecesPorDia) return Resource.Success(false)

        return rutinas.agregarRegistro(RegistroRutina(ids.nuevoId(), rutinaId, integranteId, ahora)).map { true }
    }
}
