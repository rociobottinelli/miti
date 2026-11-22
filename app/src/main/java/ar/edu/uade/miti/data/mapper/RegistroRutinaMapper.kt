package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RegistroRutinaEntity
import ar.edu.uade.miti.domain.model.RegistroRutina

fun RegistroRutinaEntity.toDomain() = RegistroRutina(id, rutinaId, integranteId, fechaHora)

fun RegistroRutina.toEntity(sync: MetadatosSync) = RegistroRutinaEntity(id, rutinaId, integranteId, fechaHora, sync)
