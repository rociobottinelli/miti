package ar.edu.uade.miti.dominio.modelo

import java.time.LocalDate
import java.time.YearMonth

enum class Reparto {
    /** 50/50 entre los dos integrantes. */
    A_MEDIAS,

    /** 100/0: el gasto es todo de [Gasto.beneficiarioId]. */
    UNO_SOLO,
}

/** Los montos se guardan en centavos para no arrastrar errores de punto flotante. */
data class Gasto(
    val id: String,
    val hogarId: String,
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String? = null,
    val cuotasTotales: Int = 1,
    /** Cuota que corresponde a [mesInicio] (permite cargar una compra en cuotas ya empezada). */
    val cuotaInicial: Int = 1,
    val mesInicio: YearMonth,
)

/** Pago entre integrantes para saldar el balance; se descuenta del mes de su [fecha]. */
data class Pago(
    val id: String,
    val hogarId: String,
    val deIntegranteId: String,
    val aIntegranteId: String,
    val montoCentavos: Long,
    val fecha: LocalDate,
)
