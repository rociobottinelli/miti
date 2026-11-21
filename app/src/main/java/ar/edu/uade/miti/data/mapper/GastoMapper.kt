package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.domain.model.Gasto

fun GastoEntity.toDomain() = Gasto(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio,
)

fun Gasto.toEntity(sync: MetadatosSync) = GastoEntity(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio, sync,
)
