package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Cierra la compra: resuelve todo lo que está en el carrito. Si el usuario aceptó registrar
 * el gasto, lo crea y lo vincula a cada ítem comprado (flujo 3); si no, la compra queda cerrada
 * sin gasto. Devuelve el gasto creado, si hubo.
 *
 * Si el gasto no es válido o no se puede guardar, la compra queda abierta (nada cambia), así el
 * usuario corrige y vuelve a intentar.
 */
class TerminarCompraUseCase @Inject constructor(
    private val pendientes: PendienteRepository,
    private val registrarGasto: RegistrarGastoUseCase,
) {
    suspend operator fun invoke(gasto: DatosDeGasto?): Resource<Gasto?> {
        val enCarrito = pendientes.observarPendientes().first()
            .filter { it.tipo == TipoPendiente.COMPRA && it.enCarrito && !it.resuelto }
        if (enCarrito.isEmpty()) return Resource.Error("No hay nada en el carrito")

        // Primero el gasto, para que los ítems ya puedan apuntar a su id.
        val gastoCreado = gasto?.let { registrarGasto(it).getOrElse { error -> return error } }
        return pendientes.guardarPendientes(
            enCarrito.map { it.copy(enCarrito = false, resuelto = true, gastoId = gastoCreado?.id) },
        ).map { gastoCreado }
    }
}
