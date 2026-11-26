package ar.edu.uade.miti.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ar.edu.uade.miti.domain.model.TipoPendiente
import java.time.LocalDateTime

@Entity(tableName = "pendientes", indices = [Index("hogarId")])
data class PendienteEntity(
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
