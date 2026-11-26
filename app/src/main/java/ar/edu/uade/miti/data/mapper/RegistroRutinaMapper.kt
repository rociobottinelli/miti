package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.converter.comoTexto
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RegistroRutinaEntity
import ar.edu.uade.miti.data.remote.dto.RegistroRutinaDto
import ar.edu.uade.miti.domain.model.RegistroRutina
import java.time.LocalDateTime

fun RegistroRutinaEntity.toDomain() = RegistroRutina(id, rutinaId, integranteId, fechaHora)

fun RegistroRutina.toEntity(sync: MetadatosSync) = RegistroRutinaEntity(id, rutinaId, integranteId, fechaHora, sync)

fun RegistroRutinaEntity.toDto() = RegistroRutinaDto(
    id, rutinaId, integranteId, fechaHora.comoTexto(), sync.updatedAt, sync.deleted,
)

fun RegistroRutinaDto.toEntity() = RegistroRutinaEntity(
    id, rutinaId, integranteId, LocalDateTime.parse(fechaHora), sincronizado(updatedAt, deleted),
)
