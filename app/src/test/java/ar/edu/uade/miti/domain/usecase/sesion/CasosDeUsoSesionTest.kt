package ar.edu.uade.miti.domain.usecase.sesion

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeUsoSesionTest {

    private class FakeSesionRepository : SesionRepository {
        val llamadas = mutableListOf<String>()
        val usuario = MutableStateFlow<Usuario?>(null)
        override fun observarUsuario(): Flow<Usuario?> = usuario
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String): Resource<Usuario> {
            llamadas += "crearCuenta($nombre, $email)"
            return Resource.Success(Usuario("u1", email))
        }
        override suspend fun iniciarSesion(email: String, contrasenia: String): Resource<Usuario> {
            llamadas += "iniciarSesion($email)"
            return Resource.Success(Usuario("u1", email))
        }
        override suspend fun cerrarSesion(): Resource<Unit> {
            llamadas += "cerrarSesion"
            return Resource.Success(Unit)
        }
    }

    private val repositorio = FakeSesionRepository()
    private val crearCuenta = CrearCuentaUseCase(repositorio)
    private val iniciarSesion = IniciarSesionUseCase(repositorio)

    @Test
    fun `crear cuenta limpia el nombre y pasa el email a minusculas`() = runTest {
        crearCuenta("  Ana ", " Ana@Mail.com ", "secreta1")

        assertEquals(listOf("crearCuenta(Ana, ana@mail.com)"), repositorio.llamadas)
    }

    @Test
    fun `un email invalido nunca llega al servidor`() = runTest {
        val resultado = crearCuenta("Ana", "ana.mail.com", "secreta1")

        assertEquals(Resource.Error("El email no tiene un formato válido"), resultado)
        assertTrue(repositorio.llamadas.isEmpty())
    }

    @Test
    fun `una contrasenia corta nunca llega al servidor`() = runTest {
        val resultado = crearCuenta("Ana", "ana@mail.com", "123")

        assertEquals(Resource.Error("La contraseña tiene que tener al menos 6 caracteres"), resultado)
        assertTrue(repositorio.llamadas.isEmpty())
    }

    @Test
    fun `sin nombre no se crea la cuenta`() = runTest {
        assertEquals(Resource.Error("Ingresá tu nombre"), crearCuenta("  ", "ana@mail.com", "secreta1"))
        assertTrue(repositorio.llamadas.isEmpty())
    }

    @Test
    fun `ingresar exige email y contrasenia pero no valida el largo`() = runTest {
        assertEquals(Resource.Error("Ingresá tu contraseña"), iniciarSesion("ana@mail.com", ""))
        assertTrue(repositorio.llamadas.isEmpty())

        iniciarSesion("ANA@mail.com", "123")
        assertEquals(listOf("iniciarSesion(ana@mail.com)"), repositorio.llamadas)
    }
}
