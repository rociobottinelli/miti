package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
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

    /** El gasto con ese id, o null si no existe o se borró. */
    suspend fun obtenerGasto(id: String): Resource<Gasto?>

    /** Ediciones propias que no entraron por un conflicto y todavía no se resolvieron (C2). */
    fun observarConflictos(): Flow<List<ConflictoDeGasto>>

    /** La persona ya eligió qué hacer con el conflicto: deja de mostrarse. */
    suspend fun resolverConflicto(id: String): Resource<Unit>
}
