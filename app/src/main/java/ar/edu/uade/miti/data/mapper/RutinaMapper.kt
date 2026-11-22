package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RutinaEntity
import ar.edu.uade.miti.domain.model.Rutina

fun RutinaEntity.toDomain() = Rutina(id, hogarId, titulo, frecuencia, dias, vecesPorDia)

fun Rutina.toEntity(sync: MetadatosSync) = RutinaEntity(id, hogarId, titulo, frecuencia, dias, vecesPorDia, sync)
