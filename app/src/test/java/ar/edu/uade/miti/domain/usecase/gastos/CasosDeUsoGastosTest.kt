package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import app.cash.turbine.test
import ar.edu.uade.miti.domain.fakes.exito
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Deuda
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.Pago
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerBalanceDelMesUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarPagoUseCase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeUsoGastosTest {

    private val reloj = relojEn(LocalDate.of(2026, 11, 5).atTime(10, 0))
    private val hogares = FakeHogarRepository()
    private val gastos = FakeGastoRepository()
    private val ids = IdsSecuenciales()

    @Test
    fun `registrar un gasto sin mes lo asigna al mes actual`() = runTest {
        val gasto = RegistrarGastoUseCase(hogares, gastos, reloj, ids)(
            DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS),
        ).exito()

        assertEquals("id-1", gasto.id)
        assertEquals(HOGAR, gasto.hogarId)
        assertEquals(YearMonth.of(2026, 11), gasto.mesInicio)
        assertEquals(listOf(gasto), gastos.gastos.value)
    }

    @Test
    fun `registrar un pago sin fecha usa la de hoy`() = runTest {
        val pago = RegistrarPagoUseCase(hogares, gastos, reloj, ids)(ANA, TOMAS, 500_000).exito()

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

    @Test
    fun `no se puede registrar un gasto sin hogar activo`() = runTest {
        val resultado = RegistrarGastoUseCase(FakeHogarRepository(hogarActivo = null), gastos, reloj, ids)(
            DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS),
        )

        assertTrue(resultado is Resource.Error)
        assertEquals(emptyList<Gasto>(), gastos.gastos.value)
    }

    @Test
    fun `un gasto invalido devuelve el error y nunca se guarda`() = runTest {
        val resultado = RegistrarGastoUseCase(hogares, gastos, reloj, ids)(
            DatosDeGasto("Heladera", 3_000_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS, cuotasTotales = 3, cuotaInicial = 4),
        )

        assertEquals("La cuota actual no puede ser mayor al total", (resultado as Resource.Error).message)
        assertEquals(emptyList<Gasto>(), gastos.gastos.value)
    }

    @Test
    fun `si la base falla al guardar el gasto, llega el error`() = runTest {
        gastos.falla = Resource.Error("No se pudo guardar")

        val resultado = RegistrarGastoUseCase(hogares, gastos, reloj, ids)(
            DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS),
        )

        assertEquals(Resource.Error("No se pudo guardar"), resultado)
    }

    @Test
    fun `un pago de un integrante a si mismo es invalido`() = runTest {
        val resultado = RegistrarPagoUseCase(hogares, gastos, reloj, ids)(ANA, ANA, 500_000)

        assertTrue(resultado is Resource.Error)
        assertEquals(emptyList<Pago>(), gastos.pagos.value)
    }
}
