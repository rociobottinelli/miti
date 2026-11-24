package ar.edu.uade.miti.domain.usecase.sincronizacion

import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Ids de rutinas, tareas, compras, gastos y pagos con cambios sin enviar. Cada pantalla pregunta
 * `item.id in ids` para mostrar el ícono de "pendiente de sincronizar" (RNF09) sin que los modelos del
 * dominio tengan que saber de sincronización.
 */
class ObservarItemsSinEnviarUseCase @Inject constructor(private val sincronizacion: SincronizacionRepository) {
    operator fun invoke(): Flow<Set<String>> = sincronizacion.observar().map { it.idsSinEnviar }.distinctUntilChanged()
}
