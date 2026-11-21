package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.domain.model.BalanceDelMes
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.HogarRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Balance de [mes], que se recalcula solo cada vez que cambia un gasto, un pago o un integrante (RF04).
 * Emite null mientras el hogar no tenga dos integrantes, porque no hay entre quiénes repartir.
 */
class ObtenerBalanceDelMesUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val gastos: GastoRepository,
) {
    operator fun invoke(mes: YearMonth): Flow<BalanceDelMes?> =
        combine(hogares.observarIntegrantes(), gastos.observarGastos(), gastos.observarPagos()) { integrantes, todosLosGastos, todosLosPagos ->
            if (integrantes.size < 2) return@combine null
            // Orden fijo por id para que A y B sean siempre los mismos en los dos celulares.
            val (a, b) = integrantes.map { it.id }.sorted()
            CalculadoraDeBalance.calcular(mes, a, b, todosLosGastos, todosLosPagos)
        }
}
