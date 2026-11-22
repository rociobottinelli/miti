package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "integrantes", indices = [Index("hogarId")])
data class IntegranteEntity(
    @PrimaryKey val id: String,
    val hogarId: String,
    val nombre: String,
    val email: String,
    @Embedded val sync: MetadatosSync,
)
