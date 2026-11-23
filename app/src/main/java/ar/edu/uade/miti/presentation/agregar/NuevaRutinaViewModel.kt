package ar.edu.uade.miti.presentation.agregar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.usecase.rutinas.RegistrarRutinaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevaRutinaUiState(
    val nombre: String = "",
    val frecuencia: Frecuencia = Frecuencia.DIARIA,
    val dias: Set<DayOfWeek> = emptySet(),
    val vecesPorDia: Int = 1,
    val errorNombre: ErrorDeFormulario? = null,
    val errorDias: ErrorDeFormulario? = null,
    val guardando: Boolean = false,
    /** Si falló al guardar en el celular (no debería: es Room, no la red). */
    val errorAlGuardar: String? = null,
) {
    /** Como en el Figma, "Guardar rutina" se ve deshabilitado hasta que hay un nombre. */
    val puedeGuardar: Boolean get() = nombre.isNotBlank() && !guardando
}

/** Errores de validación; la pantalla elige el texto (strings_agregar.xml). */
enum class ErrorDeFormulario { NOMBRE_VACIO, SIN_DIAS, FECHA_PASADA }

/**
 * Nueva rutina (Figma 37:2544). Se guarda en Room y aparece en Hoy al instante, con o sin conexión (RF01);
 * la sincronización la sube después.
 */
@HiltViewModel
class NuevaRutinaViewModel @Inject constructor(
    private val registrarRutina: RegistrarRutinaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaRutinaUiState())
    val uiState: StateFlow<NuevaRutinaUiState> = _uiState.asStateFlow()

    /** Se emite el nombre de la rutina guardada, para el aviso "Rutina agregada". */
    private val _guardada = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val guardada: SharedFlow<String> = _guardada.asSharedFlow()

    fun cambiarNombre(valor: String) = _uiState.update { it.copy(nombre = valor, errorNombre = null) }

    fun elegirFrecuencia(frecuencia: Frecuencia) = _uiState.update { it.copy(frecuencia = frecuencia, errorDias = null) }

    fun alternarDia(dia: DayOfWeek) = _uiState.update {
        it.copy(dias = if (dia in it.dias) it.dias - dia else it.dias + dia, errorDias = null)
    }

    fun elegirVeces(veces: Int) = _uiState.update { it.copy(vecesPorDia = veces.coerceIn(1, 3)) }

    fun guardar() {
        val actual = _uiState.value
        if (actual.guardando) return
        val validado = actual.copy(
            errorNombre = if (actual.nombre.isBlank()) ErrorDeFormulario.NOMBRE_VACIO else null,
            errorDias = if (actual.frecuencia == Frecuencia.DIAS_DE_LA_SEMANA && actual.dias.isEmpty()) ErrorDeFormulario.SIN_DIAS else null,
            errorAlGuardar = null,
        )
        if (validado.errorNombre != null || validado.errorDias != null) {
            _uiState.value = validado
            return
        }
        _uiState.value = validado.copy(guardando = true)
        viewModelScope.launch {
            when (val r = registrarRutina(actual.nombre, actual.frecuencia, actual.dias, actual.vecesPorDia)) {
                is Resource.Success -> {
                    // Queda listo para la próxima: la hoja se cierra y, si se vuelve a abrir, empieza vacía.
                    _uiState.value = NuevaRutinaUiState()
                    _guardada.emit(r.data.titulo)
                }
                is Resource.Error -> _uiState.update { it.copy(guardando = false, errorAlGuardar = r.message) }
            }
        }
    }
}
