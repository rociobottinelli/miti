package ar.edu.uade.miti.presentation.primeruso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.sesion.IniciarSesionUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ValidacionDeCuenta
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ingresar (Figma 37:716, sin conexión 37:745, error 37:2849). Si sale bien no navega: con la sesión,
 * el arranque pasa a "Tu hogar", que busca si la persona ya tenía uno.
 */
@HiltViewModel
class IngresarViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuentaUiState())
    val uiState: StateFlow<CuentaUiState> = _uiState.asStateFlow()

    fun cambiarEmail(valor: String) = _uiState.update { it.copy(email = valor, errorEmail = null, envio = EnvioDeCuenta.Editando) }

    fun cambiarContrasenia(valor: String) =
        _uiState.update { it.copy(contrasenia = valor, errorContrasenia = null, envio = EnvioDeCuenta.Editando) }

    fun ingresar() {
        val actual = _uiState.value
        if (actual.enviando) return
        val validado = actual.copy(
            errorEmail = ValidacionDeCuenta.errorDeEmail(actual.email),
            // Al ingresar no se exige el largo mínimo: eso es solo para crear la contraseña.
            errorContrasenia = if (actual.contrasenia.isEmpty()) ValidacionDeCuenta.errorDeContrasenia("") else null,
        )
        if (validado.errorEmail != null || validado.errorContrasenia != null) {
            _uiState.value = validado
            return
        }
        _uiState.value = validado.copy(envio = EnvioDeCuenta.Enviando)
        viewModelScope.launch {
            val resultado = iniciarSesion(actual.email, actual.contrasenia)
            if (resultado is Resource.Error) {
                _uiState.update { it.copy(envio = EnvioDeCuenta.Fallo(resultado.message, resultado.cause is IOException)) }
            }
        }
    }
}
