package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Pago
import kotlinx.coroutines.flow.Flow

interface GastoRepository {
    fun observarGastos(): Flow<List<Gasto>>
    fun observarPagos(): Flow<List<Pago>>
    suspend fun guardarGasto(gasto: Gasto)
    suspend fun eliminarGasto(id: String)
    suspend fun guardarPago(pago: Pago)
    suspend fun eliminarPago(id: String)
}
