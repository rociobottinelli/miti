package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos WHERE hogarId = :hogarId AND deleted = 0")
    fun observarGastos(hogarId: String): Flow<List<GastoEntity>>

    @Query("SELECT * FROM pagos WHERE hogarId = :hogarId AND deleted = 0 ORDER BY fecha")
    fun observarPagos(hogarId: String): Flow<List<PagoEntity>>

    @Upsert
    suspend fun guardarGasto(gasto: GastoEntity)

    @Upsert
    suspend fun guardarPago(pago: PagoEntity)

    @Query("UPDATE gastos SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarGastoBorrado(id: String, ahora: Long)

    @Query("UPDATE pagos SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarPagoBorrado(id: String, ahora: Long)

    // --- Sincronización (#18). Incluyen filas borradas: el borrado lógico también viaja. ---

    @Query("SELECT * FROM gastos WHERE syncState = 'PENDIENTE'")
    suspend fun gastosPorSubir(): List<GastoEntity>

    /**
     * Marca la fila como sincronizada solo si no cambió mientras se subía (mismo updatedAt).
     * Si cambió, queda PENDIENTE y sube en la próxima vuelta. Devuelve las filas afectadas.
     */
    @Query("UPDATE gastos SET syncState = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarGastoSincronizado(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM gastos WHERE id = :id")
    suspend fun gastosParaSync(id: String): GastoEntity?

    @Upsert
    suspend fun guardarGastosDescargados(filas: List<GastoEntity>)

    // --- Sincronización (#18). Incluyen filas borradas: el borrado lógico también viaja. ---

    @Query("SELECT * FROM pagos WHERE syncState = 'PENDIENTE'")
    suspend fun pagosPorSubir(): List<PagoEntity>

    /**
     * Marca la fila como sincronizada solo si no cambió mientras se subía (mismo updatedAt).
     * Si cambió, queda PENDIENTE y sube en la próxima vuelta. Devuelve las filas afectadas.
     */
    @Query("UPDATE pagos SET syncState = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarPagoSincronizado(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun pagosParaSync(id: String): PagoEntity?

    @Upsert
    suspend fun guardarPagosDescargados(filas: List<PagoEntity>)
}
