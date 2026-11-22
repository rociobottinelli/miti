package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "registros_rutina", indices = [Index("rutinaId"), Index("fechaHora")])
data class RegistroRutinaEntity(
    @PrimaryKey val id: String,
    val rutinaId: String,
    val integranteId: String,
    val fechaHora: LocalDateTime,
    @Embedded val sync: MetadatosSync,
)
