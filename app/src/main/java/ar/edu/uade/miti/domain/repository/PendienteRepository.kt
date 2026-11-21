package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Pendiente
import kotlinx.coroutines.flow.Flow

interface PendienteRepository {
    fun observarPendientes(): Flow<List<Pendiente>>
    suspend fun obtenerPendiente(id: String): Resource<Pendiente?>
    suspend fun guardarPendientes(pendientes: List<Pendiente>): Resource<Unit>
    suspend fun eliminarPendiente(id: String): Resource<Unit>
}
