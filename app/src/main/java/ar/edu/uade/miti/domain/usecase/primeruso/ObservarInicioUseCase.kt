package ar.edu.uade.miti.domain.usecase.primeruso

import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import ar.edu.uade.miti.domain.repository.OnboardingRepository
import ar.edu.uade.miti.domain.repository.SesionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** En qué punto del flujo 1 (preentrega) está este celular, o sea, dónde tiene que arrancar la app. */
enum class Inicio {
    /** Nunca se vio el onboarding. */
    ONBOARDING,

    /** Ya lo vio, pero no hay sesión: ingresar o crear cuenta. */
    CUENTA,

    /** Hay sesión, pero todavía no hay hogar: crear uno o unirse. */
    HOGAR,

    /** Sesión y hogar: directo a Hoy. */
    LISTO,
}

class ObservarInicioUseCase @Inject constructor(
    private val sesion: SesionRepository,
    private val ingreso: IngresoAHogarRepository,
    private val onboarding: OnboardingRepository,
) {
    operator fun invoke(): Flow<Inicio> =
        combine(sesion.observarUsuario(), ingreso.observarSiHayHogar(), onboarding.observarVisto()) { usuario, hayHogar, vioOnboarding ->
            when {
                // Con sesión el onboarding ya no importa: quien ingresó en otro celular no lo vuelve a ver.
                usuario != null && hayHogar -> Inicio.LISTO
                usuario != null -> Inicio.HOGAR
                !vioOnboarding -> Inicio.ONBOARDING
                else -> Inicio.CUENTA
            }
        }.distinctUntilChanged()
}
