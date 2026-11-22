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

fun HogarEntidad.aDominio() = Hogar(id, nombre, codigoUnion)
fun IntegranteEntidad.aDominio() = Integrante(id, hogarId, nombre, email)

fun RutinaEntidad.aDominio() = Rutina(id, hogarId, titulo, frecuencia, dias, vecesPorDia)
fun Rutina.aEntidad(sync: MetadatosSync) = RutinaEntidad(id, hogarId, titulo, frecuencia, dias, vecesPorDia, sync)

fun RegistroRutinaEntidad.aDominio() = RegistroRutina(id, rutinaId, integranteId, fechaHora)
fun RegistroRutina.aEntidad(sync: MetadatosSync) = RegistroRutinaEntidad(id, rutinaId, integranteId, fechaHora, sync)

fun PendienteEntidad.aDominio() =
    Pendiente(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId)
fun Pendiente.aEntidad(sync: MetadatosSync) =
    PendienteEntidad(id, hogarId, tipo, titulo, responsableId, fechaHora, avisar, enCarrito, resuelto, gastoId, sync)

fun GastoEntidad.aDominio() = Gasto(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio,
)
fun Gasto.aEntidad(sync: MetadatosSync) = GastoEntidad(
    id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId, cuotasTotales, cuotaInicial, mesInicio, sync,
)

fun PagoEntidad.aDominio() = Pago(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha)
fun Pago.aEntidad(sync: MetadatosSync) = PagoEntidad(id, hogarId, deIntegranteId, aIntegranteId, montoCentavos, fecha, sync)
