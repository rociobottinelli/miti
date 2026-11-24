package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.repository.GastoRepository
import javax.inject.Inject

/** Un gasto para editarlo (#14). Error si ya no existe (por ejemplo, lo borró la otra persona). */
class ObtenerGastoUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(id: String): Resource<Gasto> = when (val r = gastos.obtenerGasto(id)) {
        is Resource.Success -> r.data?.let { Resource.Success(it) } ?: Resource.Error("El gasto ya no existe")
        is Resource.Error -> r
    }
}
