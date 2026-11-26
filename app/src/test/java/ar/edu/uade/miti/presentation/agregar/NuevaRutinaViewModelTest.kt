package ar.edu.uade.miti.presentation.agregar

import app.cash.turbine.test
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakeRutinaRepository
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.usecase.rutinas.RegistrarRutinaUseCase
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NuevaRutinaViewModelTest {

    private val rutinas = FakeRutinaRepository()
    private val vm by lazy { NuevaRutinaViewModel(RegistrarRutinaUseCase(FakeHogarRepository(), rutinas, IdsSecuenciales())) }

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    @Test
    fun `sin nombre no se puede guardar`() {
        assertFalse(vm.uiState.value.puedeGuardar)
        vm.cambiarNombre("Regar las plantas")
        assertTrue(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `algunos dias sin elegir ninguno marca el error y no guarda`() {
        vm.cambiarNombre("Sacar la basura")
        vm.elegirFrecuencia(Frecuencia.DIAS_DE_LA_SEMANA)

        vm.guardar()

        assertEquals(ErrorDeFormulario.SIN_DIAS, vm.uiState.value.errorDias)
        assertTrue(rutinas.rutinas.value.isEmpty())
    }

    @Test
    fun `guarda la rutina al instante, avisa y deja el formulario vacio`() = runTest {
        vm.guardada.test {
            vm.cambiarNombre("Sacar la basura")
            vm.elegirFrecuencia(Frecuencia.DIAS_DE_LA_SEMANA)
            vm.alternarDia(DayOfWeek.MONDAY)
            vm.alternarDia(DayOfWeek.THURSDAY)
            vm.elegirVeces(2)

            vm.guardar()

            assertEquals("Sacar la basura", awaitItem())
        }
        val guardada = rutinas.rutinas.value.single()
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), guardada.dias)
        assertEquals(2, guardada.vecesPorDia)
        assertEquals(NuevaRutinaUiState(), vm.uiState.value)
    }

    @Test
    fun `al volver a todos los dias no guarda los dias elegidos antes`() {
        vm.cambiarNombre("Cambiar el agua")
        vm.elegirFrecuencia(Frecuencia.DIAS_DE_LA_SEMANA)
        vm.alternarDia(DayOfWeek.MONDAY)
        vm.elegirFrecuencia(Frecuencia.DIARIA)

        vm.guardar()

        assertTrue(rutinas.rutinas.value.single().dias.isEmpty())
    }
}
