package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ar.edu.uade.miti.data.local.entity.ConflictoGastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConflictoGastoDao {
    @Query("SELECT * FROM conflictos_gasto WHERE resuelto = 0 AND deleted = 0 ORDER BY updatedAt")
    fun observarAbiertos(): Flow<List<ConflictoGastoEntity>>

    @Query("SELECT * FROM conflictos_gasto WHERE id = :id")
    suspend fun obtener(id: String): ConflictoGastoEntity?

    /**
     * Se sube también updatedAt: así, si la sincronización vuelve a traer el mismo conflicto (descarga con
     * margen), la versión remota es más vieja y no lo reabre.
     */
    @Query("UPDATE conflictos_gasto SET resuelto = 1, updatedAt = MAX(updatedAt + 1, :ahora) WHERE id = :id")
    suspend fun marcarResuelto(id: String, ahora: Long): Int

    // --- Sincronización: solo se descargan (los crea el servidor). ---

    @Query("SELECT * FROM conflictos_gasto WHERE id = :id")
    suspend fun conflictosParaSync(id: String): ConflictoGastoEntity?

    @Upsert
    suspend fun guardarConflictosDescargados(filas: List<ConflictoGastoEntity>)
}
