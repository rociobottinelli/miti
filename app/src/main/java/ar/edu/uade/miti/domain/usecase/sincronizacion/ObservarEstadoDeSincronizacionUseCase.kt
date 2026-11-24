package ar.edu.uade.miti.domain.usecase.sincronizacion

import ar.edu.uade.miti.domain.model.DatosDesactualizados
import ar.edu.uade.miti.domain.model.EstadoDeSincronizacion
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * El estado de sincronización para mostrar. Se recalcula cuando cambia algo y, además, cada minuto,
 * para que "desactualizado" se encienda solo aunque no pase nada más.
 */
class ObservarEstadoDeSincronizacionUseCase @Inject constructor(
    private val sincronizacion: SincronizacionRepository,
    private val reloj: Clock,
) {
    operator fun invoke(): Flow<EstadoDeSincronizacion> =
        combine(sincronizacion.observar(), cadaMinuto()) { situacion, _ ->
            EstadoDeSincronizacion(
                ultimaSincronizacion = situacion.ultimaSincronizacion,
                conConexion = situacion.conConexion,
                sincronizando = situacion.sincronizando,
                reintentando = situacion.reintentando,
                cambiosSinEnviar = situacion.idsSinEnviar.size,
                desactualizado = DatosDesactualizados.segun(situacion.ultimaSincronizacion, reloj.instant()),
            )
        }

    private fun cadaMinuto(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }
}
