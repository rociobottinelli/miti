package ar.edu.uade.miti.presentation.gastos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ar.edu.uade.miti.R
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakeSesionRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.usecase.gastos.EditarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.EliminarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import ar.edu.uade.miti.presentation.MainDispatcherRule
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormularioDeGastoViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val hogares = FakeHogarRepository()
    private val gastos = FakeGastoRepository()
    private val reloj = relojEn(LocalDateTime.of(2026, 11, 23, 12, 0))

    private fun viewModel(gastoId: String? = null) = FormularioDeGastoViewModel(
        SavedStateHandle(mapOf("gastoId" to gastoId)),
        RegistrarGastoUseCase(hogares, gastos, reloj, IdsSecuenciales()),
        EditarGastoUseCase(gastos),
        ObtenerGastoUseCase(gastos),
        EliminarGastoUseCase(gastos),
        ObservarIntegrantesUseCase(hogares),
        ObservarUsuarioUseCase(FakeSesionRepository()),
    )

    @Test
    fun `al abrir carga los integrantes y propone que pago quien usa el celular`() {
        val estado = viewModel().uiState.value

        assertFalse(estado.cargando)
        assertEquals(listOf("Ana", "Tomás"), estado.integrantes.map { it.nombre })
        assertEquals(ANA, estado.pagadorId)
        assertNull(estado.beneficiarioId)
    }

    @Test
    fun `una solicitud invalida nunca invoca guardar`() {
        val vm = viewModel()
        vm.onMontoChange("abc")
        vm.onDescripcionChange("   ")

        vm.onGuardarClick()

        assertEquals(ErroresDeGasto(monto = R.string.gasto_error_monto, descripcion = R.string.gasto_error_descripcion), vm.uiState.value.errores)
        assertTrue(gastos.gastos.value.isEmpty())
    }

    @Test
    fun `cuotas con la actual mayor al total no se guardan`() {
        val vm = viewModel()
        vm.onMontoChange("30000")
        vm.onDescripcionChange("Heladera")
        vm.onEnCuotasChange(true)
        vm.onCuotasTotalesChange("3")
        vm.onCuotaActualChange("4")

        vm.onGuardarClick()

        assertEquals(R.string.gasto_error_cuota_actual, vm.uiState.value.errores.cuotas)
        assertTrue(gastos.gastos.value.isEmpty())
    }

    @Test
    fun `al corregir un campo se borra su error`() {
        val vm = viewModel()
        vm.onGuardarClick()
        vm.onMontoChange("100")
        assertNull(vm.uiState.value.errores.monto)
    }

    @Test
    fun `guardar un gasto valido lo registra y recien despues avisa que se guardo`() = runTest {
        val vm = viewModel()
        vm.onMontoChange("18.750,50")
        vm.onDescripcionChange(" Supermercado ")
        vm.onPagadorChange(TOMAS)
        vm.onRepartoChange(ANA)

        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.Guardado, awaitItem())
        }
        val gasto = gastos.gastos.value.single()
        assertEquals("Supermercado", gasto.descripcion)
        assertEquals(1_875_050L, gasto.montoTotalCentavos)
        assertEquals(TOMAS, gasto.pagadorId)
        assertEquals(Reparto.UNO_SOLO, gasto.reparto)
        assertEquals(ANA, gasto.beneficiarioId)
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `si la base falla al guardar avisa y deja volver a intentar`() = runTest {
        gastos.falla = Resource.Error("No se pudo guardar en el celular.")
        val vm = viewModel()
        vm.onMontoChange("100")
        vm.onDescripcionChange("Pan")

        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.NoSePudo("No se pudo guardar en el celular."), awaitItem())
        }
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `editar carga el gasto y guarda los cambios sobre el mismo id`() = runTest {
        gastos.gastos.value = listOf(
            Gasto("g1", HOGAR, "Heladera", 3_000_000, TOMAS, Reparto.A_MEDIAS, cuotasTotales = 3, cuotaInicial = 2, mesInicio = YearMonth.of(2026, 10)),
        )
        val vm = viewModel(gastoId = "g1")
        val cargado = vm.uiState.value
        assertTrue(cargado.editando)
        assertEquals("30000", cargado.monto)
        assertTrue(cargado.enCuotas)
        assertEquals("2", cargado.cuotaActual)

        vm.onMontoChange("31000")
        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.Guardado, awaitItem())
        }
        val editado = gastos.gastos.value.single()
        assertEquals("g1", editado.id)
        assertEquals(3_100_000L, editado.montoTotalCentavos)
        assertEquals(YearMonth.of(2026, 10), editado.mesInicio)
    }

    @Test
    fun `editar un gasto que ya no existe lo avisa`() {
        assertEquals(R.string.gasto_error_ya_no_existe, viewModel(gastoId = "nada").uiState.value.errorDeCarga)
    }

    @Test
    fun `borrar el gasto que se edita lo saca y vuelve`() = runTest {
        gastos.gastos.value = listOf(Gasto("g1", HOGAR, "Pan", 100, ANA, Reparto.A_MEDIAS, mesInicio = YearMonth.of(2026, 11)))
        val vm = viewModel(gastoId = "g1")

        vm.eventos.test {
            vm.onBorrarClick()
            assertEquals(EventoDeFormulario.Borrado, awaitItem())
        }
        assertTrue(gastos.gastos.value.isEmpty())
    }
}
