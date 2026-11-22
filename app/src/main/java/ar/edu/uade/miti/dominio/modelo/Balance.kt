package ar.edu.uade.miti.dominio.modelo

import java.time.YearMonth

/** [deudorId] le debe [montoCentavos] a [acreedorId]. */
data class Deuda(val deudorId: String, val acreedorId: String, val montoCentavos: Long)

/** La parte de un gasto que cae en un mes: qué cuota es, cuánto vale y cuánto le debe el otro integrante. */
data class CuotaDelMes(
    val gasto: Gasto,
    val numero: Int,
    val montoCentavos: Long,
    val adeudadoCentavos: Long,
)

/**
 * Balance de un mes entre los dos integrantes. Nunca se guarda: se calcula a partir de
 * gastos y pagos cada vez que se consulta (D09). Una [Deuda] null significa "están a mano".
 */
data class BalanceDelMes(
    val mes: YearMonth,
    /** Lo que quedó sin saldar de los meses anteriores. */
    val saldoAnterior: Deuda?,
    val cuotas: List<CuotaDelMes>,
    val pagos: List<Pago>,
    /** Saldo anterior + cuotas del mes - pagos del mes. */
    val resultado: Deuda?,
)
