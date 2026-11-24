package ar.edu.uade.miti.presentation.widget

import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * El estado de los widgets, recalculado cuando cambia algo en Room (venga de la app, del widget o de la
 * sincronización) y al pasar la medianoche, cuando las rutinas se reinician.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservarEstadoDelWidget @Inject constructor(
    private val observarUsuario: ObservarUsuarioUseCase,
    private val observarIntegrantes: ObservarIntegrantesUseCase,
    private val observarRutinasDelDia: ObservarRutinasDelDiaUseCase,
    private val observarEstadoDeSincronizacion: ObservarEstadoDeSincronizacionUseCase,
    private val observarItemsSinEnviar: ObservarItemsSinEnviarUseCase,
    private val reloj: Clock,
) {
    operator fun invoke(): Flow<EstadoDelWidget> = fechaDeHoy().flatMapLatest { hoy ->
        combine(
            observarUsuario(), observarIntegrantes(), observarRutinasDelDia(hoy), observarEstadoDeSincronizacion(), observarItemsSinEnviar(),
        ) { usuario, integrantes, rutinas, sync, sinEnviar ->
            EstadoDelWidget.desde(usuario, integrantes, rutinas, sync, sinEnviar, hoy, reloj.zone)
        }
    }.distinctUntilChanged()

    private fun fechaDeHoy(): Flow<LocalDate> = flow {
        while (true) {
            val ahora = LocalDateTime.now(reloj)
            emit(ahora.toLocalDate())
            val medianoche = ahora.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(ahora, medianoche).toMillis() + 1)
        }
    }.distinctUntilChanged()
}
