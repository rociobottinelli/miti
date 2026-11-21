package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.IntegranteEntity
import ar.edu.uade.miti.domain.model.Integrante

fun IntegranteEntity.toDomain() = Integrante(id, hogarId, nombre, email)
