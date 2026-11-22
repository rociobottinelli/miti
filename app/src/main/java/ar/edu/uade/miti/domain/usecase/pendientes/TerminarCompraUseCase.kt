package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
