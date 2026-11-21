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
}
