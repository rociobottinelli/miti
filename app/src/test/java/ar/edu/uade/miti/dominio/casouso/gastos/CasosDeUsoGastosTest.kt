package ar.edu.uade.miti.dominio.casouso.gastos

import ar.edu.uade.miti.dominio.fakes.ANA
import ar.edu.uade.miti.dominio.fakes.FakeGastoRepository
import ar.edu.uade.miti.dominio.fakes.FakeHogarRepository
import ar.edu.uade.miti.dominio.fakes.HOGAR
import ar.edu.uade.miti.dominio.fakes.IdsSecuenciales
import ar.edu.uade.miti.dominio.fakes.TOMAS
import ar.edu.uade.miti.dominio.fakes.relojEn
import ar.edu.uade.miti.dominio.modelo.Deuda
import ar.edu.uade.miti.dominio.modelo.Integrante
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Reparto
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CasosDeUsoGastosTest {

    private val reloj = relojEn(LocalDate.of(2026, 11, 5).atTime(10, 0))
    private val hogares = FakeHogarRepository()
    private val gastos = FakeGastoRepository()
    private val ids = IdsSecuenciales()

    @Test
    fun `registrar un gasto sin mes lo asigna al mes actual`() = runTest {
        val gasto = RegistrarGastoUseCase(hogares, gastos, reloj, ids)(
            DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS),
        )

        assertEquals("id-1", gasto.id)
        assertEquals(HOGAR, gasto.hogarId)
        assertEquals(YearMonth.of(2026, 11), gasto.mesInicio)
        assertEquals(listOf(gasto), gastos.gastos.value)
    }

    @Test
    fun `registrar un pago sin fecha usa la de hoy`() = runTest {
        val pago = RegistrarPagoUseCase(hogares, gastos, reloj, ids)(ANA, TOMAS, 500_000)

        assertEquals(Pago("id-1", HOGAR, ANA, TOMAS, 500_000, LocalDate.of(2026, 11, 5)), pago)
    }

    @Test
    fun `el balance se actualiza solo al registrar gastos y pagos`() = runTest {
        val registrarGasto = RegistrarGastoUseCase(hogares, gastos, reloj, ids)
        val registrarPago = RegistrarPagoUseCase(hogares, gastos, reloj, ids)

        ObtenerBalanceDelMesUseCase(hogares, gastos)(YearMonth.of(2026, 11)).test {
            assertNull(awaitItem()?.resultado)

            registrarGasto(DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS))
            assertEquals(Deuda(ANA, TOMAS, 1_200_000), awaitItem()?.resultado)

            registrarPago(ANA, TOMAS, 1_200_000)
            assertNull(awaitItem()?.resultado)
        }
    }

    @Test
    fun `sin dos integrantes no hay balance`() = runTest {
        val soloAna = FakeHogarRepository(integrantes = listOf(Integrante(ANA, HOGAR, "Ana", "ana@mail.com")))

        ObtenerBalanceDelMesUseCase(soloAna, gastos)(YearMonth.of(2026, 11)).test {
            assertNull(awaitItem())
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `no se puede registrar un gasto sin hogar activo`() = runTest {
        RegistrarGastoUseCase(FakeHogarRepository(hogarActivo = null), gastos, reloj, ids)(
            DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS),
        )
    }
}
