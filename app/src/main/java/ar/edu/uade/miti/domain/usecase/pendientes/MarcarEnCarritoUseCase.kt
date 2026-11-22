package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject

/** Pone o saca un ítem del carrito (tachar en el supermercado). */
class MarcarEnCarritoUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    suspend operator fun invoke(compraId: String, enCarrito: Boolean): Resource<Unit> {
        val compra = pendientes.obtenerPendiente(compraId).getOrElse { return it }
            ?: return Resource.Error("Ese ítem ya no está en la lista")
        if (compra.tipo != TipoPendiente.COMPRA) return Resource.Error("Eso no es un ítem de la lista de compras")
        return pendientes.guardarPendientes(listOf(compra.copy(enCarrito = enCarrito)))
    }
}
