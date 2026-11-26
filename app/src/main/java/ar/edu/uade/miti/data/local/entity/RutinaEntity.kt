package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ar.edu.uade.miti.domain.model.Frecuencia
import java.time.DayOfWeek

@Entity(tableName = "rutinas", indices = [Index("hogarId")])
data class RutinaEntity(
    @PrimaryKey val id: String,
    val hogarId: String,
    val titulo: String,
    val frecuencia: Frecuencia,
    val dias: Set<DayOfWeek>,
    val vecesPorDia: Int,
    @Embedded val sync: MetadatosSync,
)
