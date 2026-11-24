package ar.edu.uade.miti.presentation.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.hogar.ObservarHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.sesion.CerrarSesionUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AjustesUiState {
    data object Cargando : AjustesUiState
    data object Error : AjustesUiState

    data class Contenido(
        val integrantes: List<IntegranteDeAjustes>,
        /** Null mientras los datos del hogar no bajaron al celular. */
        val codigo: String?,
        val cierre: CierreDeSesion = CierreDeSesion.Nada,
    ) : AjustesUiState
}

/** [posicion] decide el color, igual que en Hoy. */
data class IntegranteDeAjustes(val id: String, val nombre: String, val email: String, val posicion: Int, val esYo: Boolean)

sealed interface CierreDeSesion {
    data object Nada : CierreDeSesion

    /** Se muestra "¿Cerrar sesión?" (Figma 37:980). */
    data object Confirmando : CierreDeSesion
    data object Cerrando : CierreDeSesion
    data class NoSePudo(val mensaje: String) : CierreDeSesion
}

/**
 * Ajustes (Figma 37:1023, preentrega sección 11). Cerrar sesión borra los datos del celular; los del
 * hogar siguen en el servidor. Al cerrar, la raíz de la app (#17) vuelve sola a Ingresar.
 */
@HiltViewModel
class AjustesViewModel @Inject constructor(
    observarHogar: ObservarHogarUseCase,
    observarIntegrantes: ObservarIntegrantesUseCase,
    observarUsuario: ObservarUsuarioUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
) : ViewModel() {

    private val cierre = MutableStateFlow<CierreDeSesion>(CierreDeSesion.Nada)

    val uiState: StateFlow<AjustesUiState> =
        combine(observarHogar(), observarIntegrantes(), observarUsuario(), cierre) { hogar, integrantes, usuario, cierre ->
            AjustesUiState.Contenido(
                integrantes = integrantes.mapIndexed { posicion, it ->
                    IntegranteDeAjustes(it.id, it.nombre, it.email, posicion, esYo = it.id == usuario?.id)
                }.sortedByDescending { it.esYo },
                codigo = hogar?.codigoUnion,
                cierre = cierre,
            ) as AjustesUiState
        }
            .catch { emit(AjustesUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState.Cargando)

    fun pedirCerrarSesion() {
        cierre.value = CierreDeSesion.Confirmando
    }

    fun cancelarCierre() {
        cierre.value = CierreDeSesion.Nada
    }

    fun confirmarCierre() {
        if (cierre.value == CierreDeSesion.Cerrando) return
        cierre.value = CierreDeSesion.Cerrando
        viewModelScope.launch {
            when (val r = cerrarSesion()) {
                // No hay que navegar: sin sesión, la raíz de la app muestra Ingresar.
                is Resource.Success -> Unit
                is Resource.Error -> cierre.value = CierreDeSesion.NoSePudo(r.message)
            }
        }
    }
}
