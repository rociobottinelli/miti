package ar.edu.uade.miti.domain.model

/** La lista del supermercado partida en lo que falta y lo que ya está en el carrito. */
data class ListaDeCompras(val porComprar: List<Pendiente>, val enCarrito: List<Pendiente>)
