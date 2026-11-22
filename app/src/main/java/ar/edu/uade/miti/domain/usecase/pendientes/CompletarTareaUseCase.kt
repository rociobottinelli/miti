package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/** Marca una tarea como resuelta. Si ya lo estaba, no escribe nada (no genera un cambio para sincronizar). */
class CompletarTareaUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(tareaId: String): Resource<Unit> {
        val tarea = pendientes.obtenerPendiente(tareaId).getOrElse { return it }
            ?: return Resource.Error("La tarea ya no existe")
        if (tarea.tipo != TipoPendiente.TAREA) return Resource.Error("Eso no es una tarea")
        if (tarea.resuelto) return Resource.Success(Unit)
        return pendientes.guardarPendientes(listOf(tarea.copy(resuelto = true)))
    }
}
