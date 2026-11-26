package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/**
 * Tocar una tarea en Hoy: si está pendiente, la marca como hecha (RF03); si ya estaba hecha, la vuelve a dejar
 * pendiente. Devuelve cómo quedó.
 */
class AlternarTareaUseCase @Inject constructor(
    private val pendientes: PendienteRepository,
    private val completar: CompletarTareaUseCase,
    private val reabrir: ReabrirTareaUseCase,
) {
    suspend operator fun invoke(tareaId: String): Resource<Pendiente> {
        val tarea = pendientes.obtenerPendiente(tareaId).getOrElse { return it }
            ?: return Resource.Error("La tarea ya no existe")
        return if (tarea.resuelto) reabrir(tareaId).map { tarea.copy(resuelto = false) } else completar(tareaId)
    }
}
