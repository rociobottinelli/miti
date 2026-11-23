package ar.edu.uade.miti.presentation.primeruso

/**
 * Estado de los formularios de cuenta (crear e ingresar). Los errores de cada campo se muestran debajo
 * del campo; el resultado del envío va aparte para que un error del servidor no borre lo escrito.
 */
data class CuentaUiState(
    val nombre: String = "",
    val email: String = "",
    val contrasenia: String = "",
    val errorNombre: String? = null,
    val errorEmail: String? = null,
    val errorContrasenia: String? = null,
    val envio: EnvioDeCuenta = EnvioDeCuenta.Editando,
) {
    val enviando: Boolean get() = envio is EnvioDeCuenta.Enviando
}

sealed interface EnvioDeCuenta {
    data object Editando : EnvioDeCuenta

    /** Esperando al servidor. Si sale bien, la app pasa sola a la pantalla siguiente. */
    data object Enviando : EnvioDeCuenta

    /** [sinConexion] separa "no hay internet" (aviso arriba) de lo que respondió el servidor. */
    data class Fallo(val mensaje: String, val sinConexion: Boolean) : EnvioDeCuenta
}
