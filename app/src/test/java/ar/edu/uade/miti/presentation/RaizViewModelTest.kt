package ar.edu.uade.miti.presentation

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import ar.edu.uade.miti.domain.repository.OnboardingRepository
import ar.edu.uade.miti.domain.usecase.primeruso.MarcarOnboardingVistoUseCase
import ar.edu.uade.miti.domain.usecase.primeruso.ObservarInicioUseCase
import ar.edu.uade.miti.presentation.primeruso.SesionFalsa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RaizViewModelTest {

    private val sesion = SesionFalsa()
    private val hayHogar = MutableStateFlow(false)
    private val vioOnboarding = MutableStateFlow(false)

    private val ingreso = object : IngresoAHogarRepository {
        override fun observarSiHayHogar(): Flow<Boolean> = hayHogar
        override suspend fun crearHogar(nombreHogar: String): Resource<Hogar> = error("no se usa")
        override suspend fun unirseAHogar(codigo: String): Resource<Hogar> = error("no se usa")
        override suspend fun recuperarMiHogar(): Resource<Boolean> = error("no se usa")
    }
    private val onboarding = object : OnboardingRepository {
        override fun observarVisto(): Flow<Boolean> = vioOnboarding
        override suspend fun marcarVisto() { vioOnboarding.value = true }
    }

    private fun raiz() = RaizViewModel(ObservarInicioUseCase(sesion, ingreso, onboarding), MarcarOnboardingVistoUseCase(onboarding))

    @Before
    fun despachador() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun restaurar() = Dispatchers.resetMain()

    private fun entrar() {
        sesion.usuario.value = Usuario("u-ana", "ana@mail.com")
    }

    @Test
    fun `flujo 1 completo creando el hogar`() {
        val raiz = raiz()
        assertEquals(DestinoRaiz.Onboarding, raiz.destino.value)

        raiz.terminarOnboarding()
        assertEquals(DestinoRaiz.Cuenta(empezarCreando = true), raiz.destino.value)

        entrar()
        assertEquals(DestinoRaiz.TuHogar, raiz.destino.value)

        // Al crear, el hogar ya existe, pero se sigue mostrando el código hasta que toque Continuar.
        raiz.empezarACrearHogar()
        hayHogar.value = true
        assertEquals(DestinoRaiz.TuHogar, raiz.destino.value)

        raiz.terminarTuHogar()
        assertEquals(DestinoRaiz.Permiso, raiz.destino.value)

        raiz.resolverPermiso()
        assertEquals(DestinoRaiz.App, raiz.destino.value)
    }

    @Test
    fun `al unirse pasa directo al permiso`() {
        vioOnboarding.value = true
        val raiz = raiz()
        entrar()

        hayHogar.value = true

        assertEquals(DestinoRaiz.Permiso, raiz.destino.value)
    }

    @Test
    fun `con sesion y hogar abre la app sin pedir el permiso de nuevo`() {
        entrar()
        hayHogar.value = true

        assertEquals(DestinoRaiz.App, raiz().destino.value)
    }

    @Test
    fun `al cerrar sesion vuelve a Ingresar y no a Crear cuenta`() {
        val raiz = raiz()
        raiz.terminarOnboarding()
        entrar()
        hayHogar.value = true
        raiz.terminarTuHogar()
        raiz.resolverPermiso()
        assertEquals(DestinoRaiz.App, raiz.destino.value)

        sesion.usuario.value = null
        hayHogar.value = false

        assertEquals(DestinoRaiz.Cuenta(empezarCreando = false), raiz.destino.value)
    }
}
