package ar.edu.uade.miti.presentation.primeruso

import ar.edu.uade.miti.domain.usecase.sesion.IniciarSesionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IngresarViewModelTest {

    private val sesion = SesionFalsa()
    private val vm by lazy { IngresarViewModel(IniciarSesionUseCase(sesion)) }

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    @Test
    fun `sin email ni contrasenia marca los dos campos y no llama al servidor`() {
        vm.ingresar()

        assertNotNull(vm.uiState.value.errorEmail)
        assertNotNull(vm.uiState.value.errorContrasenia)
        assertTrue(sesion.pedidos.isEmpty())
    }

    @Test
    fun `al ingresar no exige el largo minimo de una contrasenia nueva`() {
        vm.cambiarEmail("ana@mail.com")
        vm.cambiarContrasenia("123")

        vm.ingresar()

        assertNull(vm.uiState.value.errorContrasenia)
        assertEquals(listOf("ingresar:ana@mail.com"), sesion.pedidos)
    }

    @Test
    fun `datos incorrectos llegan como rechazo y no como falta de conexion`() {
        sesion.errorDelServidor = "Email o contraseña incorrectos"
        vm.cambiarEmail("ana@mail.com")
        vm.cambiarContrasenia("otra")

        vm.ingresar()

        assertEquals(EnvioDeCuenta.Fallo("Email o contraseña incorrectos", sinConexion = false), vm.uiState.value.envio)
    }

    @Test
    fun `sin conexion se marca como tal y al volver a escribir desaparece el aviso`() {
        sesion.fallarSinConexion = true
        vm.cambiarEmail("ana@mail.com")
        vm.cambiarContrasenia("secreto")
        vm.ingresar()
        assertTrue((vm.uiState.value.envio as EnvioDeCuenta.Fallo).sinConexion)

        vm.cambiarContrasenia("secreto2")

        assertEquals(EnvioDeCuenta.Editando, vm.uiState.value.envio)
    }
}
