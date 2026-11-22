package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ar.edu.uade.miti.data.local.entity.HogarEntity
import ar.edu.uade.miti.data.local.entity.IntegranteEntity
import kotlinx.coroutines.flow.Flow

// DAOs ("Data Access Objects"): las consultas a la base. Room genera el código de cada función
// a partir de la anotación. Las lecturas devuelven Flow, que vuelve a emitir cuando cambia la tabla.
// Todas las lecturas ignoran las filas borradas lógicamente.
// @Upsert inserta la fila o, si ya existe una con el mismo id, la reemplaza.

@Dao
interface HogarDao {
    @Query("SELECT * FROM hogares WHERE id = :hogarId AND deleted = 0")
    fun observarHogar(hogarId: String): Flow<HogarEntity?>

    @Query("SELECT * FROM integrantes WHERE hogarId = :hogarId AND deleted = 0 ORDER BY nombre")
    fun observarIntegrantes(hogarId: String): Flow<List<IntegranteEntity>>

    @Upsert
    suspend fun guardarHogar(hogar: HogarEntity)

    @Upsert
    suspend fun guardarIntegrante(integrante: IntegranteEntity)

    // --- Sincronización (#18): hogares e integrantes solo se descargan (se crean en el servidor). ---

    @Query("SELECT * FROM hogares WHERE id = :id")
    suspend fun hogaresParaSync(id: String): HogarEntity?

    @Query("SELECT * FROM integrantes WHERE id = :id")
    suspend fun integrantesParaSync(id: String): IntegranteEntity?

    @Upsert
    suspend fun guardarHogaresDescargados(filas: List<HogarEntity>)

    @Upsert
    suspend fun guardarIntegrantesDescargados(filas: List<IntegranteEntity>)
}
