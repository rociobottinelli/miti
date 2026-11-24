package ar.edu.uade.miti.presentation.balance

import app.cash.turbine.test
import ar.edu.uade.miti.R
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.CampoDeGasto
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.usecase.gastos.DescartarConflictoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObservarConflictosDeGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerBalanceDelMesUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarPagoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RehacerMiEdicionUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.presentation.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BalanceViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val noviembre = YearMonth.of(2026, 11)
    private val reloj = relojEn(LocalDateTime.of(2026, 11, 23, 12, 0))
    private val hogares = FakeHogarRepository()
    private val gastos = FakeGastoRepository()

    private fun viewModel() = BalanceViewModel(
        ObtenerBalanceDelMesUseCase(hogares, gastos),
        ObservarIntegrantesUseCase(hogares),
        ObservarConflictosDeGastoUseCase(gastos),
        RegistrarPagoUseCase(hogares, gastos, reloj, IdsSecuenciales()),
        RehacerMiEdicionUseCase(gastos),
        DescartarConflictoUseCase(gastos),
        reloj,
    )

    /** uiState es WhileSubscribed: el test se suscribe como lo haría la pantalla. */
    private suspend fun BalanceViewModel.contenido() = uiState.filterIsInstance<BalanceUiState.Contenido>().first()

    @Test
    fun `muestra quien le debe a quien, lo que pago cada uno y los movimientos`() = runTest {
        gastos.gastos.value = listOf(
            Gasto("g1", HOGAR, "Supermercado", 2_400_000, TOMAS, Reparto.A_MEDIAS, mesInicio = noviembre),
            Gasto("g2", HOGAR, "Seguro", 3_000_000, ANA, Reparto.A_MEDIAS, cuotasTotales = 3, mesInicio = noviembre),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val estado = vm.contenido()

        // Tomás puso 24.000 a medias (Ana le debe 12.000); Ana puso la cuota de 10.000 a medias (Tomás le debe 5.000).
        assertEquals(DeudaEnPantalla(ANA, "Ana", TOMAS, "Tomás", 700_000), estado.resultado)
        assertEquals(listOf(1_000_000L, 2_400_000L), estado.pagadoPor.map { it.montoCentavos })
        val seguro = estado.movimientos.filterIsInstance<Movimiento.CuotaDeGasto>().single { it.gastoId == "g2" }
        assertEquals(1_000_000L, seguro.montoCentavos)
        assertTrue(seguro.enCuotas)
        assertFalse(estado.puedeAvanzar)
    }

    @Test
    fun `sin la otra persona no hay balance`() = runTest {
        hogares.integrantes.value = listOf(Integrante(ANA, HOGAR, "Ana", "ana@mail.com"))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(BalanceUiState.SinOtraPersona, vm.uiState.first { it != BalanceUiState.Cargando })
    }

    @Test
    fun `no se puede pasar al mes que viene pero si volver`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.mesSiguiente()
        assertEquals(noviembre, vm.contenido().mes)
        vm.mesAnterior()
        assertEquals(noviembre.minusMonths(1), vm.uiState.first { (it as? BalanceUiState.Contenido)?.mes == noviembre.minusMonths(1) }.let { (it as BalanceUiState.Contenido).mes })
    }

    @Test
    fun `registrar pago propone saldar la deuda y al guardar queda a mano`() = runTest {
        gastos.gastos.value = listOf(Gasto("g1", HOGAR, "Super", 2_400_000, TOMAS, Reparto.A_MEDIAS, mesInicio = noviembre))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.contenido()

        vm.abrirPago()
        val pago = vm.contenido().pago!!
        assertEquals(ANA, pago.deId)
        assertEquals("12000", pago.monto)
        assertEquals(LocalDate.of(2026, 11, 23), pago.fecha)

        vm.eventos.test {
            vm.registrarElPago()
            assertEquals(EventoDeBalance.Mensaje(R.string.pago_registrado), awaitItem())
        }
        val estado = vm.uiState.first { (it as? BalanceUiState.Contenido)?.pago == null } as BalanceUiState.Contenido
        assertNull(estado.resultado)
        assertEquals(1, gastos.pagos.value.size)
    }

    @Test
    fun `un pago sin monto valido no se registra`() = runTest {
        gastos.gastos.value = listOf(Gasto("g1", HOGAR, "Super", 2_400_000, TOMAS, Reparto.A_MEDIAS, mesInicio = noviembre))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.contenido()
        vm.abrirPago()

        vm.onMontoDelPagoChange("0")
        vm.registrarElPago()

        assertEquals(R.string.pago_error_monto, vm.contenido().pago?.error)
        assertTrue(gastos.pagos.value.isEmpty())
    }

    @Test
    fun `cambiar la direccion invierte quien le paga a quien`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.contenido()
        vm.abrirPago()
        val antes = vm.contenido().pago!!

        vm.cambiarDireccionDelPago()

        val despues = vm.contenido().pago!!
        assertEquals(antes.deId, despues.aId)
        assertEquals(antes.aId, despues.deId)
    }

    @Test
    fun `los conflictos aparecen arriba y se cierran al elegir`() = runTest {
        val actual = Gasto("g1", HOGAR, "Heladera", 3_100_000, ANA, Reparto.A_MEDIAS, mesInicio = noviembre)
        gastos.gastos.value = listOf(actual)
        val conflicto = ConflictoDeGasto("c1", actual, actual.copy(montoTotalCentavos = 2_900_000), false)
        gastos.conflictos.value = listOf(conflicto)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val enPantalla = vm.contenido().conflictos.single()
        assertEquals(2_900_000L, enPantalla.miMontoCentavos)
        assertEquals(3_100_000L, enPantalla.montoActualCentavos)
        assertEquals(
            listOf(DiferenciaDeGasto(CampoDeGasto.MONTO, ValorDeCampo.Pesos(2_900_000), ValorDeCampo.Pesos(3_100_000))),
            enPantalla.diferencias,
        )

        vm.rehacer(conflicto)

        val estado = vm.uiState.first { (it as? BalanceUiState.Contenido)?.conflictos?.isEmpty() == true } as BalanceUiState.Contenido
        assertTrue(estado.conflictos.isEmpty())
        assertEquals(2_900_000L, gastos.gastos.value.single().montoTotalCentavos)
    }

    @Test
    fun `un conflicto por quien pago muestra los dos pagadores y no el monto`() = runTest {
        val actual = Gasto("g1", HOGAR, "Heladera", 3_100_000, ANA, Reparto.A_MEDIAS, mesInicio = noviembre)
        gastos.gastos.value = listOf(actual)
        gastos.conflictos.value = listOf(ConflictoDeGasto("c1", actual, actual.copy(pagadorId = TOMAS), false))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(
            listOf(DiferenciaDeGasto(CampoDeGasto.PAGADOR, ValorDeCampo.Persona("Tomás"), ValorDeCampo.Persona("Ana"))),
            vm.contenido().conflictos.single().diferencias,
        )
    }

    @Test
    fun `un conflicto por reparto muestra de quien era el gasto en cada version`() = runTest {
        val actual = Gasto("g1", HOGAR, "Heladera", 3_100_000, ANA, Reparto.A_MEDIAS, mesInicio = noviembre)
        gastos.gastos.value = listOf(actual)
        val mia = actual.copy(reparto = Reparto.UNO_SOLO, beneficiarioId = TOMAS)
        gastos.conflictos.value = listOf(ConflictoDeGasto("c1", actual, mia, false))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(
            listOf(DiferenciaDeGasto(CampoDeGasto.REPARTO, ValorDeCampo.SoloDe("Tomás"), ValorDeCampo.AMedias)),
            vm.contenido().conflictos.single().diferencias,
        )
    }

    @Test
    fun `si el gasto ya no esta no hay diferencias que mostrar`() = runTest {
        val mia = Gasto("g1", HOGAR, "Heladera", 2_900_000, ANA, Reparto.A_MEDIAS, mesInicio = noviembre)
        gastos.conflictos.value = listOf(ConflictoDeGasto("c1", actual = null, miVersion = mia, eraBorrado = false))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val enPantalla = vm.contenido().conflictos.single()
        assertEquals(null, enPantalla.montoActualCentavos)
        assertTrue(enPantalla.diferencias.isEmpty())
    }
}
