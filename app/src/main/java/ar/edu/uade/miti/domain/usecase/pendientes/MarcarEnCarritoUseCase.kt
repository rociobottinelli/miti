package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/** Pone o saca un ítem del carrito (tachar en el supermercado). */
class MarcarEnCarritoUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(compraId: String, enCarrito: Boolean) {
        val compra = requireNotNull(pendientes.obtenerPendiente(compraId)) { "No existe la compra $compraId" }
        require(compra.tipo == TipoPendiente.COMPRA) { "$compraId no es una compra" }
        pendientes.guardarPendientes(listOf(compra.copy(enCarrito = enCarrito)))
    }
}
