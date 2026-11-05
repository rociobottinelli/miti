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
    val resuelto: Boolean = false,
    /** Gasto generado al terminar la compra, si se registró. */
    val gastoId: String? = null,
)
