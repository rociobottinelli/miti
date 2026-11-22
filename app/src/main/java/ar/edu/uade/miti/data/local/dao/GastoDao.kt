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
}
