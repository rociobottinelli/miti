package ar.edu.uade.miti.presentation.gastos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ar.edu.uade.miti.R
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.FakeSesionRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.usecase.gastos.EditarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.EliminarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.TerminarCompraUseCase
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
    private val pendientes = FakePendienteRepository()
    private val reloj = relojEn(LocalDateTime.of(2026, 11, 23, 12, 0))

    private fun viewModel(gastoId: String? = null, deCompra: Boolean = false): FormularioDeGastoViewModel {
        val registrar = RegistrarGastoUseCase(hogares, gastos, reloj, IdsSecuenciales())
        return FormularioDeGastoViewModel(
            SavedStateHandle(mapOf("gastoId" to gastoId, "deCompra" to deCompra)),
            registrar,
            EditarGastoUseCase(gastos),
            ObtenerGastoUseCase(gastos),
            EliminarGastoUseCase(gastos),
            TerminarCompraUseCase(pendientes, registrar),
            ObservarIntegrantesUseCase(hogares),
            ObservarUsuarioUseCase(FakeSesionRepository()),
        )
    }

    private fun compra(id: String, titulo: String, enCarrito: Boolean) =
        Pendiente(id, HOGAR, TipoPendiente.COMPRA, titulo, enCarrito = enCarrito)

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

    // --- Gasto de la compra (flujo 3) ---

    @Test
    fun `el gasto de una compra la cierra y vincula lo que estaba en el carrito`() = runTest {
        pendientes.pendientes.value = listOf(compra("p1", "Leche", enCarrito = true), compra("p2", "Pan", enCarrito = false))
        val vm = viewModel(deCompra = true)
        assertTrue(vm.uiState.value.deCompra)
        vm.onMontoChange("24000")
        vm.onDescripcionChange("Supermercado")

        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.Guardado, awaitItem())
        }
        val gasto = gastos.gastos.value.single()
        val leche = pendientes.pendientes.value.single { it.id == "p1" }
        val pan = pendientes.pendientes.value.single { it.id == "p2" }
        assertTrue(leche.resuelto)
        assertEquals(gasto.id, leche.gastoId)
        // Lo que no estaba en el carrito sigue en la lista, sin gasto.
        assertFalse(pan.resuelto)
        assertNull(pan.gastoId)
    }

    @Test
    fun `si el carrito ya se vacio no registra el gasto y lo avisa`() = runTest {
        // Por ejemplo, la otra persona terminó la compra mientras se completaba el formulario.
        val vm = viewModel(deCompra = true)
        vm.onMontoChange("24000")
        vm.onDescripcionChange("Supermercado")

        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.NoSePudo("No hay nada en el carrito"), awaitItem())
        }
        assertTrue(gastos.gastos.value.isEmpty())
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `si no se puede guardar el gasto la compra queda abierta`() = runTest {
        pendientes.pendientes.value = listOf(compra("p1", "Leche", enCarrito = true))
        gastos.falla = Resource.Error("No se pudo guardar en el celular.")
        val vm = viewModel(deCompra = true)
        vm.onMontoChange("24000")
        vm.onDescripcionChange("Supermercado")

        vm.eventos.test {
            vm.onGuardarClick()
            assertEquals(EventoDeFormulario.NoSePudo("No se pudo guardar en el celular."), awaitItem())
        }
        val leche = pendientes.pendientes.value.single()
        assertTrue(leche.enCarrito)
        assertFalse(leche.resuelto)
    }
}
