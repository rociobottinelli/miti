package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.ListaDeCompras
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarListaDeComprasUseCase @Inject constructor(private val pendientes: PendienteRepository) {
    operator fun invoke(): Flow<ListaDeCompras> =
        pendientes.observarPendientes().map { todos ->
            val abiertas = todos.filter { it.tipo == TipoPendiente.COMPRA && !it.resuelto }.sortedBy { it.titulo }
            val (enCarrito, porComprar) = abiertas.partition { it.enCarrito }
            ListaDeCompras(porComprar, enCarrito)
        }
}
