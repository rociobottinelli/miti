package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/**
 * Borra una tarea o un ítem de compras (borrado lógico, D07). Devuelve cómo estaba, para poder deshacerlo con
 * [RestaurarPendienteUseCase]. Si era una tarea con aviso, la alarma se cancela sola al reprogramar.
 */
class EliminarPendienteUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(id: String): Resource<Pendiente> {
        val pendiente = pendientes.obtenerPendiente(id).getOrElse { return it }
            ?: return Resource.Error("Ya no existe")
        return pendientes.eliminarPendiente(id).map { pendiente }
    }
}

/** El "Deshacer" de un borrado: vuelve a guardar el ítem como estaba. */
class RestaurarPendienteUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(pendiente: Pendiente): Resource<Unit> = pendientes.guardarPendientes(listOf(pendiente))
}
