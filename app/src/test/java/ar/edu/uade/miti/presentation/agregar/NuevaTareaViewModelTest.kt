package ar.edu.uade.miti.presentation.agregar

import app.cash.turbine.test
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarTareaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import ar.edu.uade.miti.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NuevaTareaViewModelTest {

    /** Miércoles 25/11/2026, 9:40. */
    private val ahora = LocalDateTime.of(2026, 11, 25, 9, 40)
    private val reloj = Clock.fixed(ahora.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val pendientes = FakePendienteRepository()
    /** Tomás usa el celular. */
    private val sesion = object : SesionRepository {
        override fun observarUsuario(): Flow<Usuario?> = flowOf(Usuario(TOMAS, "tomas@mail.com"))
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = error("no se usa")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = error("no se usa")
        override suspend fun cerrarSesion() = error("no se usa")
    }
    private val hogar = FakeHogarRepository()

    private val vm by lazy {
        NuevaTareaViewModel(
            RegistrarTareaUseCase(hogar, pendientes, IdsSecuenciales()),
            ObservarIntegrantesUseCase(hogar),
            ObservarUsuarioUseCase(sesion),
            reloj,
        )
    }

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    @Test
    fun `arranca hoy a la proxima hora en punto y con quien usa el celular primero`() {
        val estado = vm.uiState.value
        assertEquals(LocalDate.of(2026, 11, 25), estado.fecha)
        assertEquals(LocalTime.of(10, 0), estado.hora)
        assertEquals(listOf(TOMAS, ANA), estado.responsables.map { it.id })
        assertEquals(TOMAS, estado.responsableId)
        assertTrue(estado.avisar)
    }

    @Test
    fun `una fecha que ya paso se marca y no se guarda`() {
        vm.cambiarNombre("Pagar ABL")
        vm.elegirHora(LocalTime.of(9, 0))

        vm.guardar()

        assertEquals(ErrorDeFormulario.FECHA_PASADA, vm.uiState.value.errorFecha)
        assertTrue(pendientes.pendientes.value.isEmpty())
    }

    @Test
    fun `guarda la tarea con responsable, fecha, hora y aviso`() = runTest {
        vm.guardada.test {
            vm.cambiarNombre("Pagar ABL")
            vm.elegirResponsable(ANA)
            vm.elegirFecha(LocalDate.of(2026, 11, 26))
            vm.elegirHora(LocalTime.of(18, 0))
            vm.cambiarAviso(false)

            vm.guardar()

            assertEquals("Pagar ABL", awaitItem())
        }
        val tarea = pendientes.pendientes.value.single()
        assertEquals(ANA, tarea.responsableId)
        assertEquals(LocalDateTime.of(2026, 11, 26, 18, 0), tarea.fechaHora)
        assertEquals(false, tarea.avisar)
        // Queda vacío para la próxima, con quien usa el celular otra vez como responsable.
        assertEquals("", vm.uiState.value.nombre)
        assertEquals(TOMAS, vm.uiState.value.responsableId)
    }
}
