package ar.edu.uade.miti.presentation.primeruso

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import ar.edu.uade.miti.domain.usecase.hogar.CrearHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.RecuperarMiHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.UnirseAHogarUseCase
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TuHogarViewModelTest {

    private class IngresoFalso : IngresoAHogarRepository {
        var recuperar: Resource<Boolean> = Resource.Success(false)
        var crear: Resource<Hogar> = Resource.Success(Hogar("h1", "Nuestro hogar", "K7P2QX"))
        var unirse: Resource<Hogar> = Resource.Success(Hogar("h1", "Nuestro hogar", "K7P2QX"))
        val pedidos = mutableListOf<String>()
        override fun observarSiHayHogar(): Flow<Boolean> = flowOf(false)
        override suspend fun recuperarMiHogar() = recuperar.also { pedidos += "recuperar" }
        override suspend fun crearHogar(nombreHogar: String) = crear.also { pedidos += "crear:$nombreHogar" }
        override suspend fun unirseAHogar(codigo: String) = unirse.also { pedidos += "unirse:$codigo" }
    }

    private val ingreso = IngresoFalso()
    private fun vm() = TuHogarViewModel(RecuperarMiHogarUseCase(ingreso), CrearHogarUseCase(ingreso), UnirseAHogarUseCase(ingreso))

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    @Test
    fun `si la persona ya tenia hogar lo recupera y termina sin pedirle nada`() {
        ingreso.recuperar = Resource.Success(true)
        assertEquals(TuHogarUiState.Listo, vm().uiState.value)
    }

    @Test
    fun `sin hogar ofrece crear o unirse`() {
        assertEquals(TuHogarUiState.Eligiendo(), vm().uiState.value)
    }

    @Test
    fun `si no pudo buscar por falta de conexion igual ofrece las opciones con un aviso`() {
        ingreso.recuperar = Resource.Error("Necesitás conexión", IOException())
        val estado = vm().uiState.value as TuHogarUiState.Eligiendo
        assertTrue(estado.aviso!!.sinConexion)
    }

    @Test
    fun `al crear muestra el codigo para compartir`() {
        val vm = vm()
        vm.crear("Nuestro hogar")
        assertEquals(TuHogarUiState.Creado("K7P2QX"), vm.uiState.value)
    }

    @Test
    fun `el codigo se escribe en mayusculas sin espacios y hasta 6 caracteres`() {
        val vm = vm()
        vm.cambiarCodigo("k7p 2qx99")
        assertEquals("K7P2QX", (vm.uiState.value as TuHogarUiState.Eligiendo).codigo)
    }

    @Test
    fun `un codigo rechazado por el servidor queda como error del campo`() {
        ingreso.unirse = Resource.Error("Código inválido")
        val vm = vm()
        vm.cambiarCodigo("K7P2QZ")

        vm.unirse()

        val estado = vm.uiState.value as TuHogarUiState.Eligiendo
        assertEquals("Código inválido", estado.errorCodigo)
        assertEquals(null, estado.enCurso)
    }

    @Test
    fun `sin conexion al unirse el aviso va arriba y no en el campo`() {
        ingreso.unirse = Resource.Error("Necesitás conexión", IOException())
        val vm = vm()
        vm.cambiarCodigo("K7P2QX")

        vm.unirse()

        val estado = vm.uiState.value as TuHogarUiState.Eligiendo
        assertEquals(null, estado.errorCodigo)
        assertTrue(estado.aviso!!.sinConexion)
    }

    @Test
    fun `al unirse termina`() {
        val vm = vm()
        vm.cambiarCodigo("K7P2QX")
        vm.unirse()
        assertEquals(TuHogarUiState.Listo, vm.uiState.value)
        assertEquals(listOf("recuperar", "unirse:K7P2QX"), ingreso.pedidos)
    }
}
