package ar.edu.uade.miti.datos.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ar.edu.uade.miti.dominio.modelo.Frecuencia
import ar.edu.uade.miti.dominio.modelo.Reparto
import ar.edu.uade.miti.dominio.modelo.TipoPendiente
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// Tablas de Room. Son el reflejo de los modelos de dominio más los datos de sincronización,
// que el dominio no conoce. No hay claves foráneas a propósito: con la sincronización, una fila
// puede llegar antes que la fila a la que apunta (por ejemplo, una marca antes que su rutina).

enum class EstadoSync {
    /** Igual que en el servidor. */
    SINCRONIZADO,

    /** Cambió en este celular y todavía no se envió (D06). */
    PENDIENTE,
}

/** Columnas que comparten todas las tablas sincronizables (preentrega, sección 14). */
data class MetadatosSync(
    /** Momento del último cambio, en milisegundos. Decide qué cambio gana en un conflicto (D08). */
    val updatedAt: Long,
    /** Borrado lógico: la fila queda para que el borrado viaje al otro celular (D07). */
    val deleted: Boolean = false,
    val syncState: EstadoSync = EstadoSync.PENDIENTE,
)

@Entity(tableName = "hogares")
data class HogarEntidad(
    @PrimaryKey val id: String,
    val nombre: String,
    val codigoUnion: String,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "integrantes", indices = [Index("hogarId")])
data class IntegranteEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    val nombre: String,
    val email: String,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "rutinas", indices = [Index("hogarId")])
data class RutinaEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    val titulo: String,
    val frecuencia: Frecuencia,
    val dias: Set<DayOfWeek>,
    val vecesPorDia: Int,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "registros_rutina", indices = [Index("rutinaId"), Index("fechaHora")])
data class RegistroRutinaEntidad(
    @PrimaryKey val id: String,
    val rutinaId: String,
    val integranteId: String,
    val fechaHora: LocalDateTime,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "pendientes", indices = [Index("hogarId")])
data class PendienteEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    val tipo: TipoPendiente,
    val titulo: String,
    val responsableId: String?,
    val fechaHora: LocalDateTime?,
    val avisar: Boolean,
    val enCarrito: Boolean,
    val resuelto: Boolean,
    val gastoId: String?,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "gastos", indices = [Index("hogarId")])
data class GastoEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String?,
    val cuotasTotales: Int,
    val cuotaInicial: Int,
    val mesInicio: YearMonth,
    @Embedded val sync: MetadatosSync,
)

@Entity(tableName = "pagos", indices = [Index("hogarId")])
data class PagoEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    val deIntegranteId: String,
    val aIntegranteId: String,
    val montoCentavos: Long,
    val fecha: LocalDate,
    @Embedded val sync: MetadatosSync,
)
