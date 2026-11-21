package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Pago
import kotlinx.coroutines.flow.Flow

interface GastoRepository {
    fun observarGastos(): Flow<List<Gasto>>
    fun observarPagos(): Flow<List<Pago>>
    suspend fun guardarGasto(gasto: Gasto): Resource<Unit>
    suspend fun eliminarGasto(id: String): Resource<Unit>
    suspend fun guardarPago(pago: Pago): Resource<Unit>
    suspend fun eliminarPago(id: String): Resource<Unit>
}
