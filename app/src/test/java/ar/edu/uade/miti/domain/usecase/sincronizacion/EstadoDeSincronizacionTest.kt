package ar.edu.uade.miti.domain.usecase.sincronizacion

import ar.edu.uade.miti.domain.model.DatosDesactualizados
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import ar.edu.uade.miti.domain.repository.SituacionDeSincronizacion
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoDeSincronizacionTest {

    private val ahora = Instant.parse("2026-11-23T12:00:00Z")
    private val reloj = Clock.fixed(ahora, ZoneOffset.UTC)

    private class FakeSincronizacion(inicial: SituacionDeSincronizacion) : SincronizacionRepository {
        val situacion = MutableStateFlow(inicial)
        var pedidos = 0
        override fun observar() = situacion
        override suspend fun sincronizarAhora() { pedidos++ }
    }

    private fun situacion(ultima: Instant?, ids: Set<String> = emptySet()) = SituacionDeSincronizacion(
        ultimaSincronizacion = ultima, conConexion = false, sincronizando = false, reintentando = true, idsSinEnviar = ids,
    )

    @Test
    fun `nunca sincronizado es desactualizado`() {
        assertTrue(DatosDesactualizados.segun(null, ahora))
    }

    @Test
    fun `el umbral es de 30 minutos`() {
        assertFalse(DatosDesactualizados.segun(ahora.minus(Duration.ofMinutes(30)), ahora))
        assertTrue(DatosDesactualizados.segun(ahora.minus(Duration.ofMinutes(31)), ahora))
    }

    @Test
    fun `arma el estado con la cantidad de cambios sin enviar`() = runTest {
        val repo = FakeSincronizacion(situacion(ahora.minus(Duration.ofHours(2)), setOf("r1", "g1")))

        val estado = ObservarEstadoDeSincronizacionUseCase(repo, reloj)().first()

        assertEquals(2, estado.cambiosSinEnviar)
        assertTrue(estado.desactualizado)
        assertFalse(estado.conConexion)
        assertTrue(estado.reintentando)
    }

    @Test
    fun `al sincronizar se apaga el aviso de desactualizado`() = runTest {
        val repo = FakeSincronizacion(situacion(null, setOf("r1")))
        val caso = ObservarEstadoDeSincronizacionUseCase(repo, reloj)

        assertTrue(caso().first().desactualizado)
        repo.situacion.value = situacion(ahora.minusSeconds(5))

        val estado = caso().first()
        assertFalse(estado.desactualizado)
        assertEquals(0, estado.cambiosSinEnviar)
    }

    @Test
    fun `los ids sin enviar solo emiten cuando cambian`() = runTest {
        val repo = FakeSincronizacion(situacion(ahora, setOf("r1")))
        val caso = ObservarItemsSinEnviarUseCase(repo)

        assertEquals(setOf("r1"), caso().first())
        // Cambia la conexión pero no los ids: no hay una emisión nueva con el mismo conjunto.
        repo.situacion.value = repo.situacion.value.copy(conConexion = true)
        repo.situacion.value = repo.situacion.value.copy(idsSinEnviar = emptySet())
        assertEquals(listOf(emptySet<String>()), caso().take(1).toList())
    }

    @Test
    fun `reintentar pide una sincronizacion`() = runTest {
        val repo = FakeSincronizacion(situacion(ahora))
        SincronizarAhoraUseCase(repo)()
        assertEquals(1, repo.pedidos)
    }
}
