package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tareas sin resolver ordenadas por fecha; con [responsableId], solo las de esa persona. */
class ObservarProximasTareasUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    operator fun invoke(responsableId: String? = null): Flow<List<Pendiente>> =
        pendientes.observarPendientes().map { todos ->
            todos
                .filter { it.tipo == TipoPendiente.TAREA && !it.resuelto }
                .filter { responsableId == null || it.responsableId == responsableId }
                .sortedBy { it.fechaHora }
        }
}
