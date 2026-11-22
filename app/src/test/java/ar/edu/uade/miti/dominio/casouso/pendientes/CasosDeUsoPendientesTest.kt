package ar.edu.uade.miti.dominio.casouso.pendientes

import ar.edu.uade.miti.dominio.casouso.gastos.DatosDeGasto
import ar.edu.uade.miti.dominio.casouso.gastos.RegistrarGasto
import ar.edu.uade.miti.dominio.fakes.ANA
import ar.edu.uade.miti.dominio.fakes.FakeGastoRepositorio
import ar.edu.uade.miti.dominio.fakes.FakeHogarRepositorio
import ar.edu.uade.miti.dominio.fakes.FakePendienteRepositorio
import ar.edu.uade.miti.dominio.fakes.IdsSecuenciales
import ar.edu.uade.miti.dominio.fakes.TOMAS
import ar.edu.uade.miti.dominio.fakes.relojEn
import ar.edu.uade.miti.dominio.modelo.Reparto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CasosDeUsoPendientesTest {

    private val hogares = FakeHogarRepositorio()
    private val pendientes = FakePendienteRepositorio()
    private val gastos = FakeGastoRepositorio()
    private val ids = IdsSecuenciales()
    private val reloj = relojEn(LocalDate.of(2026, 11, 5).atTime(18, 30))

    private val registrarTarea = RegistrarTarea(hogares, pendientes, ids)
    private val registrarCompra = RegistrarCompra(hogares, pendientes, ids)
    private val marcarEnCarrito = MarcarEnCarrito(pendientes)
    private val terminarCompra = TerminarCompra(pendientes, RegistrarGasto(hogares, gastos, reloj, ids))

    private val juevesALas18 = LocalDateTime.of(2026, 11, 5, 18, 0)

    @Test
    fun `las proximas tareas salen ordenadas por fecha y filtradas por responsable`() = runTest {
        val abl = registrarTarea("Pagar ABL", TOMAS, juevesALas18, avisar = true)
        val turno = registrarTarea("Sacar turno", ANA, juevesALas18.minusDays(1), avisar = false)
        val gas = registrarTarea("Pagar el gas", TOMAS, juevesALas18.minusHours(2), avisar = false)

        assertEquals(listOf(turno, gas, abl), ObservarProximasTareas(pendientes)().first())
        assertEquals(listOf(gas, abl), ObservarProximasTareas(pendientes)(TOMAS).first())
    }

    @Test
    fun `una tarea completada deja de figurar como proxima`() = runTest {
        val abl = registrarTarea("Pagar ABL", TOMAS, juevesALas18, avisar = true)

        CompletarTarea(pendientes)(abl.id)

        assertTrue(pendientes.obtenerPendiente(abl.id)!!.resuelto)
        assertEquals(emptyList<Any>(), ObservarProximasTareas(pendientes)().first())
    }

    @Test
    fun `tachar un item lo pasa al carrito`() = runTest {
        val leche = registrarCompra("Leche")
        val detergente = registrarCompra("Detergente")

        marcarEnCarrito(leche.id, enCarrito = true)

        val lista = ObservarListaDeCompras(pendientes)().first()
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
        assertEquals(listOf(pan), ObservarListaDeCompras(pendientes)().first().porComprar)
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
        CompletarTarea(pendientes)(leche.id)
    }
}
