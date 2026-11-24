package ar.edu.uade.miti.domain.usecase

import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.RutinaRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Indica si el hogar ya tiene alguna rutina o tarea cargada, de cualquier día y aunque esté resuelta.
 * Hoy lo usa para elegir entre dos estados que se ven parecidos pero dicen cosas distintas:
 * "Tu hogar está listo, agregá tu primera rutina" (no hay nada) y "Todo al día por hoy" (hay, pero
 * hoy no queda nada por hacer; por ejemplo, una rutina de los lunes un martes). Las compras no cuentan:
 * viven en su propia pantalla.
 */
class ObservarSiHayItemsUseCase @Inject constructor(
    private val rutinas: RutinaRepository,
    private val pendientes: PendienteRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        combine(rutinas.observarRutinas(), pendientes.observarPendientes()) { todasLasRutinas, todosLosPendientes ->
            todasLasRutinas.isNotEmpty() || todosLosPendientes.any { it.tipo == TipoPendiente.TAREA }
        }.distinctUntilChanged()
}
