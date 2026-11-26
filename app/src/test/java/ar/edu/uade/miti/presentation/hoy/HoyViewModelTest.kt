package ar.edu.uade.miti.presentation.hoy

import ar.edu.uade.miti.presentation.components.AvisoDeSync
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
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import ar.edu.uade.miti.domain.model.PermisosDeAvisos
import ar.edu.uade.miti.domain.usecase.avisos.ConsultarPermisosDeAvisosUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.SincronizarAhoraUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.repository.SituacionDeSincronizacion
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import ar.edu.uade.miti.domain.usecase.ObservarSiHayItemsUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.AlternarTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.CompletarTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarTareasHechasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarProximasTareasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ReabrirTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.EliminarPendienteUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.AlternarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.DeshacerMarcaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.EliminarRutinaUseCase
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
import org.junit.Assert.assertFalse
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
    private val sync = SyncFalso(miercoles.atTime(9, 25).toInstant(ZoneOffset.UTC))
    private val alarmas = AlarmasFalsas()

    private class AlarmasFalsas : AlarmasDeTareas {
        var permisos = PermisosDeAvisos(notificaciones = true, alarmasExactas = true)
        override suspend fun programarSolo(tareas: List<Pendiente>, avisosVigentes: Set<String>) = Unit
        override fun permisos() = permisos
    }

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
            alternarRutina = AlternarRutinaUseCase(
                rutinas, MarcarRutinaUseCase(rutinas, reloj, IdsSecuenciales()), DeshacerMarcaUseCase(rutinas, reloj), reloj,
            ),
            observarEstadoDeSincronizacion = ObservarEstadoDeSincronizacionUseCase(sync, reloj),
            observarItemsSinEnviar = ObservarItemsSinEnviarUseCase(sync),
            sincronizarAhora = SincronizarAhoraUseCase(sync),
            consultarPermisos = ConsultarPermisosDeAvisosUseCase(alarmas),
            observarTareasHechas = ObservarTareasHechasUseCase(pendientes),
            alternarTarea = AlternarTareaUseCase(pendientes, CompletarTareaUseCase(pendientes), ReabrirTareaUseCase(pendientes)),
            reabrirTarea = ReabrirTareaUseCase(pendientes),
            deshacerMarca = DeshacerMarcaUseCase(rutinas, reloj),
            eliminarRutina = EliminarRutinaUseCase(rutinas),
            eliminarPendiente = EliminarPendienteUseCase(pendientes),
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
    fun `tocar una rutina que completo solo el otro explica quien fue y no la cambia`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura, pancho)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", TOMAS, miercoles.atTime(8, 0)))
        val vm = viewModel()

        vm.eventos.test {
            vm.marcar("basura")
            assertEquals(EventoDeHoy.MarcadaPorOtro("Tomás"), awaitItem())
        }
        assertEquals(1, rutinas.registros.value.size)
    }

    @Test
    fun `tocar una rutina completa la destilda sacando solo mi marca`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        rutinas.registros.value = listOf(RegistroRutina("de-tomas", "pancho", TOMAS, miercoles.atTime(8, 0)))
        val vm = viewModel()

        vm.marcar("pancho")
        assertEquals(2, rutinas.registros.value.size)

        vm.marcar("pancho")
        assertEquals(listOf("de-tomas"), rutinas.registros.value.map { it.id })
    }

    @Test
    fun `tocar una tarea la tilda, sigue en la lista y otro toque la destilda`() = runTest(planificador) {
        pendientes.pendientes.value = listOf(abl, veterinario)
        val vm = viewModel()

        vm.uiState.test {
            primerEstado()
            vm.completar("abl")
            val tildada = expectMostRecentItem() as HoyUiState.Contenido
            // Las pendientes primero; la hecha queda al final, tildada.
            assertEquals(listOf("vete" to false, "abl" to true), tildada.tareas.map { it.id to it.hecha })

            vm.completar("abl")
            val destildada = expectMostRecentItem() as HoyUiState.Contenido
            assertEquals(listOf("abl" to false, "vete" to false), destildada.tareas.map { it.id to it.hecha })
        }
    }

    @Test
    fun `una tarea atrasada desaparece al hacerla y por eso se ofrece deshacer`() = runTest(planificador) {
        val atrasada = tarea("plomero", "Llamar al plomero", ANA, miercoles.minusDays(1).atTime(9, 0))
        pendientes.pendientes.value = listOf(atrasada, abl)
        val vm = viewModel()

        vm.eventos.test {
            vm.completar("plomero")
            assertEquals(EventoDeHoy.TareaHecha("plomero", "Llamar al plomero"), awaitItem())
        }
        vm.reabrir("plomero")
        assertEquals(false, pendientes.pendientes.value.single { it.id == "plomero" }.resuelto)
    }

    @Test
    fun `puedo deshacer solo si yo marque la rutina hoy`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho, basura)
        rutinas.registros.value = listOf(
            RegistroRutina("m1", "pancho", ANA, miercoles.atTime(8, 0)),
            RegistroRutina("m2", "basura", TOMAS, miercoles.atTime(8, 0)),
        )

        viewModel().uiState.test {
            val rutinasEnPantalla = (primerEstado() as HoyUiState.Contenido).rutinas.associateBy { it.id }
            assertTrue(rutinasEnPantalla.getValue("pancho").puedoDeshacer)
            assertFalse(rutinasEnPantalla.getValue("basura").puedoDeshacer)
        }
    }

    @Test
    fun `borrar una rutina o una tarea las saca y avisa`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho, basura)
        pendientes.pendientes.value = listOf(abl, veterinario)
        val vm = viewModel()

        vm.eventos.test {
            vm.borrarRutina("basura")
            assertEquals(EventoDeHoy.Borrado("Sacar la basura"), awaitItem())
            vm.borrarTarea("abl")
            assertEquals(EventoDeHoy.Borrado("Pagar ABL"), awaitItem())
        }
        assertEquals(listOf("pancho"), rutinas.rutinas.value.map { it.id })
        assertEquals(listOf("vete"), pendientes.pendientes.value.map { it.id })
    }

    @Test
    fun `si la base falla al marcar avisa con un mensaje`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        rutinas.falla = Resource.Error("disco lleno")
        val vm = viewModel()

        vm.eventos.test {
            vm.marcar("pancho")
            assertEquals(EventoDeHoy.NoSePudoMarcar, awaitItem())
        }
    }

    @Test
    fun `con todo hecho festeja pero deja la lista para poder destildar`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", ANA, miercoles.atTime(8, 0)))

        viewModel().uiState.test {
            val estado = primerEstado() as HoyUiState.Contenido
            assertTrue(estado.todoAlDia)
            assertTrue(estado.rutinas.single().completa)
        }
    }

    @Test
    fun `si no hay nada para hoy muestra todo al dia`() = runTest(planificador) {
        // Solo una rutina de los lunes, y hoy es miércoles.
        rutinas.rutinas.value = listOf(
            Rutina("lunes", HOGAR, "Cambiar las sábanas", Frecuencia.DIAS_DE_LA_SEMANA, setOf(java.time.DayOfWeek.MONDAY), 1),
        )

        viewModel().uiState.test {
            assertTrue(primerEstado() is HoyUiState.TodoAlDia)
        }
    }

    @Test
    fun `a la medianoche las rutinas vuelven a estar pendientes sin reabrir la pantalla`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(basura)
        rutinas.registros.value = listOf(RegistroRutina("m1", "basura", ANA, miercoles.atTime(21, 3)))

        viewModel(desde = miercoles.atTime(23, 59)).uiState.test {
            assertTrue((primerEstado() as HoyUiState.Contenido).todoAlDia)

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
            AlternarRutinaUseCase(
                rutinasQueFallan, MarcarRutinaUseCase(rutinasQueFallan, reloj, IdsSecuenciales()),
                DeshacerMarcaUseCase(rutinasQueFallan, reloj), reloj,
            ),
            ObservarEstadoDeSincronizacionUseCase(sync, reloj),
            ObservarItemsSinEnviarUseCase(sync),
            SincronizarAhoraUseCase(sync),
            ConsultarPermisosDeAvisosUseCase(alarmas),
            ObservarTareasHechasUseCase(pendientes),
            AlternarTareaUseCase(pendientes, CompletarTareaUseCase(pendientes), ReabrirTareaUseCase(pendientes)),
            ReabrirTareaUseCase(pendientes),
            DeshacerMarcaUseCase(rutinasQueFallan, reloj),
            EliminarRutinaUseCase(rutinasQueFallan),
            EliminarPendienteUseCase(pendientes),
            reloj,
        )

        vm.uiState.test {
            assertEquals(HoyUiState.Error, primerEstado())

            rutinasQueFallan.fallar = false
            vm.reintentar()

            assertTrue(awaitItem() is HoyUiState.Contenido)
        }
    }

    // Aviso de permisos (preentrega, sección 11)

    @Test
    fun `sin notificaciones avisa y al volver con el permiso dado el aviso se va`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        alarmas.permisos = PermisosDeAvisos(notificaciones = false, alarmasExactas = false)
        val vm = viewModel()

        vm.uiState.test {
            // Faltan los dos: se muestra el más grave, el de notificaciones.
            assertEquals(AvisoDePermisos.SIN_NOTIFICACIONES, (primerEstado() as HoyUiState.Contenido).encabezado.permisos)

            alarmas.permisos = PermisosDeAvisos(notificaciones = true, alarmasExactas = false)
            vm.revisarPermisos()
            assertEquals(AvisoDePermisos.SIN_ALARMAS_EXACTAS, (expectMostRecentItem() as HoyUiState.Contenido).encabezado.permisos)

            alarmas.permisos = PermisosDeAvisos(notificaciones = true, alarmasExactas = true)
            vm.revisarPermisos()
            assertEquals(null, (expectMostRecentItem() as HoyUiState.Contenido).encabezado.permisos)
        }
    }

    // Avisos de sincronización (C1, #19)

    @Test
    fun `sin conexion muestra el aviso con la hora de la ultima sincronizacion`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        sync.situacion.value = sync.situacion.value.copy(conConexion = false)

        viewModel().uiState.test {
            val estado = primerEstado() as HoyUiState.Contenido
            assertEquals(AvisoDeSync.SinConexion(miercoles.atTime(9, 25)), estado.encabezado.aviso)
        }
    }

    @Test
    fun `con todo al dia no hay aviso y un error al sincronizar se avisa con reintentar`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(null, (primerEstado() as HoyUiState.Contenido).encabezado.aviso)

            sync.situacion.value = sync.situacion.value.copy(reintentando = true)

            assertEquals(AvisoDeSync.NoSePudoSincronizar, (expectMostRecentItem() as HoyUiState.Contenido).encabezado.aviso)
            vm.sincronizar()
            assertEquals(1, sync.pedidosDeSincronizar)
        }
    }

    @Test
    fun `con datos desactualizados pide confirmar antes de marcar y no marca si cancela`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        // La última sincronización fue hace más de 30 minutos.
        sync.situacion.value = sync.situacion.value.copy(ultimaSincronizacion = miercoles.atTime(8, 15).toInstant(ZoneOffset.UTC))
        val vm = viewModel()

        vm.uiState.test {
            val antes = primerEstado() as HoyUiState.Contenido
            assertEquals(AvisoDeSync.Desactualizado(miercoles.atTime(8, 15)), antes.encabezado.aviso)

            vm.marcar("pancho")

            val confirmacion = (expectMostRecentItem() as HoyUiState.Contenido).confirmarMarca
            assertEquals(ConfirmacionDeMarca("pancho", "Darle de comer a Pancho", "Tomás", miercoles.atTime(8, 15)), confirmacion)
            assertTrue(rutinas.registros.value.isEmpty())

            vm.cancelarMarca()

            assertEquals(null, (expectMostRecentItem() as HoyUiState.Contenido).confirmarMarca)
            assertTrue(rutinas.registros.value.isEmpty())
        }
    }

    @Test
    fun `al confirmar con datos desactualizados se marca igual`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho)
        sync.situacion.value = sync.situacion.value.copy(ultimaSincronizacion = null)
        val vm = viewModel()

        vm.uiState.test {
            primerEstado()
            vm.marcar("pancho")
            vm.confirmarMarca()

            assertEquals(1, (expectMostRecentItem() as HoyUiState.Contenido).rutinas.single().hechas)
            assertEquals(1, rutinas.registros.value.size)
        }
    }

    @Test
    fun `marca como pendiente de sincronizar lo que todavia no llego al otro celular`() = runTest(planificador) {
        rutinas.rutinas.value = listOf(pancho, basura)
        pendientes.pendientes.value = listOf(abl)
        sync.situacion.value = sync.situacion.value.copy(idsSinEnviar = setOf("pancho", "abl"))

        viewModel().uiState.test {
            val estado = primerEstado() as HoyUiState.Contenido
            assertEquals(mapOf("pancho" to true, "basura" to false), estado.rutinas.associate { it.id to it.pendienteDeSync })
            assertTrue(estado.tareas.single().pendienteDeSync)
        }
    }

    /** Sincronización falsa: por defecto con conexión y sincronizada hace 5 minutos. */
    private class SyncFalso(ultima: Instant) : SincronizacionRepository {
        val situacion = MutableStateFlow(SituacionDeSincronizacion(ultima, conConexion = true, sincronizando = false, reintentando = false, idsSinEnviar = emptySet()))
        var pedidosDeSincronizar = 0
        override fun observar(): Flow<SituacionDeSincronizacion> = situacion
        override suspend fun sincronizarAhora() { pedidosDeSincronizar++ }
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
