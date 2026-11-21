package ar.edu.uade.miti.dominio.casouso.pendientes

import ar.edu.uade.miti.dominio.casouso.gastos.DatosDeGasto
import ar.edu.uade.miti.dominio.casouso.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.TipoPendiente
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepository
import ar.edu.uade.miti.dominio.repositorio.PendienteRepository
import ar.edu.uade.miti.dominio.repositorio.requerirHogarActivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/** La lista del supermercado partida en lo que falta y lo que ya está en el carrito. */
data class ListaDeCompras(val porComprar: List<Pendiente>, val enCarrito: List<Pendiente>)

class RegistrarTareaUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String, responsableId: String, fechaHora: LocalDateTime, avisar: Boolean): Pendiente {
        val tarea = Pendiente(
            id = ids.nuevoId(),
            hogarId = hogares.requerirHogarActivo(),
            tipo = TipoPendiente.TAREA,
            titulo = titulo.trim(),
            responsableId = responsableId,
            fechaHora = fechaHora,
            avisar = avisar,
        )
        pendientes.guardarPendientes(listOf(tarea))
        return tarea
    }
}

class RegistrarCompraUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String): Pendiente {
        val compra = Pendiente(ids.nuevoId(), hogares.requerirHogarActivo(), TipoPendiente.COMPRA, titulo.trim())
        pendientes.guardarPendientes(listOf(compra))
        return compra
    }
}

class CompletarTareaUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(tareaId: String) {
        val tarea = requireNotNull(pendientes.obtenerPendiente(tareaId)) { "No existe la tarea $tareaId" }
        require(tarea.tipo == TipoPendiente.TAREA) { "$tareaId no es una tarea" }
        if (!tarea.resuelto) pendientes.guardarPendientes(listOf(tarea.copy(resuelto = true)))
    }
}

/** Pone o saca un ítem del carrito (tachar en el supermercado). */
class MarcarEnCarritoUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(compraId: String, enCarrito: Boolean) {
        val compra = requireNotNull(pendientes.obtenerPendiente(compraId)) { "No existe la compra $compraId" }
        require(compra.tipo == TipoPendiente.COMPRA) { "$compraId no es una compra" }
        pendientes.guardarPendientes(listOf(compra.copy(enCarrito = enCarrito)))
    }
}

/**
 * Cierra la compra: resuelve todo lo que está en el carrito. Si el usuario aceptó registrar
 * el gasto, lo crea y lo vincula a cada ítem comprado (flujo 3); si no, la compra queda cerrada
 * sin gasto. Devuelve el gasto creado, si hubo.
 */
class TerminarCompraUseCase @Inject constructor(
    private val pendientes: PendienteRepository,
    private val registrarGasto: RegistrarGastoUseCase,
) {
    suspend operator fun invoke(gasto: DatosDeGasto?): Gasto? {
        val enCarrito = pendientes.observarPendientes().first()
            .filter { it.tipo == TipoPendiente.COMPRA && it.enCarrito && !it.resuelto }
        require(enCarrito.isNotEmpty()) { "No hay nada en el carrito" }

        // Primero el gasto, para que los ítems ya puedan apuntar a su id.
        val gastoCreado = gasto?.let { registrarGasto(it) }
        pendientes.guardarPendientes(
            enCarrito.map { it.copy(enCarrito = false, resuelto = true, gastoId = gastoCreado?.id) },
        )
        return gastoCreado
    }
}

/** Tareas sin resolver ordenadas por fecha; con [responsableId], solo las de esa persona. */
class ObservarProximasTareasUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    operator fun invoke(responsableId: String? = null): Flow<List<Pendiente>> =
        pendientes.observarPendientes().map { todos ->
            todos
                .filter { it.tipo == TipoPendiente.TAREA && !it.resuelto }
                .filter { responsableId == null || it.responsableId == responsableId }
                .sortedBy { it.fechaHora }
        }
}

class ObservarListaDeComprasUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    operator fun invoke(): Flow<ListaDeCompras> =
        pendientes.observarPendientes().map { todos ->
            val abiertas = todos.filter { it.tipo == TipoPendiente.COMPRA && !it.resuelto }.sortedBy { it.titulo }
            val (enCarrito, porComprar) = abiertas.partition { it.enCarrito }
            ListaDeCompras(porComprar, enCarrito)
        }
}
