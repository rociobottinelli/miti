package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.PagoEntity
import ar.edu.uade.miti.domain.model.Pago

fun PagoEntity.toDomain() = Pago(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha)

fun Pago.toEntity(sync: MetadatosSync) = PagoEntity(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha, sync)
