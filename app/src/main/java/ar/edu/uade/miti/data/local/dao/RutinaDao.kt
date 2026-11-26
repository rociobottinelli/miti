package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ar.edu.uade.miti.data.local.entity.RegistroRutinaEntity
import ar.edu.uade.miti.data.local.entity.RutinaEntity
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaDao {
    @Query("SELECT * FROM rutinas WHERE hogarId = :hogarId AND deleted = 0 ORDER BY titulo")
    fun observarRutinas(hogarId: String): Flow<List<RutinaEntity>>

    /** Marcas de las rutinas del hogar entre [desde] (incluido) y [hasta] (excluido). */
    @Query(
        """
        SELECT registro.* FROM registros_rutina AS registro
        INNER JOIN rutinas AS rutina ON rutina.id = registro.rutinaId
        WHERE rutina.hogarId = :hogarId AND registro.deleted = 0
          AND registro.fechaHora >= :desde AND registro.fechaHora < :hasta
        ORDER BY registro.fechaHora
        """,
    )
    fun observarRegistros(hogarId: String, desde: LocalDateTime, hasta: LocalDateTime): Flow<List<RegistroRutinaEntity>>

    @Upsert
    suspend fun guardarRutina(rutina: RutinaEntity)

    @Upsert
    suspend fun guardarRegistro(registro: RegistroRutinaEntity)

    @Query("UPDATE rutinas SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarBorrada(id: String, ahora: Long)

    /** Deshacer una marca: borrado lógico, para que el otro celular también la deje de contar. */
    @Query("UPDATE registros_rutina SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarRegistroBorrado(id: String, ahora: Long)

    // --- Sincronización (#18). Incluyen filas borradas: el borrado lógico también viaja. ---

    @Query("SELECT * FROM rutinas WHERE syncState = 'PENDIENTE'")
    suspend fun rutinasPorSubir(): List<RutinaEntity>

    /**
     * Marca la fila como sincronizada solo si no cambió mientras se subía (mismo updatedAt).
     * Si cambió, queda PENDIENTE y sube en la próxima vuelta. Devuelve las filas afectadas.
     */
    @Query("UPDATE rutinas SET syncState = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarRutinaSincronizado(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM rutinas WHERE id = :id")
    suspend fun rutinasParaSync(id: String): RutinaEntity?

    @Upsert
    suspend fun guardarRutinasDescargados(filas: List<RutinaEntity>)

    // --- Sincronización (#18). Incluyen filas borradas: el borrado lógico también viaja. ---

    @Query("SELECT * FROM registros_rutina WHERE syncState = 'PENDIENTE'")
    suspend fun registrosPorSubir(): List<RegistroRutinaEntity>

    /**
     * Marca la fila como sincronizada solo si no cambió mientras se subía (mismo updatedAt).
     * Si cambió, queda PENDIENTE y sube en la próxima vuelta. Devuelve las filas afectadas.
     */
    @Query("UPDATE registros_rutina SET syncState = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarRegistroSincronizado(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM registros_rutina WHERE id = :id")
    suspend fun registrosParaSync(id: String): RegistroRutinaEntity?

    @Upsert
    suspend fun guardarRegistrosDescargados(filas: List<RegistroRutinaEntity>)
}
