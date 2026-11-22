package ar.edu.uade.miti.dominio.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GastoTest {

    private val noviembre = YearMonth.of(2026, 11)

    private fun gasto(
        monto: Long = 3_000_000,
        cuotas: Int = 3,
        cuotaInicial: Int = 1,
        reparto: Reparto = Reparto.A_MEDIAS,
        beneficiarioId: String? = null,
    ) = Gasto(
        id = "g1",
        hogarId = "h1",
        descripcion = "Heladera",
        montoTotalCentavos = monto,
        pagadorId = "tomas",
        reparto = reparto,
        beneficiarioId = beneficiarioId,
        cuotasTotales = cuotas,
        cuotaInicial = cuotaInicial,
        mesInicio = noviembre,
    )

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza monto cero`() {
        gasto(monto = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una cuota actual mayor al total`() {
        gasto(cuotas = 3, cuotaInicial = 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un gasto de uno solo sin beneficiario`() {
        gasto(reparto = Reparto.UNO_SOLO)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un gasto a medias con beneficiario`() {
        gasto(beneficiarioId = "ana")
    }

    @Test
    fun `divide el monto en cuotas iguales`() {
        val heladera = gasto()
        assertEquals(listOf(1_000_000L, 1_000_000L, 1_000_000L), (1..3).map(heladera::montoDeCuota))
    }

    @Test
    fun `reparte el resto de la division en las primeras cuotas`() {
        val impar = gasto(monto = 1_000, cuotas = 3)
        assertEquals(listOf(334L, 333L, 333L), (1..3).map(impar::montoDeCuota))
    }

    @Test
    fun `calcula que cuota cae en cada mes`() {
        val heladera = gasto()
        assertNull(heladera.cuotaEn(YearMonth.of(2026, 10)))
        assertEquals(1, heladera.cuotaEn(noviembre))
        assertEquals(3, heladera.cuotaEn(YearMonth.of(2027, 1)))
        assertNull(heladera.cuotaEn(YearMonth.of(2027, 2)))
    }

    @Test
    fun `una compra ya empezada arranca en su cuota actual`() {
        val empezada = gasto(cuotas = 6, cuotaInicial = 3)
        assertEquals(3, empezada.cuotaEn(noviembre))
        assertEquals(6, empezada.cuotaEn(YearMonth.of(2027, 2)))
        assertNull(empezada.cuotaEn(YearMonth.of(2027, 3)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un pago de cero`() {
        Pago("p1", "h1", "ana", "tomas", 0, LocalDate.of(2026, 11, 30))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza un pago a uno mismo`() {
        Pago("p1", "h1", "ana", "ana", 500_000, LocalDate.of(2026, 11, 30))
    }
}
