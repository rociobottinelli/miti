package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.repository.GastoRepository
import javax.inject.Inject

/** Borra un gasto (borrado lógico, D07). Sale del balance en los dos celulares al sincronizar. */
class EliminarGastoUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(id: String): Resource<Unit> = gastos.eliminarGasto(id)
}
