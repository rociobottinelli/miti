package ar.edu.uade.miti.presentation.primeruso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.sesion.CrearCuentaUseCase
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
 * Crear cuenta (flujo 1). Valida cada campo antes de ir a la red, para marcar el error al lado del campo.
 * Si sale bien no navega: la sesión nueva hace que el arranque pase solo a "Tu hogar".
 */
@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val crearCuenta: CrearCuentaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuentaUiState())
    val uiState: StateFlow<CuentaUiState> = _uiState.asStateFlow()

    fun cambiarNombre(valor: String) = _uiState.update { it.copy(nombre = valor, errorNombre = null, envio = EnvioDeCuenta.Editando) }

    fun cambiarEmail(valor: String) = _uiState.update { it.copy(email = valor, errorEmail = null, envio = EnvioDeCuenta.Editando) }

    fun cambiarContrasenia(valor: String) =
        _uiState.update { it.copy(contrasenia = valor, errorContrasenia = null, envio = EnvioDeCuenta.Editando) }

    fun crear() {
        val actual = _uiState.value
        if (actual.enviando) return
        val validado = actual.copy(
            errorNombre = ValidacionDeCuenta.errorDeNombre(actual.nombre),
            errorEmail = ValidacionDeCuenta.errorDeEmail(actual.email),
            errorContrasenia = ValidacionDeCuenta.errorDeContrasenia(actual.contrasenia),
        )
        if (validado.errorNombre != null || validado.errorEmail != null || validado.errorContrasenia != null) {
            _uiState.value = validado
            return
        }
        _uiState.value = validado.copy(envio = EnvioDeCuenta.Enviando)
        viewModelScope.launch {
            val resultado = crearCuenta(actual.nombre, actual.email, actual.contrasenia)
            if (resultado is Resource.Error) {
                _uiState.update { it.copy(envio = EnvioDeCuenta.Fallo(resultado.message, resultado.cause is IOException)) }
            }
        }
    }
}
