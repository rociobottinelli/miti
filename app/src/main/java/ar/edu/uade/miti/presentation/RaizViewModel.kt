package ar.edu.uade.miti.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.domain.usecase.primeruso.Inicio
import ar.edu.uade.miti.domain.usecase.primeruso.MarcarOnboardingVistoUseCase
import ar.edu.uade.miti.domain.usecase.primeruso.ObservarInicioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Qué parte de la app se muestra. */
sealed interface DestinoRaiz {
    /** Todavía no se sabe: queda el splash. */
    data object Cargando : DestinoRaiz
    data object Onboarding : DestinoRaiz

    /** [empezarCreando]: si viene del onboarding, se abre Crear cuenta (con Ingresar detrás). */
    data class Cuenta(val empezarCreando: Boolean) : DestinoRaiz
    data object TuHogar : DestinoRaiz
    data object Permiso : DestinoRaiz
    data object App : DestinoRaiz
}

/**
 * Decide la pantalla a partir de [ObservarInicioUseCase] y reacciona sola: al crear la cuenta pasa a
 * "Tu hogar", al tener hogar pasa a la app, al cerrar sesión (#21) vuelve a la cuenta. Las pantallas no
 * navegan entre sí para estos saltos; solo cambian datos.
 */
@HiltViewModel
class RaizViewModel @Inject constructor(
    observarInicio: ObservarInicioUseCase,
    private val marcarOnboardingVisto: MarcarOnboardingVistoUseCase,
) : ViewModel() {

    /** Mientras se muestra el código del hogar recién creado, no se salta a la app aunque ya haya hogar. */
    private val mostrandoCodigo = MutableStateFlow(false)
    private val permisoResuelto = MutableStateFlow(false)
    private val vieneDelOnboarding = MutableStateFlow(false)

    /**
     * El permiso se pide una vez, al terminar el primer uso. Quien abre la app ya lista (con sesión y
     * hogar) no lo vuelve a ver: si lo rechazó, Hoy lo recuerda con un aviso.
     */
    private var pasoPorElPrimerUso: Boolean? = null

    val destino: StateFlow<DestinoRaiz> = combine(
        observarInicio(), mostrandoCodigo, permisoResuelto, vieneDelOnboarding,
    ) { inicio, codigo, permiso, onboarding ->
        if (pasoPorElPrimerUso == null) pasoPorElPrimerUso = inicio != Inicio.LISTO
        when (inicio) {
            Inicio.ONBOARDING -> DestinoRaiz.Onboarding
            Inicio.CUENTA -> DestinoRaiz.Cuenta(empezarCreando = onboarding)
            Inicio.HOGAR -> DestinoRaiz.TuHogar
            Inicio.LISTO -> when {
                codigo -> DestinoRaiz.TuHogar
                pasoPorElPrimerUso == true && !permiso -> DestinoRaiz.Permiso
                else -> DestinoRaiz.App
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DestinoRaiz.Cargando)

    fun terminarOnboarding() {
        vieneDelOnboarding.value = true
        viewModelScope.launch { marcarOnboardingVisto() }
    }

    fun empezarACrearHogar() {
        mostrandoCodigo.value = true
    }

    fun terminarTuHogar() {
        mostrandoCodigo.value = false
        // Ya entró: si después cierra sesión, vuelve a Ingresar y no a Crear cuenta.
        vieneDelOnboarding.value = false
    }

    fun resolverPermiso() {
        permisoResuelto.value = true
    }
}
