package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tareas ya hechas cuya fecha es [desde] o posterior: Hoy las sigue mostrando tildadas, así se pueden destildar
 * si se marcaron por error. Cuando pasa su fecha, dejan de mostrarse. Con [responsableId], solo las de esa persona.
 */
class ObservarTareasHechasUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    operator fun invoke(desde: LocalDate, responsableId: String? = null): Flow<List<Pendiente>> =
        pendientes.observarPendientes().map { todos ->
            todos
                .filter { it.tipo == TipoPendiente.TAREA && it.resuelto }
                .filter { tarea -> tarea.fechaHora?.toLocalDate()?.let { it >= desde } == true }
                .filter { responsableId == null || it.responsableId == responsableId }
                .sortedBy { it.fechaHora }
        }
}
