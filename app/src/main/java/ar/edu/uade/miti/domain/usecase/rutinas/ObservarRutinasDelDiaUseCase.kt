package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.domain.model.RutinaDelDia
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Rutinas que corresponden a [fecha] con su progreso. Se recalcula sola cada vez que
 * cambia una rutina o llega una marca nueva, venga de la app, del widget o de la sincronización.
 */
class ObservarRutinasDelDiaUseCase @Inject constructor(private val rutinas: RutinaRepository) {
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
