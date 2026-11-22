package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ar.edu.uade.miti.domain.model.Reparto
import java.time.YearMonth

@Entity(tableName = "gastos", indices = [Index("hogarId")])
data class GastoEntity(
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
