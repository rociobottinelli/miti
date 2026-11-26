package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.repository.GastoRepository
import javax.inject.Inject

/**
 * Borra un pago cargado por error (borrado lógico, D07). El balance se recalcula solo (D09) y el otro celular lo
 * recibe al sincronizar. Sin esto, un pago mal cargado dejaba el balance mal para siempre.
 */
class EliminarPagoUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(id: String): Resource<Unit> = gastos.eliminarPago(id)
}
