package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.CompletarTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.MarcarEnCarritoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarListaDeComprasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarProximasTareasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarCompraUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.TerminarCompraUseCase
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeUsoPendientesTest {

    private val hogares = FakeHogarRepository()
    private val pendientes = FakePendienteRepository()
    private val gastos = FakeGastoRepository()
    private val ids = IdsSecuenciales()
    private val reloj = relojEn(LocalDate.of(2026, 11, 5).atTime(18, 30))

    private val registrarTarea = RegistrarTareaUseCase(hogares, pendientes, ids)
    private val registrarCompra = RegistrarCompraUseCase(hogares, pendientes, ids)
    private val marcarEnCarrito = MarcarEnCarritoUseCase(pendientes)
    private val terminarCompra = TerminarCompraUseCase(pendientes, RegistrarGastoUseCase(hogares, gastos, reloj, ids))

    private val juevesALas18 = LocalDateTime.of(2026, 11, 5, 18, 0)

    @Test
    fun `las proximas tareas salen ordenadas por fecha y filtradas por responsable`() = runTest {
        val abl = registrarTarea("Pagar ABL", TOMAS, juevesALas18, avisar = true)
        val turno = registrarTarea("Sacar turno", ANA, juevesALas18.minusDays(1), avisar = false)
        val gas = registrarTarea("Pagar el gas", TOMAS, juevesALas18.minusHours(2), avisar = false)

        assertEquals(listOf(turno, gas, abl), ObservarProximasTareasUseCase(pendientes)().first())
        assertEquals(listOf(gas, abl), ObservarProximasTareasUseCase(pendientes)(TOMAS).first())
    }

    @Test
    fun `una tarea completada deja de figurar como proxima`() = runTest {
        val abl = registrarTarea("Pagar ABL", TOMAS, juevesALas18, avisar = true)

        CompletarTareaUseCase(pendientes)(abl.id)

        assertTrue(pendientes.obtenerPendiente(abl.id)!!.resuelto)
        assertEquals(emptyList<Any>(), ObservarProximasTareasUseCase(pendientes)().first())
    }

    @Test
    fun `tachar un item lo pasa al carrito`() = runTest {
        val leche = registrarCompra("Leche")
        val detergente = registrarCompra("Detergente")

        marcarEnCarrito(leche.id, enCarrito = true)

        val lista = ObservarListaDeComprasUseCase(pendientes)().first()
        assertEquals(listOf(detergente), lista.porComprar)
        assertEquals(listOf(leche.copy(enCarrito = true)), lista.enCarrito)
    }

    @Test
    fun `terminar la compra con gasto resuelve el carrito y vincula el gasto`() = runTest {
        val leche = registrarCompra("Leche")
        val detergente = registrarCompra("Detergente")
        val pan = registrarCompra("Pan")
        marcarEnCarrito(leche.id, true)
        marcarEnCarrito(detergente.id, true)

        val gasto = terminarCompra(DatosDeGasto("Supermercado", 2_400_000, pagadorId = TOMAS, reparto = Reparto.A_MEDIAS))

        assertEquals(listOf(gasto), gastos.gastos.value)
        val comprados = listOf(leche, detergente).map { pendientes.obtenerPendiente(it.id)!! }
        assertTrue(comprados.all { it.resuelto && !it.enCarrito && it.gastoId == gasto!!.id })
        // Lo que no estaba en el carrito sigue en la lista.
        assertEquals(listOf(pan), ObservarListaDeComprasUseCase(pendientes)().first().porComprar)
    }

    @Test
    fun `terminar la compra sin gasto la cierra igual`() = runTest {
        val leche = registrarCompra("Leche")
        marcarEnCarrito(leche.id, true)

        assertNull(terminarCompra(gasto = null))

        val cerrada = pendientes.obtenerPendiente(leche.id)!!
        assertTrue(cerrada.resuelto)
        assertNull(cerrada.gastoId)
        assertEquals(emptyList<Any>(), gastos.gastos.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se puede terminar una compra con el carrito vacio`() = runTest {
        registrarCompra("Leche")
        terminarCompra(gasto = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `una compra no se completa como tarea`() = runTest {
        val leche = registrarCompra("Leche")
        CompletarTareaUseCase(pendientes)(leche.id)
    }
}
