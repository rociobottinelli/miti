package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/**
 * Vuelve a dejar pendiente una tarea que se marcó como hecha por error (el "Deshacer" de Hoy). Si tenía
 * aviso y su hora todavía no pasó, la alarma se vuelve a programar sola, porque se reprograma con cada cambio.
 */
class ReabrirTareaUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(tareaId: String): Resource<Unit> {
        val tarea = pendientes.obtenerPendiente(tareaId).getOrElse { return it }
            ?: return Resource.Error("La tarea ya no existe")
        if (tarea.tipo != TipoPendiente.TAREA) return Resource.Error("Eso no es una tarea")
        if (!tarea.resuelto) return Resource.Success(Unit)
        return pendientes.guardarPendientes(listOf(tarea.copy(resuelto = false)))
    }
}
