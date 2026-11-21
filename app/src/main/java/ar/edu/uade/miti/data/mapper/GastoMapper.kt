package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.remote.dto.GastoDto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import java.time.YearMonth

fun GastoEntity.toDomain() = Gasto(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio,
)

fun Gasto.toEntity(sync: MetadatosSync) = GastoEntity(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio, sync,
)

fun GastoEntity.toDto() = GastoDto(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto.name, beneficiarioId, cuotasTotales, cuotaInicial,
    mesInicio.toString(), sync.updatedAt, sync.deleted,
)

fun GastoDto.toEntity() = GastoEntity(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, Reparto.valueOf(reparto), beneficiarioId, cuotasTotales,
    cuotaInicial, YearMonth.parse(mesInicio), sincronizado(updatedAt, deleted),
)
