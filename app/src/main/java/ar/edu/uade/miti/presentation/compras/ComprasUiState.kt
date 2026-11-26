package ar.edu.uade.miti.presentation.compras

import ar.edu.uade.miti.presentation.components.AvisoDeSync
import java.time.LocalDate

sealed interface ComprasUiState {
    data object Cargando : ComprasUiState

    /** No se pudo leer la lista del celular (Figma "Compras - error"). */
    data object Error : ComprasUiState

    /**
     * La lista partida en lo que falta y lo que ya está en el carrito. Si las dos partes están vacías se
     * muestra "No falta nada", pero el campo para agregar sigue abajo, por eso no es otro estado.
     */
    data class Contenido(
        val porComprar: List<ItemDeCompra>,
        val enCarrito: List<ItemDeCompra>,
        /** Lo que se está escribiendo en "Agregar ítem". */
        val nuevoItem: String = "",
        val aviso: AvisoDeSync? = null,
        /** Para mostrar la hora de la última sincronización ("14:32" o "20/11 14:32"). */
        val hoy: LocalDate,
        val terminando: Boolean = false,
    ) : ComprasUiState {
        val vacia: Boolean get() = porComprar.isEmpty() && enCarrito.isEmpty()
        val total: Int get() = porComprar.size + enCarrito.size
        val sePuedeAgregar: Boolean get() = nuevoItem.isNotBlank()
    }
}

data class ItemDeCompra(
    val id: String,
    val titulo: String,
    /** Cambio guardado en el celular que todavía no llegó al otro (RNF09). */
    val pendienteDeSync: Boolean = false,
)

sealed interface EventoDeCompras {
    data object CompraTerminada : EventoDeCompras

    /** Se borró un ítem: el snackbar ofrece deshacerlo. */
    data class Borrado(val titulo: String) : EventoDeCompras

    /** El mensaje viene del caso de uso ("Ya está en la lista", "No hay nada en el carrito"…). */
    data class NoSePudo(val mensaje: String) : EventoDeCompras
}
