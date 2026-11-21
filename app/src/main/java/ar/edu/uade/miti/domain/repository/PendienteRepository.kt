package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.domain.model.Pendiente
import kotlinx.coroutines.flow.Flow

interface PendienteRepository {
    fun observarPendientes(): Flow<List<Pendiente>>
    suspend fun obtenerPendiente(id: String): Pendiente?
    suspend fun guardarPendientes(pendientes: List<Pendiente>)
    suspend fun eliminarPendiente(id: String)
}
