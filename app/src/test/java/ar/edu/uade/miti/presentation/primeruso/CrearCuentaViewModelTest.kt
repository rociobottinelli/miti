package ar.edu.uade.miti.presentation.primeruso

import ar.edu.uade.miti.domain.usecase.sesion.CrearCuentaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrearCuentaViewModelTest {

    private val sesion = SesionFalsa()
    private val vm by lazy { CrearCuentaViewModel(CrearCuentaUseCase(sesion)) }

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    private fun completar(nombre: String = "Ana", email: String = "ana@mail.com", contrasenia: String = "secreto") {
        vm.cambiarNombre(nombre)
        vm.cambiarEmail(email)
        vm.cambiarContrasenia(contrasenia)
    }

    @Test
    fun `con campos invalidos marca cada error y no llama al servidor`() {
        completar(nombre = " ", email = "ana@", contrasenia = "123")

        vm.crear()

        val estado = vm.uiState.value
        assertNotNull(estado.errorNombre)
        assertNotNull(estado.errorEmail)
        assertNotNull(estado.errorContrasenia)
        assertTrue(sesion.pedidos.isEmpty())
    }

    @Test
    fun `al corregir un campo se borra su error`() {
        completar(nombre = "")
        vm.crear()

        vm.cambiarNombre("Ana")

        assertEquals(null, vm.uiState.value.errorNombre)
    }

    @Test
    fun `si sale bien queda enviando hasta que la app pase a la pantalla siguiente`() {
        completar()

        vm.crear()

        assertEquals(listOf("crear:Ana:ana@mail.com"), sesion.pedidos)
        assertEquals(EnvioDeCuenta.Enviando, vm.uiState.value.envio)
    }

    @Test
    fun `sin conexion lo distingue de un error del servidor`() {
        completar()
        sesion.fallarSinConexion = true

        vm.crear()

        val fallo = vm.uiState.value.envio as EnvioDeCuenta.Fallo
        assertTrue(fallo.sinConexion)
    }

    @Test
    fun `un error del servidor se muestra con su mensaje y no borra lo escrito`() {
        completar()
        sesion.errorDelServidor = "Ya existe una cuenta con ese email. Probá ingresar."

        vm.crear()

        val estado = vm.uiState.value
        assertEquals(EnvioDeCuenta.Fallo("Ya existe una cuenta con ese email. Probá ingresar.", false), estado.envio)
        assertEquals("ana@mail.com", estado.email)
    }
}
