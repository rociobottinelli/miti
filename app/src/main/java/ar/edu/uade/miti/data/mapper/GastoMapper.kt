package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.ConflictoGastoEntity
import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.remote.dto.ConflictoGastoDto
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
    mesInicio.toString(), sync.updatedAt, sync.deleted, baseUpdatedAt = baseUpdatedAt,
)

fun GastoDto.toEntity() = GastoEntity(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, Reparto.valueOf(reparto), beneficiarioId, cuotasTotales,
    cuotaInicial, YearMonth.parse(mesInicio), sincronizado(updatedAt, deleted),
)

fun ConflictoGastoDto.toEntity() = with(versionRechazada) {
    ConflictoGastoEntity(
        id = this@toEntity.id,
        gastoId = this@toEntity.gastoId,
        descripcion = descripcion,
        montoTotalCentavos = montoTotalCentavos,
        pagadorId = pagadorId,
        reparto = Reparto.valueOf(reparto),
        beneficiarioId = beneficiarioId,
        cuotasTotales = cuotasTotales,
        cuotaInicial = cuotaInicial,
        mesInicio = YearMonth.parse(mesInicio),
        eraBorrado = deleted,
        sync = sincronizado(this@toEntity.updatedAt, this@toEntity.deleted),
    )
}

/** La versión que no entró, como gasto: mismo id y hogar que el gasto en conflicto. */
fun ConflictoGastoEntity.miVersion(hogarId: String) = Gasto(
    gastoId, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio,
)
