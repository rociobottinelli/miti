package ar.edu.uade.miti.datos.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

// DAOs ("Data Access Objects"): las consultas a la base. Room genera el código de cada función
// a partir de la anotación. Las lecturas devuelven Flow, que vuelve a emitir cuando cambia la tabla.
// Todas las lecturas ignoran las filas borradas lógicamente.
// @Upsert inserta la fila o, si ya existe una con el mismo id, la reemplaza.

@Dao
interface HogarDao {
    @Query("SELECT * FROM hogares WHERE id = :hogarId AND deleted = 0")
    fun observarHogar(hogarId: String): Flow<HogarEntidad?>

    @Query("SELECT * FROM integrantes WHERE hogarId = :hogarId AND deleted = 0 ORDER BY nombre")
    fun observarIntegrantes(hogarId: String): Flow<List<IntegranteEntidad>>

    @Upsert
    suspend fun guardarHogar(hogar: HogarEntidad)

    @Upsert
    suspend fun guardarIntegrante(integrante: IntegranteEntidad)
}

@Dao
interface RutinaDao {
    @Query("SELECT * FROM rutinas WHERE hogarId = :hogarId AND deleted = 0 ORDER BY titulo")
    fun observarRutinas(hogarId: String): Flow<List<RutinaEntidad>>

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
    fun observarRegistros(hogarId: String, desde: LocalDateTime, hasta: LocalDateTime): Flow<List<RegistroRutinaEntidad>>

    @Upsert
    suspend fun guardarRutina(rutina: RutinaEntidad)

    @Upsert
    suspend fun guardarRegistro(registro: RegistroRutinaEntidad)

    @Query("UPDATE rutinas SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarBorrada(id: String, ahora: Long)
}

@Dao
interface PendienteDao {
    @Query("SELECT * FROM pendientes WHERE hogarId = :hogarId AND deleted = 0")
    fun observarPendientes(hogarId: String): Flow<List<PendienteEntidad>>

    @Query("SELECT * FROM pendientes WHERE id = :id AND deleted = 0")
    suspend fun obtenerPendiente(id: String): PendienteEntidad?

    /** Guarda todos juntos: si falla uno, no se guarda ninguno. */
    @Upsert
    suspend fun guardarPendientes(pendientes: List<PendienteEntidad>)

    @Query("UPDATE pendientes SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarBorrado(id: String, ahora: Long)
}

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos WHERE hogarId = :hogarId AND deleted = 0")
    fun observarGastos(hogarId: String): Flow<List<GastoEntidad>>

    @Query("SELECT * FROM pagos WHERE hogarId = :hogarId AND deleted = 0 ORDER BY fecha")
    fun observarPagos(hogarId: String): Flow<List<PagoEntidad>>

    @Upsert
    suspend fun guardarGasto(gasto: GastoEntidad)

    @Upsert
    suspend fun guardarPago(pago: PagoEntidad)

    @Query("UPDATE gastos SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarGastoBorrado(id: String, ahora: Long)

    @Query("UPDATE pagos SET deleted = 1, updatedAt = :ahora, syncState = 'PENDIENTE' WHERE id = :id")
    suspend fun marcarPagoBorrado(id: String, ahora: Long)
}
