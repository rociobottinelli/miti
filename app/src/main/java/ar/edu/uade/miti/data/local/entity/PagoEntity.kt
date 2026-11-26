package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "pagos", indices = [Index("hogarId")])
data class PagoEntity(
    @PrimaryKey val id: String,
    val hogarId: String,
    val deIntegranteId: String,
    val aIntegranteId: String,
    val montoCentavos: Long,
    val fecha: LocalDate,
    @Embedded val sync: MetadatosSync,
)
