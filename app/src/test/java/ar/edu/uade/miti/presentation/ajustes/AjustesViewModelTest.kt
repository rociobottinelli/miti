package ar.edu.uade.miti.presentation.ajustes

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import ar.edu.uade.miti.domain.usecase.hogar.ObservarHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.sesion.CerrarSesionUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {

    /** Tomás usa el celular. */
    private class SesionFalsa : SesionRepository {
        val usuario = MutableStateFlow<Usuario?>(Usuario(TOMAS, "tomas@mail.com"))
        var falla: String? = null
        override fun observarUsuario(): Flow<Usuario?> = usuario
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = error("no se usa")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = error("no se usa")
        override suspend fun cerrarSesion(): Resource<Unit> {
            falla?.let { return Resource.Error(it) }
            usuario.value = null
            return Resource.Success(Unit)
        }
    }

    private val sesion = SesionFalsa()
    private val hogar = FakeHogarRepository()
    private fun vm() = AjustesViewModel(
        ObservarHogarUseCase(hogar), ObservarIntegrantesUseCase(hogar), ObservarUsuarioUseCase(sesion), CerrarSesionUseCase(sesion),
    )

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    private suspend fun ReceiveTurbine<AjustesUiState>.contenido(): AjustesUiState.Contenido {
        var estado = awaitItem()
        while (estado == AjustesUiState.Cargando) estado = awaitItem()
        return estado as AjustesUiState.Contenido
    }

    @Test
    fun `muestra primero a quien usa el celular, con los colores de siempre, y el codigo`() = runTest {
        vm().uiState.test {
            val estado = contenido()
            assertEquals(listOf(TOMAS, ANA), estado.integrantes.map { it.id })
            assertEquals(listOf(true, false), estado.integrantes.map { it.esYo })
            // El color sigue la posición por nombre (Ana 0, Tomás 1), aunque el orden en pantalla cambie.
            assertEquals(listOf(1, 0), estado.integrantes.map { it.posicion })
            assertEquals("ABC123", estado.codigo)
        }
    }

    @Test
    fun `cerrar sesion pide confirmar y cancelar no cierra`() = runTest {
        val vm = vm()
        vm.uiState.test {
            contenido()
            vm.pedirCerrarSesion()
            assertEquals(CierreDeSesion.Confirmando, (expectMostRecentItem() as AjustesUiState.Contenido).cierre)

            vm.cancelarCierre()

            assertEquals(CierreDeSesion.Nada, (expectMostRecentItem() as AjustesUiState.Contenido).cierre)
            assertEquals(TOMAS, sesion.usuario.value?.id)
        }
    }

    @Test
    fun `al confirmar cierra la sesion`() = runTest {
        val vm = vm()
        vm.uiState.test {
            contenido()
            vm.pedirCerrarSesion()
            vm.confirmarCierre()
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(sesion.usuario.value)
    }

    @Test
    fun `si no se pudo cerrar la sesion lo avisa`() = runTest {
        sesion.falla = "No se pudieron borrar los datos del celular"
        val vm = vm()
        vm.uiState.test {
            contenido()
            vm.pedirCerrarSesion()
            vm.confirmarCierre()

            assertEquals(
                CierreDeSesion.NoSePudo("No se pudieron borrar los datos del celular"),
                (expectMostRecentItem() as AjustesUiState.Contenido).cierre,
            )
        }
    }
}
