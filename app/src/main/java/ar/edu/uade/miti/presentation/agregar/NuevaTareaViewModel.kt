package ar.edu.uade.miti.presentation.agregar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarTareaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una persona del hogar como opción de responsable. */
data class Responsable(val id: String, val nombre: String)

data class NuevaTareaUiState(
    val nombre: String = "",
    /** Primero quien usa el celular, que es el responsable por defecto. */
    val responsables: List<Responsable> = emptyList(),
    val responsableId: String? = null,
    val fecha: LocalDate,
    val hora: LocalTime,
    val avisar: Boolean = true,
    val errorNombre: ErrorDeFormulario? = null,
    val errorFecha: ErrorDeFormulario? = null,
    val guardando: Boolean = false,
    val errorAlGuardar: String? = null,
) {
    val puedeGuardar: Boolean get() = nombre.isNotBlank() && responsableId != null && !guardando
}

/** Nueva tarea (Figma 37:2226): una vez, con fecha y hora, responsable y aviso opcional (RF01). */
@HiltViewModel
class NuevaTareaViewModel @Inject constructor(
    private val registrarTarea: RegistrarTareaUseCase,
    observarIntegrantes: ObservarIntegrantesUseCase,
    observarUsuario: ObservarUsuarioUseCase,
    private val reloj: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(vacio())
    val uiState: StateFlow<NuevaTareaUiState> = _uiState.asStateFlow()

    private val _guardada = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val guardada: SharedFlow<String> = _guardada.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(observarIntegrantes(), observarUsuario()) { integrantes, usuario ->
                val yo = integrantes.filter { it.id == usuario?.id }
                (yo + (integrantes - yo.toSet())).map { Responsable(it.id, it.nombre) }
            }.collect { opciones ->
                _uiState.update { estado ->
                    // Se mantiene la elección si la persona sigue en el hogar; si no, quien usa el celular.
                    val elegido = estado.responsableId?.takeIf { id -> opciones.any { it.id == id } } ?: opciones.firstOrNull()?.id
                    estado.copy(responsables = opciones, responsableId = elegido)
                }
            }
        }
    }

    /** Hoy, a la próxima hora en punto: lo más común es anotar algo para más tarde. */
    private fun vacio(): NuevaTareaUiState {
        val proxima = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.HOURS).plusHours(1)
        return NuevaTareaUiState(fecha = proxima.toLocalDate(), hora = proxima.toLocalTime())
    }

    fun cambiarNombre(valor: String) = _uiState.update { it.copy(nombre = valor, errorNombre = null) }

    fun elegirResponsable(id: String) = _uiState.update { it.copy(responsableId = id) }

    fun elegirFecha(fecha: LocalDate) = _uiState.update { it.copy(fecha = fecha, errorFecha = null) }

    fun elegirHora(hora: LocalTime) = _uiState.update { it.copy(hora = hora, errorFecha = null) }

    fun cambiarAviso(avisar: Boolean) = _uiState.update { it.copy(avisar = avisar) }

    fun guardar() {
        val actual = _uiState.value
        if (actual.guardando) return
        val fechaHora = actual.fecha.atTime(actual.hora)
        val validado = actual.copy(
            errorNombre = if (actual.nombre.isBlank()) ErrorDeFormulario.NOMBRE_VACIO else null,
            errorFecha = if (!fechaHora.isAfter(LocalDateTime.now(reloj))) ErrorDeFormulario.FECHA_PASADA else null,
            errorAlGuardar = null,
        )
        val responsable = actual.responsableId
        if (validado.errorNombre != null || validado.errorFecha != null || responsable == null) {
            _uiState.value = validado
            return
        }
        _uiState.value = validado.copy(guardando = true)
        viewModelScope.launch {
            when (val r = registrarTarea(actual.nombre, responsable, fechaHora, actual.avisar)) {
                is Resource.Success -> {
                    // Vacío para la próxima, pero conservando la lista de responsables.
                    _uiState.value = vacio().copy(responsables = actual.responsables, responsableId = actual.responsables.firstOrNull()?.id)
                    _guardada.emit(r.data.titulo)
                }
                is Resource.Error -> _uiState.update { it.copy(guardando = false, errorAlGuardar = r.message) }
            }
        }
    }
}
