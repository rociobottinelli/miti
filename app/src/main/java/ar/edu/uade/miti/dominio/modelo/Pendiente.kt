package ar.edu.uade.miti.dominio.modelo

import java.time.LocalDateTime

enum class TipoPendiente { TAREA, COMPRA }

/** Tarea programada o ítem de la lista de compras. */
data class Pendiente(
    val id: String,
    val hogarId: String,
    val tipo: TipoPendiente,
    val titulo: String,
    /** Solo para tareas. */
    val responsableId: String? = null,
    /** Solo para tareas. */
    val fechaHora: LocalDateTime? = null,
    val avisar: Boolean = false,
    /** Solo para compras: el ítem ya está en el carrito, pero la compra no terminó. */
    val enCarrito: Boolean = false,
    val resuelto: Boolean = false,
    /** Gasto generado al terminar la compra, si se registró. */
    val gastoId: String? = null,
) {
    init {
        require(titulo.isNotBlank()) { "El título no puede estar vacío" }
        when (tipo) {
            TipoPendiente.TAREA -> {
                require(responsableId != null) { "Una tarea necesita un responsable" }
                require(fechaHora != null) { "Una tarea necesita fecha y hora" }
                require(!enCarrito && gastoId == null) { "Una tarea no tiene carrito ni gasto" }
            }
            TipoPendiente.COMPRA -> {
                require(responsableId == null && fechaHora == null && !avisar) {
                    "Una compra no tiene responsable, fecha ni aviso"
                }
            }
        }
    }
}
