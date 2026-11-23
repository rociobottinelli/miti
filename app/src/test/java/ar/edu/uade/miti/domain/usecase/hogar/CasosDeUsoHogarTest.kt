package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeUsoHogarTest {

    /** Anota qué le llegó; responde siempre que salió bien. */
    private class FakeIngreso : IngresoAHogarRepository {
        val pedidos = mutableListOf<String>()
        override suspend fun crearHogar(nombreHogar: String): Resource<Hogar> {
            pedidos += "crear:$nombreHogar"
            return Resource.Success(Hogar("h1", nombreHogar, "K7P2QX"))
        }
        override suspend fun unirseAHogar(codigo: String): Resource<Hogar> {
            pedidos += "unirse:$codigo"
            return Resource.Success(Hogar("h1", "Casa", codigo))
        }
        override suspend fun recuperarMiHogar(): Resource<Boolean> = Resource.Success(true)
    }

    private val ingreso = FakeIngreso()

    @Test
    fun `crear un hogar le saca los espacios al nombre`() = runTest {
        CrearHogarUseCase(ingreso)("  Nuestra casa ")
        assertEquals(listOf("crear:Nuestra casa"), ingreso.pedidos)
    }

    @Test
    fun `crear un hogar sin nombre no llama al servidor`() = runTest {
        assertTrue(CrearHogarUseCase(ingreso)("  ") is Resource.Error)
        assertTrue(ingreso.pedidos.isEmpty())
    }

    @Test
    fun `el codigo se acepta en minusculas y con espacios`() = runTest {
        UnirseAHogarUseCase(ingreso)(" k7p 2qx ")
        assertEquals(listOf("unirse:K7P2QX"), ingreso.pedidos)
    }

    @Test
    fun `un codigo con otra forma se rechaza sin llamar al servidor`() = runTest {
        val unirse = UnirseAHogarUseCase(ingreso)
        for (codigo in listOf("", "K7P2Q", "K7P2QX9", "K7P-2Q")) {
            assertTrue("\"$codigo\" debería rechazarse", unirse(codigo) is Resource.Error)
        }
        assertTrue(ingreso.pedidos.isEmpty())
    }
}
