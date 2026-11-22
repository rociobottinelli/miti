package ar.edu.uade.miti.dominio.casouso.gastos

import ar.edu.uade.miti.dominio.fakes.ANA
import ar.edu.uade.miti.dominio.fakes.HOGAR
import ar.edu.uade.miti.dominio.fakes.TOMAS
import ar.edu.uade.miti.dominio.modelo.Deuda
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Reparto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Casos de RNF11: el balance tiene que coincidir con el cálculo manual. Montos en centavos. */
class CalculadoraDeBalanceTest {

    private val noviembre = YearMonth.of(2026, 11)
    private val diciembre = YearMonth.of(2026, 12)
    private val enero = YearMonth.of(2027, 1)

    private fun gasto(
        id: String,
        monto: Long,
        pagador: String = TOMAS,
        reparto: Reparto = Reparto.A_MEDIAS,
        beneficiario: String? = null,
        cuotas: Int = 1,
        cuotaInicial: Int = 1,
        mes: YearMonth = noviembre,
    ) = Gasto(id, HOGAR, id, monto, pagador, reparto, beneficiario, cuotas, cuotaInicial, mes)

    private fun pago(monto: Long, de: String = ANA, a: String = TOMAS, fecha: LocalDate = LocalDate.of(2026, 11, 30)) =
        Pago("pago-$monto-$fecha", HOGAR, de, a, monto, fecha)

    private fun balance(mes: YearMonth, gastos: List<Gasto>, pagos: List<Pago> = emptyList()) =
        CalculadoraDeBalance.calcular(mes, ANA, TOMAS, gastos, pagos)

    // Ejemplo de la preentrega: Tomás paga $30.000 en 3 cuotas, a medias.
    private val heladera = gasto("heladera", monto = 3_000_000, cuotas = 3)

    @Test
    fun `sin gastos estan a mano`() {
        val resultado = balance(noviembre, emptyList())
        assertNull(resultado.resultado)
        assertNull(resultado.saldoAnterior)
    }

    @Test
    fun `ejemplo de la preentrega - cada mes Ana le debe la mitad de la cuota`() {
        val resultado = balance(noviembre, listOf(heladera))

        assertEquals(Deuda(ANA, TOMAS, 500_000), resultado.resultado)
        assertEquals(1, resultado.cuotas.single().numero)
        assertEquals(1_000_000, resultado.cuotas.single().montoCentavos)
    }

    @Test
    fun `ejemplo de la preentrega - si Ana paga 5000 quedan a mano`() {
        val resultado = balance(noviembre, listOf(heladera), listOf(pago(500_000)))
        assertNull(resultado.resultado)
    }

    @Test
    fun `ejemplo de la preentrega - si Ana no paga, el mes siguiente arrastra el saldo`() {
        val resultado = balance(diciembre, listOf(heladera))

        assertEquals(Deuda(ANA, TOMAS, 500_000), resultado.saldoAnterior)
        assertEquals(Deuda(ANA, TOMAS, 1_000_000), resultado.resultado)
    }

    @Test
    fun `el saldo se arrastra varios meses y se descuentan los pagos parciales`() {
        val pagos = listOf(pago(300_000), pago(200_000, fecha = LocalDate.of(2026, 12, 10)))

        val resultado = balance(enero, listOf(heladera), pagos)

        // Noviembre: 5000 - 3000 = 2000. Diciembre: 2000 + 5000 - 2000 = 5000. Enero: 5000 + 5000.
        assertEquals(Deuda(ANA, TOMAS, 500_000), resultado.saldoAnterior)
        assertEquals(Deuda(ANA, TOMAS, 1_000_000), resultado.resultado)
    }

    @Test
    fun `terminadas las cuotas el gasto ya no suma`() {
        val resultado = balance(YearMonth.of(2027, 2), listOf(heladera), listOf(pago(1_500_000)))

        assertEquals(emptyList<Any>(), resultado.cuotas)
        assertNull(resultado.resultado)
    }

    @Test
    fun `un gasto de uno solo lo debe entero el beneficiario`() {
        val remedios = gasto("remedios", monto = 800_000, pagador = ANA, reparto = Reparto.UNO_SOLO, beneficiario = TOMAS)

        assertEquals(Deuda(TOMAS, ANA, 800_000), balance(noviembre, listOf(remedios)).resultado)
    }

    @Test
    fun `un gasto de uno solo para quien pago no genera deuda`() {
        val propio = gasto("propio", monto = 800_000, pagador = ANA, reparto = Reparto.UNO_SOLO, beneficiario = ANA)

        assertNull(balance(noviembre, listOf(propio)).resultado)
    }

    @Test
    fun `los gastos de los dos se compensan`() {
        val supermercado = gasto("super", monto = 2_400_000, pagador = TOMAS)
        val luz = gasto("luz", monto = 1_000_000, pagador = ANA)

        // Ana le debe 12000 a Tomás y Tomás le debe 5000 a Ana: Ana le debe 7000.
        assertEquals(Deuda(ANA, TOMAS, 700_000), balance(noviembre, listOf(supermercado, luz)).resultado)
    }

    @Test
    fun `si Ana paga de mas, pasa a deberle Tomas`() {
        val resultado = balance(noviembre, listOf(heladera), listOf(pago(800_000)))

        assertEquals(Deuda(TOMAS, ANA, 300_000), resultado.resultado)
    }

    @Test
    fun `una compra en cuotas ya empezada solo suma desde su cuota actual`() {
        val tele = gasto("tele", monto = 600_000, cuotas = 6, cuotaInicial = 5)

        assertEquals(Deuda(ANA, TOMAS, 50_000), balance(noviembre, listOf(tele)).resultado)
        assertEquals(Deuda(ANA, TOMAS, 100_000), balance(diciembre, listOf(tele)).resultado)
        assertEquals(Deuda(ANA, TOMAS, 100_000), balance(enero, listOf(tele)).resultado)
    }

    @Test
    fun `un gasto que empieza mas adelante no afecta meses anteriores`() {
        val futuro = gasto("futuro", monto = 1_000_000, mes = enero)

        assertNull(balance(diciembre, listOf(futuro)).resultado)
    }

    @Test
    fun `a medias con monto impar el centavo que sobra lo absorbe quien pago`() {
        val impar = gasto("impar", monto = 1_001)

        assertEquals(Deuda(ANA, TOMAS, 500), balance(noviembre, listOf(impar)).resultado)
    }

    @Test
    fun `el resultado no depende del orden de los integrantes`() {
        val gastos = listOf(heladera)
        val alReves = CalculadoraDeBalance.calcular(noviembre, TOMAS, ANA, gastos, emptyList())

        assertEquals(balance(noviembre, gastos).resultado, alReves.resultado)
    }
}
