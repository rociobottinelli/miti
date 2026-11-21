package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.PendienteEntity
import ar.edu.uade.miti.domain.model.Pendiente

fun PendienteEntity.toDomain() =
    Pendiente(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId)

fun Pendiente.toEntity(sync: MetadatosSync) =
    PendienteEntity(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId, sync)
