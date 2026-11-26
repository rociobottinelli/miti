package ar.edu.uade.miti.domain.usecase.primeruso

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import ar.edu.uade.miti.domain.repository.OnboardingRepository
import ar.edu.uade.miti.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservarInicioUseCaseTest {

    private val usuario = MutableStateFlow<Usuario?>(null)
    private val hayHogar = MutableStateFlow(false)
    private val vioOnboarding = MutableStateFlow(false)

    private val sesion = object : SesionRepository {
        override fun observarUsuario(): Flow<Usuario?> = usuario
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = error("no se usa")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = error("no se usa")
        override suspend fun cerrarSesion() = error("no se usa")
    }
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
    private val inicio = ObservarInicioUseCase(sesion, ingreso, onboarding)

    @Test
    fun `la primera vez arranca en el onboarding`() = runTest {
        assertEquals(Inicio.ONBOARDING, inicio().first())
    }

    @Test
    fun `con el onboarding visto y sin sesion pide la cuenta`() = runTest {
        vioOnboarding.value = true
        assertEquals(Inicio.CUENTA, inicio().first())
    }

    @Test
    fun `con sesion y sin hogar pide el hogar aunque no haya visto el onboarding`() = runTest {
        usuario.value = Usuario("u", "ana@mail.com")
        assertEquals(Inicio.HOGAR, inicio().first())
    }

    @Test
    fun `con sesion y hogar va directo a Hoy`() = runTest {
        usuario.value = Usuario("u", "ana@mail.com")
        hayHogar.value = true
        assertEquals(Inicio.LISTO, inicio().first())
    }

    @Test
    fun `al cerrar sesion vuelve a pedir la cuenta y no el onboarding`() = runTest {
        vioOnboarding.value = true
        usuario.value = Usuario("u", "ana@mail.com")
        hayHogar.value = true
        assertEquals(Inicio.LISTO, inicio().first())

        usuario.value = null
        hayHogar.value = false

        assertEquals(Inicio.CUENTA, inicio().first())
    }
}
