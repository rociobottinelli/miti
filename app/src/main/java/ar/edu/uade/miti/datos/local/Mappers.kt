package ar.edu.uade.miti.datos.local

import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Hogar
import ar.edu.uade.miti.dominio.modelo.Integrante
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina

// Traducen entre tablas y modelos de dominio. Al leer se descartan los datos de sincronización;
// al guardar, el repositorio los agrega con [MetadatosSync].

fun HogarEntity.toDomain() = Hogar(id, nombre, codigoUnion)
fun IntegranteEntity.toDomain() = Integrante(id, hogarId, nombre, email)

fun RutinaEntity.toDomain() = Rutina(id, hogarId, titulo, frecuencia, dias, vecesPorDia)
fun Rutina.toEntity(sync: MetadatosSync) = RutinaEntity(id, hogarId, titulo, frecuencia, dias, vecesPorDia, sync)

fun RegistroRutinaEntity.toDomain() = RegistroRutina(id, rutinaId, integranteId, fechaHora)
fun RegistroRutina.toEntity(sync: MetadatosSync) = RegistroRutinaEntity(id, rutinaId, integranteId, fechaHora, sync)

fun PendienteEntity.toDomain() =
    Pendiente(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId)
fun Pendiente.toEntity(sync: MetadatosSync) =
    PendienteEntity(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId, sync)

fun GastoEntity.toDomain() = Gasto(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio,
)
fun Gasto.toEntity(sync: MetadatosSync) = GastoEntity(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio, sync,
)

fun PagoEntity.toDomain() = Pago(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha)
fun Pago.toEntity(sync: MetadatosSync) = PagoEntity(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha, sync)
