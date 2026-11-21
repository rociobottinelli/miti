package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RutinaEntity
import ar.edu.uade.miti.data.remote.dto.RutinaDto
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Rutina

fun RutinaEntity.toDomain() = Rutina(id, hogarId, titulo, frecuencia, dias, vecesPorDia)

fun Rutina.toEntity(sync: MetadatosSync) = RutinaEntity(id, hogarId, titulo, frecuencia, dias, vecesPorDia, sync)

fun RutinaEntity.toDto() = RutinaDto(
    id, hogarId, titulo, frecuencia.name, convertidores.desdeDias(dias), vecesPorDia, sync.updatedAt, sync.deleted,
)

fun RutinaDto.toEntity() = RutinaEntity(
    id, hogarId, titulo, Frecuencia.valueOf(frecuencia), convertidores.aDias(dias), vecesPorDia,
    sincronizado(updatedAt, deleted),
)
