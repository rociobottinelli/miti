package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ar.edu.uade.miti.domain.model.Reparto
import java.time.YearMonth

/**
 * Una edición de gasto de esta persona que no entró porque el otro lo había cambiado antes (C2, #20).
 * La crea el servidor y llega con la sincronización; acá se guarda la versión que no entró para mostrarla
 * y poder rehacerla.
 */
@Entity(tableName = "conflictos_gasto", indices = [Index("gastoId")])
data class ConflictoGastoEntity(
    @PrimaryKey val id: String,
    val gastoId: String,
    // La versión que no entró.
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String?,
    val cuotasTotales: Int,
    val cuotaInicial: Int,
    val mesInicio: YearMonth,
    /** La edición que no entró era borrar el gasto. */
    val eraBorrado: Boolean,
    /** Ya lo vio y eligió qué hacer (rehacer o descartar). Solo vive en este celular. */
    val resuelto: Boolean = false,
    @Embedded val sync: MetadatosSync,
)
