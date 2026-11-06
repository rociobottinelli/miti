package ar.edu.uade.miti.dominio.casouso.gastos

import ar.edu.uade.miti.dominio.modelo.BalanceDelMes
import ar.edu.uade.miti.dominio.modelo.CuotaDelMes
import ar.edu.uade.miti.dominio.modelo.Deuda
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Reparto
import java.time.YearMonth

/**
 * Regla de cálculo del balance (preentrega, sección 9). Es una función pura: no lee
 * ni guarda nada, así se prueba con cualquier combinación de gastos y pagos (RNF11).
 *
 * Internamente todo se suma como un único número, el "saldo", desde el punto de vista de [integranteA]:
 * positivo si B le debe a A, negativo si A le debe a B, cero si están a mano.
 */
object CalculadoraDeBalance {

    fun calcular(
        mes: YearMonth,
        integranteA: String,
        integranteB: String,
        gastos: List<Gasto>,
        pagos: List<Pago>,
    ): BalanceDelMes {
        var saldoAnterior = 0L
        val cuotasDelMes = mutableListOf<CuotaDelMes>()

        for (gasto in gastos) {
            for ((mesDeCuota, numero) in gasto.mesesConCuota()) {
                if (mesDeCuota > mes) continue
                val monto = gasto.montoDeCuota(numero)
                val adeudado = adeudadoPorElOtro(gasto, monto)
                if (mesDeCuota == mes) {
                    cuotasDelMes += CuotaDelMes(gasto, numero, monto, adeudado)
                } else {
                    saldoAnterior += efectoDeGasto(gasto, adeudado, integranteA)
                }
            }
        }

        val pagosDelMes = pagos.filter { YearMonth.from(it.fecha) == mes }
        saldoAnterior += pagos
            .filter { YearMonth.from(it.fecha) < mes }
            .sumOf { efectoDePago(it, integranteA) }

        val saldoDelMes = cuotasDelMes.sumOf { efectoDeGasto(it.gasto, it.adeudadoCentavos, integranteA) } +
            pagosDelMes.sumOf { efectoDePago(it, integranteA) }

        return BalanceDelMes(
            mes = mes,
            saldoAnterior = comoDeuda(saldoAnterior, integranteA, integranteB),
            cuotas = cuotasDelMes.sortedBy { it.gasto.descripcion },
            pagos = pagosDelMes.sortedBy { it.fecha },
            resultado = comoDeuda(saldoAnterior + saldoDelMes, integranteA, integranteB),
        )
    }

    /**
     * Cuánto de una cuota le corresponde pagar a quien no pagó. A medias, la mitad
     * (si el monto es impar, el centavo que sobra lo absorbe quien pagó).
     * De uno solo, todo si el beneficiario es el otro, o nada si es el mismo que pagó.
     */
    private fun adeudadoPorElOtro(gasto: Gasto, montoCuota: Long): Long = when (gasto.reparto) {
        Reparto.A_MEDIAS -> montoCuota / 2
        Reparto.UNO_SOLO -> if (gasto.beneficiarioId == gasto.pagadorId) 0 else montoCuota
    }

    /** Si pagó A, B le debe más (suma). Si pagó B, A le debe más (resta). */
    private fun efectoDeGasto(gasto: Gasto, adeudado: Long, integranteA: String): Long =
        if (gasto.pagadorId == integranteA) adeudado else -adeudado

    /** Un pago de B a A achica lo que B le debe a A (resta); un pago de A a B, al revés. */
    private fun efectoDePago(pago: Pago, integranteA: String): Long =
        if (pago.aIntegranteId == integranteA) -pago.montoCentavos else pago.montoCentavos

    private fun comoDeuda(saldo: Long, integranteA: String, integranteB: String): Deuda? = when {
        saldo > 0 -> Deuda(deudorId = integranteB, acreedorId = integranteA, montoCentavos = saldo)
        saldo < 0 -> Deuda(deudorId = integranteA, acreedorId = integranteB, montoCentavos = -saldo)
        else -> null
    }
}
