package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.PagoEntity
import ar.edu.uade.miti.data.remote.dto.PagoDto
import ar.edu.uade.miti.domain.model.Pago
import java.time.LocalDate

fun PagoEntity.toDomain() = Pago(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha)

fun Pago.toEntity(sync: MetadatosSync) = PagoEntity(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha, sync)

fun PagoEntity.toDto() = PagoDto(
    id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha.toString(), sync.updatedAt, sync.deleted,
)

fun PagoDto.toEntity() = PagoEntity(
    id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, LocalDate.parse(fecha), sincronizado(updatedAt, deleted),
)
