package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

class CompletarTareaUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(tareaId: String) {
        val tarea = requireNotNull(pendientes.obtenerPendiente(tareaId)) { "No existe la tarea $tareaId" }
        require(tarea.tipo == TipoPendiente.TAREA) { "$tareaId no es una tarea" }
        if (!tarea.resuelto) pendientes.guardarPendientes(listOf(tarea.copy(resuelto = true)))
    }
}
