package ar.edu.uade.miti.presentation.hoy

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.FakeRutinaRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import ar.edu.uade.miti.domain.usecase.ObservarSiHayItemsUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarProximasTareasUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.MarcarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HoyViewModelTest {

    private val miercoles = LocalDate.of(2026, 9, 30)

    private val planificador = TestCoroutineScheduler()
    private val rutinas = FakeRutinaRepository()
    private val pendientes = FakePendienteRepository()
    private val hogar = FakeHogarRepository()
    private val sesion = FakeSesion(Usuario(ANA, "ana@mail.com"))

    private val pancho = Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2)
    private val basura = Rutina("basura", HOGAR, "Sacar la basura", Frecuencia.DIARIA, vecesPorDia = 1)
    private val abl = tarea("abl", "Pagar ABL", TOMAS, miercoles.plusDays(1).atTime(18, 0))
    private val veterinario = tarea("vete", "Llevar a Pancho al veterinario", ANA, miercoles.plusDays(3).atTime(10, 0))

    @Before
    fun usarDespachadorDePrueba() {
        Dispatchers.setMain(UnconfinedTestDispatcher(planificador))
    }

    @After
    fun restaurarDespachador() {
        Dispatchers.resetMain()
    }

    /** Un reloj que avanza con el tiempo virtual de las corrutinas: permite probar la medianoche. */
    private fun relojDesde(inicio: LocalDateTime): Clock = object : Clock() {
        private val base = inicio.toInstant(ZoneOffset.UTC)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = base.plusMillis(planificador.currentTime)
    }

    private fun viewModel(desde: LocalDateTime = miercoles.atTime(9, 30)): HoyViewModel {
        val reloj = relojDesde(desde)
        return HoyViewModel(
            observarRutinasDelDia = ObservarRutinasDelDiaUseCase(rutinas),
            observarProximasTareas = ObservarProximasTareasUseCase(pendientes),
            observarSiHayItems = ObservarSiHayItemsUseCase(rutinas, pendientes),
            observarIntegrantes = ObservarIntegrantesUseCase(hogar),
            observarUsuario = ObservarUsuarioUseCase(sesion),
            marcarRutina = MarcarRutinaUseCase(rutinas, reloj, IdsSecuenciales()),
            reloj = reloj,
        )
    }

    @Test
    fun `sin nada cargado muestra el estado vacio con la fecha y el avatar propio`() = runTest(planificador) {
        viewModel().uiState.test {
            val estado = primerEstado() as HoyUiState.Vacio
            assertEquals(miercoles, estado.encabezado.fecha)
            assertEquals(IntegranteEnPantalla(ANA, "Ana", 0), estado.encabezado.yo)
            assertEquals(IntegranteEnPantalla(TOMAS, "Tomás", 1), estado.encabezado.otro)
        }
    }

    @Test
    fun `con rutinas y tareas muestra el contenido con las completas al final`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura, pancho)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", ANA, miercoles.atTime(8, 0)))
        pendientes.pendientes.value = listOf(veterinario, abl)

        viewModel().uiState.test {
            val estado = primerEstado() as HoyUiState.Contenido
            assertEquals(listOf("pancho", "basura"), estado.rutinas.map { it.id })
            assertEquals(Marca("Ana", LocalTime.of(8, 0)), estado.rutinas.last().ultimaMarca)
            assertEquals(1, estado.rutinasCompletas)
            assertEquals(listOf("abl", "vete"), estado.tareas.map { it.id })
            assertEquals(CuandoVence.Maniana(LocalTime.of(18, 0)), estado.tareas.first().cuando)
            assertEquals("Tomás", estado.tareas.first().responsable?.nombre)
        }
    }

    @Test
    fun `marcar una rutina se refleja enseguida y registra quien usa el celular`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(0, (primerEstado() as HoyUiState.Contenido).rutinas.single().hechas)

            vm.marcar("pancho")

            val rutina = (awaitItem() as HoyUiState.Contenido).rutinas.single()
            assertEquals(1, rutina.hechas)
            assertEquals(Marca("Ana", LocalTime.of(9, 30)), rutina.ultimaMarca)
            assertEquals(ANA, rutinas.registros.value.single().integranteId)
        }
    }

    @Test
    fun `marcar una rutina ya completa avisa y no agrega otra marca`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura, pancho)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", TOMAS, miercoles.atTime(8, 0)))
        val vm = viewModel()

        vm.eventos.test {
            vm.marcar("basura")
            assertEquals(EventoDeHoy.RUTINA_YA_COMPLETA, awaitItem())
        }
        assertEquals(1, rutinas.registros.value.size)
    }

    @Test
    fun `si la base falla al marcar avisa con un mensaje`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        rutinas.falla = Resource.Error("disco lleno")
        val vm = viewModel()

        vm.eventos.test {
            vm.marcar("pancho")
            assertEquals(EventoDeHoy.NO_SE_PUDO_MARCAR, awaitItem())
        }
    }

    @Test
    fun `con todo hecho y sin tareas pendientes muestra todo al dia`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", ANA, miercoles.atTime(8, 0)))

        viewModel().uiState.test {
            assertTrue(primerEstado() is HoyUiState.TodoAlDia)
        }
    }

    @Test
    fun `a la medianoche las rutinas vuelven a estar pendientes sin reabrir la pantalla`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", ANA, miercoles.atTime(21, 3)))

        viewModel(desde = miercoles.atTime(23, 59)).uiState.test {
            assertTrue(primerEstado() is HoyUiState.TodoAlDia)

            planificador.advanceTimeBy(61_000)

            val jueves = awaitItem() as HoyUiState.Contenido
            assertEquals(miercoles.plusDays(1), jueves.encabezado.fecha)
            assertEquals(0, jueves.rutinas.single().hechas)
        }
    }

    @Test
    fun `el filtro Mias deja solo mis tareas y conserva los chips`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        pendientes.pendientes.value = listOf(abl, veterinario)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(2, (primerEstado() as HoyUiState.Contenido).tareas.size)

            vm.elegirFiltro(FiltroDeTareas.MIAS)

            val filtrado = expectMostRecentItem() as HoyUiState.Contenido
            assertEquals(FiltroDeTareas.MIAS, filtrado.filtro)
            assertEquals(listOf("vete"), filtrado.tareas.map { it.id })
        }
    }

    @Test
    fun `con el filtro del otro sin tareas suyas sigue en contenido y no en todo al dia`() = runTest(planificador) {
        pendientes.pendientes.value = listOf(veterinario)
        val vm = viewModel()

        vm.uiState.test {
            primerEstado()
            vm.elegirFiltro(FiltroDeTareas.DEL_OTRO)

            val filtrado = expectMostRecentItem() as HoyUiState.Contenido
            assertTrue(filtrado.tareas.isEmpty())
        }
    }

    @Test
    fun `si fallan los datos muestra error y reintentar vuelve a cargar`() = runTest(planificador) {
        val rutinasQueFallan = object : FakeRutinaRepository() {
            var fallar = true
            override fun observarRutinas(): Flow<List<Rutina>> =
                if (fallar) flow { throw IllegalStateException("base corrupta") } else super.observarRutinas()
        }
        rutinasQueFallan.rutinas.value = listOf(pancho)
        val reloj = relojDesde(miercoles.atTime(9, 30))
        val vm = HoyViewModel(
            ObservarRutinasDelDiaUseCase(rutinasQueFallan),
            ObservarProximasTareasUseCase(pendientes),
            ObservarSiHayItemsUseCase(rutinasQueFallan, pendientes),
            ObservarIntegrantesUseCase(hogar),
            ObservarUsuarioUseCase(sesion),
            MarcarRutinaUseCase(rutinasQueFallan, reloj, IdsSecuenciales()),
            reloj,
        )

        vm.uiState.test {
            assertEquals(HoyUiState.Error, primerEstado())

            rutinasQueFallan.fallar = false
            vm.reintentar()

            assertTrue(awaitItem() is HoyUiState.Contenido)
        }
    }

    /** El estado inicial es Cargando hasta que responden los datos; los tests miran lo que viene después. */
    private suspend fun ReceiveTurbine<HoyUiState>.primerEstado(): HoyUiState {
        var estado = awaitItem()
        while (estado == HoyUiState.Cargando) estado = awaitItem()
        return estado
    }

    private fun tarea(id: String, titulo: String, responsable: String, cuando: LocalDateTime) =
        Pendiente(id, HOGAR, TipoPendiente.TAREA, titulo, responsableId = responsable, fechaHora = cuando)

    private class FakeSesion(usuario: Usuario?) : SesionRepository {
        private val usuario = MutableStateFlow(usuario)
        override fun observarUsuario(): Flow<Usuario?> = usuario
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = error("no se usa")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = error("no se usa")
        override suspend fun cerrarSesion() = error("no se usa")
    }
}
