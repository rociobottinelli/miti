package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ar.edu.uade.miti.data.local.entity.PendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendienteDao {
    @Query("SELECT * FROM pendientes WHERE hogarId = :hogarId AND deleted = 0")
    fun observarPendientes(hogarId: String): Flow<List<PendienteEntity>>

    @Query("SELECT * FROM pendientes WHERE id = :id AND deleted = 0")
    suspend fun obtenerPendiente(id: String): PendienteEntity?

    /** Guarda todos juntos: si falla uno, no se guarda ninguno. */
    @Upsert
    suspend fun guardarPendientes(pendientes: List<PendienteEntity>)

    @Query("UPDATE pendientes SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarBorrado(id: String, ahora: Long)

    // --- Sincronización (#18). Incluyen filas borradas: el borrado lógico también viaja. ---

    @Query("SELECT * FROM pendientes WHERE syncState = 'PENDIENTE'")
    suspend fun pendientesPorSubir(): List<PendienteEntity>

    /**
     * Marca la fila como sincronizada solo si no cambió mientras se subía (mismo updatedAt).
     * Si cambió, queda PENDIENTE y sube en la próxima vuelta. Devuelve las filas afectadas.
     */
    @Query("UPDATE pendientes SET syncState = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarPendienteSincronizado(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM pendientes WHERE id = :id")
    suspend fun pendientesParaSync(id: String): PendienteEntity?

    @Upsert
    suspend fun guardarPendientesDescargados(filas: List<PendienteEntity>)
}
