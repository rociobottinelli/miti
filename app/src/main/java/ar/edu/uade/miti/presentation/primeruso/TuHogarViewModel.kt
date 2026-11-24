package ar.edu.uade.miti.presentation.primeruso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.hogar.CrearHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.RecuperarMiHogarUseCase
import ar.edu.uade.miti.domain.usecase.hogar.UnirseAHogarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TuHogarUiState {
    /** Al entrar: se busca si la persona ya tenía hogar (cambió de celular o reinstaló). */
    data object Buscando : TuHogarUiState

    data class Eligiendo(
        val codigo: String = "",
        val errorCodigo: String? = null,
        val enCurso: AccionDeHogar? = null,
        /** Un problema que no es del código (sin conexión, servidor caído): va arriba. */
        val aviso: AvisoDeHogar? = null,
    ) : TuHogarUiState

    /** Recién creado: se muestra el código para compartir hasta que la persona sigue. */
    data class Creado(val codigo: String) : TuHogarUiState

    /** Se unió o se recuperó el hogar: no hay nada más que mostrar. */
    data object Listo : TuHogarUiState
}

enum class AccionDeHogar { CREANDO, UNIENDOSE }

data class AvisoDeHogar(val mensaje: String, val sinConexion: Boolean)

/** "Tu hogar" (Figma 37:780, error del código 37:879). Todo lo que hace necesita conexión (sección 14). */
@HiltViewModel
class TuHogarViewModel @Inject constructor(
    private val recuperarMiHogar: RecuperarMiHogarUseCase,
    private val crearHogar: CrearHogarUseCase,
    private val unirseAHogar: UnirseAHogarUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TuHogarUiState>(TuHogarUiState.Buscando)
    val uiState: StateFlow<TuHogarUiState> = _uiState.asStateFlow()

    init {
        buscar()
    }

    /** También sirve para reintentar si la búsqueda falló por falta de conexión. */
    fun buscar() {
        _uiState.value = TuHogarUiState.Buscando
        viewModelScope.launch {
            _uiState.value = when (val r = recuperarMiHogar()) {
                is Resource.Success -> if (r.data) TuHogarUiState.Listo else TuHogarUiState.Eligiendo()
                // Si no se pudo buscar, igual se ofrece crear o unirse; el aviso explica qué pasó.
                is Resource.Error -> TuHogarUiState.Eligiendo(aviso = avisoDe(r))
            }
        }
    }

    fun cambiarCodigo(valor: String) = editar {
        // Se muestra como se va a mandar: mayúsculas y sin espacios, hasta 6 caracteres.
        copy(codigo = UnirseAHogarUseCase.normalizar(valor).take(6), errorCodigo = null)
    }

    fun crear(nombreHogar: String) = enviar(AccionDeHogar.CREANDO) {
        when (val r = crearHogar(nombreHogar)) {
            is Resource.Success -> TuHogarUiState.Creado(r.data.codigoUnion)
            is Resource.Error -> copy(enCurso = null, aviso = avisoDe(r))
        }
    }

    fun unirse() = enviar(AccionDeHogar.UNIENDOSE) {
        when (val r = unirseAHogar(codigo)) {
            is Resource.Success -> TuHogarUiState.Listo
            // Lo que dice el servidor del código ("Código inválido", "ya tiene dos integrantes") va en el campo.
            is Resource.Error ->
                if (r.cause is IOException) copy(enCurso = null, aviso = avisoDe(r)) else copy(enCurso = null, errorCodigo = r.message)
        }
    }

    private fun editar(cambio: TuHogarUiState.Eligiendo.() -> TuHogarUiState.Eligiendo) =
        _uiState.update { (it as? TuHogarUiState.Eligiendo)?.cambio() ?: it }

    private fun enviar(accion: AccionDeHogar, pedido: suspend TuHogarUiState.Eligiendo.() -> TuHogarUiState) {
        val actual = _uiState.value as? TuHogarUiState.Eligiendo ?: return
        if (actual.enCurso != null) return
        val enviando = actual.copy(enCurso = accion, aviso = null, errorCodigo = null)
        _uiState.value = enviando
        viewModelScope.launch { _uiState.value = enviando.pedido() }
    }

    private fun avisoDe(error: Resource.Error) = AvisoDeHogar(error.message, error.cause is IOException)
}
