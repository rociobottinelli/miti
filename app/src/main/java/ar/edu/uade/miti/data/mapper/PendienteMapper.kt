package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.converter.comoTexto
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.PendienteEntity
import ar.edu.uade.miti.data.remote.dto.PendienteDto
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import java.time.LocalDateTime

fun PendienteEntity.toDomain() =
    Pendiente(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId)

fun Pendiente.toEntity(sync: MetadatosSync) =
    PendienteEntity(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId, sync)

fun PendienteEntity.toDto() = PendienteDto(
    id, hogarId, tipo.name, titulo, responsableId, fechaHora?.comoTexto(), avisar, enCarrito, resuelto,
    gastoId, sync.updatedAt, sync.deleted,
)

fun PendienteDto.toEntity() = PendienteEntity(
    id, hogarId, TipoPendiente.valueOf(tipo), titulo, responsableId, fechaHora?.let(LocalDateTime::parse), avisar,
    enCarrito, resuelto, gastoId, sincronizado(updatedAt, deleted),
)
