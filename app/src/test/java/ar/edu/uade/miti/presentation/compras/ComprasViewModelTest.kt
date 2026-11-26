package ar.edu.uade.miti.presentation.compras

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import ar.edu.uade.miti.domain.repository.SituacionDeSincronizacion
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.EliminarPendienteUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.MarcarEnCarritoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RestaurarPendienteUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarListaDeComprasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarCompraUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.TerminarCompraUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.SincronizarAhoraUseCase
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComprasViewModelTest {

    private val miercoles = LocalDate.of(2026, 9, 30)
    private val reloj = Clock.fixed(miercoles.atTime(9, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val planificador = TestCoroutineScheduler()
    private val hogares = FakeHogarRepository()
    private val pendientes = PendientesQuePuedenFallar(FakePendienteRepository())
    private val sync = SyncFalso(miercoles.atTime(9, 25).toInstant(ZoneOffset.UTC))
    private val ids = IdsSecuenciales()

    @Before
    fun usarDespachadorDePrueba() {
        Dispatchers.setMain(UnconfinedTestDispatcher(planificador))
    }

    @After
    fun restaurarDespachador() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ComprasViewModel(
        observarLista = ObservarListaDeComprasUseCase(pendientes),
        registrarCompra = RegistrarCompraUseCase(hogares, pendientes, ids),
        marcarEnCarrito = MarcarEnCarritoUseCase(pendientes),
        terminarCompra = TerminarCompraUseCase(pendientes, RegistrarGastoUseCase(hogares, FakeGastoRepository(), reloj, ids)),
        eliminarPendiente = EliminarPendienteUseCase(pendientes),
        restaurarPendiente = RestaurarPendienteUseCase(pendientes),
        observarEstadoDeSincronizacion = ObservarEstadoDeSincronizacionUseCase(sync, reloj),
        observarItemsSinEnviar = ObservarItemsSinEnviarUseCase(sync),
        sincronizarAhora = SincronizarAhoraUseCase(sync),
        reloj = reloj,
    )

    private fun ComprasViewModel.agregar(titulo: String) {
        escribir(titulo)
        agregar()
    }

    @Test
    fun `sin items la lista esta vacia`() = runTest(planificador) {
        viewModel().uiState.test {
            val estado = contenido()
            assertTrue(estado.vacia)
            assertEquals(null, estado.aviso)
        }
    }

    @Test
    fun `agregar un item lo suma a la lista y vacia el campo`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            vm.agregar("Tomate")
            vm.agregar("Papa")

            val estado = expectMostRecentItem() as ComprasUiState.Contenido
            assertEquals(listOf("Papa", "Tomate"), estado.porComprar.map { it.titulo })
            assertEquals("", estado.nuevoItem)
        }
    }

    @Test
    fun `borrar un item lo saca y deshacer lo devuelve`() = runTest(planificador) {
        val vm = viewModel()
        vm.agregar("Lehce")
        val id = pendientes.real.pendientes.value.single().id

        vm.eventos.test {
            vm.borrar(id)
            assertEquals(EventoDeCompras.Borrado("Lehce"), awaitItem())
        }
        assertTrue(pendientes.real.pendientes.value.isEmpty())

        vm.deshacerBorrado()
        assertEquals(listOf("Lehce"), pendientes.real.pendientes.value.map { it.titulo })
    }

    @Test
    fun `un item repetido no se agrega, avisa y conserva el texto`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            vm.eventos.test {
                vm.agregar("Leche")
                vm.agregar("leche")
                assertEquals(EventoDeCompras.NoSePudo(RegistrarCompraUseCase.YA_ESTA_EN_LA_LISTA), awaitItem())
            }
            val estado = expectMostRecentItem() as ComprasUiState.Contenido
            assertEquals(1, estado.total)
            assertEquals("leche", estado.nuevoItem)
        }
    }

    @Test
    fun `tachar pasa al carrito y destildar lo devuelve`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            vm.agregar("Papa")
            vm.agregar("Tomate")
            val papa = (expectMostRecentItem() as ComprasUiState.Contenido).porComprar.first { it.titulo == "Papa" }

            vm.tachar(papa.id, enCarrito = true)
            val tachado = expectMostRecentItem() as ComprasUiState.Contenido
            assertEquals(listOf("Tomate"), tachado.porComprar.map { it.titulo })
            assertEquals(listOf("Papa"), tachado.enCarrito.map { it.titulo })

            vm.tachar(papa.id, enCarrito = false)
            assertEquals(2, (expectMostRecentItem() as ComprasUiState.Contenido).porComprar.size)
        }
    }

    @Test
    fun `terminar la compra cierra lo del carrito y deja lo que falta`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            vm.agregar("Papa")
            vm.agregar("Tomate")
            val papa = (expectMostRecentItem() as ComprasUiState.Contenido).porComprar.first { it.titulo == "Papa" }
            vm.tachar(papa.id, enCarrito = true)

            vm.eventos.test {
                vm.terminar()
                assertEquals(EventoDeCompras.CompraTerminada, awaitItem())
            }
            val estado = expectMostRecentItem() as ComprasUiState.Contenido
            assertEquals(emptyList<ItemDeCompra>(), estado.enCarrito)
            assertEquals(listOf("Tomate"), estado.porComprar.map { it.titulo })
            assertFalse(estado.terminando)
        }
    }

    @Test
    fun `terminar con el carrito vacio avisa`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            contenido()
            vm.eventos.test {
                vm.terminar()
                assertEquals(EventoDeCompras.NoSePudo("No hay nada en el carrito"), awaitItem())
            }
            assertFalse((expectMostRecentItem() as ComprasUiState.Contenido).terminando)
        }
    }

    @Test
    fun `sin conexion se avisa con la hora de la ultima sincronizacion`() = runTest(planificador) {
        sync.situacion.value = sync.situacion.value.copy(conConexion = false)
        viewModel().uiState.test {
            assertEquals(AvisoDeSync.SinConexion(miercoles.atTime(9, 25)), contenido().aviso)
        }
    }

    @Test
    fun `lo que no llego al otro celular se marca como pendiente`() = runTest(planificador) {
        val vm = viewModel()
        vm.uiState.test {
            vm.agregar("Papa")
            val papa = (expectMostRecentItem() as ComprasUiState.Contenido).porComprar.single()
            sync.situacion.value = sync.situacion.value.copy(idsSinEnviar = setOf(papa.id))

            assertTrue((expectMostRecentItem() as ComprasUiState.Contenido).porComprar.single().pendienteDeSync)
        }
    }

    @Test
    fun `si no se puede leer la lista muestra el error y se puede reintentar`() = runTest(planificador) {
        pendientes.fallar = true
        val vm = viewModel()
        vm.uiState.test {
            var estado = awaitItem()
            while (estado == ComprasUiState.Cargando) estado = awaitItem()
            assertEquals(ComprasUiState.Error, estado)

            pendientes.fallar = false
            vm.reintentar()
            assertTrue(contenido().vacia)
        }
    }

    @Test
    fun `actualizar pide sincronizar`() = runTest(planificador) {
        viewModel().sincronizar()
        assertEquals(1, sync.pedidosDeSincronizar)
    }

    private suspend fun ReceiveTurbine<ComprasUiState>.contenido(): ComprasUiState.Contenido {
        var estado = awaitItem()
        while (estado !is ComprasUiState.Contenido) estado = awaitItem()
        return estado
    }

    /** La base real del fake, con la opción de que leerla falle. */
    private class PendientesQuePuedenFallar(val real: FakePendienteRepository) : PendienteRepository by real {
        var fallar = false
        override fun observarPendientes() = if (fallar) flow { throw IOException("Room no responde") } else real.observarPendientes()
    }

    private class SyncFalso(ultima: Instant) : SincronizacionRepository {
        val situacion = MutableStateFlow(SituacionDeSincronizacion(ultima, conConexion = true, sincronizando = false, reintentando = false, idsSinEnviar = emptySet()))
        var pedidosDeSincronizar = 0
        override fun observar(): Flow<SituacionDeSincronizacion> = situacion
        override suspend fun sincronizarAhora() { pedidosDeSincronizar++ }
    }
}
