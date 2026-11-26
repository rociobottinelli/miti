package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hogares")
data class HogarEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val codigoUnion: String,
    @Embedded val sync: MetadatosSync,
)
