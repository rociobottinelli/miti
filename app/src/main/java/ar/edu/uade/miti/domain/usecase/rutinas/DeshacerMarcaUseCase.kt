package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Deshace la última marca de hoy que hizo [integranteId] en una rutina (por ejemplo, un toque sin querer en el
 * widget). Solo las propias: la marca del otro dice "quién y cuándo" y no se toca. Devuelve false si no había
 * ninguna propia para deshacer.
 */
class DeshacerMarcaUseCase @Inject constructor(
    private val rutinas: RutinaRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(rutinaId: String, integranteId: String): Resource<Boolean> {
        val ultima = rutinas.observarRegistrosDelDia(LocalDate.now(reloj)).first()
            .filter { it.rutinaId == rutinaId && it.integranteId == integranteId }
            .maxByOrNull { it.fechaHora }
            ?: return Resource.Success(false)
        return rutinas.quitarRegistro(ultima.id).map { true }
    }
}
