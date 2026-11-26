package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.yaCoinciden
import ar.edu.uade.miti.domain.repository.GastoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Ediciones propias de gastos que no entraron por un conflicto (C2, #20). Balance las muestra arriba.
 * Si lo que quedó ya es igual a lo que puso esta persona (por ejemplo, rehízo su cambio desde otro celular),
 * no hay nada que avisar y no se muestra: la tarjeta diría "lo cambió antes" sin ninguna diferencia.
 */
class ObservarConflictosDeGastoUseCase @Inject constructor(private val gastos: GastoRepository) {
    operator fun invoke(): Flow<List<ConflictoDeGasto>> =
        gastos.observarConflictos().map { conflictos -> conflictos.filterNot { it.yaCoinciden } }
}

/**
 * La persona elige su versión: se vuelve a aplicar como una edición nueva sobre lo que hay ahora (así no
 * vuelve a chocar con la misma edición del otro) y el conflicto se cierra. El otro integrante la recibe
 * con la próxima sincronización.
 */
class RehacerMiEdicionUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(conflicto: ConflictoDeGasto): Resource<Unit> {
        if (conflicto.eraBorrado) {
            gastos.eliminarGasto(conflicto.miVersion.id).getOrElse { return it }
        } else {
            gastos.guardarGasto(conflicto.miVersion).getOrElse { return it }
        }
        return gastos.resolverConflicto(conflicto.id)
    }
}

/** La persona se queda con la versión que ganó: el conflicto deja de mostrarse. */
class DescartarConflictoUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(conflicto: ConflictoDeGasto): Resource<Unit> = gastos.resolverConflicto(conflicto.id)
}
