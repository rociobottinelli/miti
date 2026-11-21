package ar.edu.uade.miti.domain.usecase.sesion

/**
 * Reglas de los formularios de cuenta e ingreso. Se validan antes de llamar al servidor para dar el error
 * al lado del campo y no gastar un viaje por la red. Devuelven el mensaje de error, o null si está bien.
 */
object ValidacionDeCuenta {
    /** Mínimo que exige Supabase Auth por defecto. */
    const val LARGO_MINIMO_CONTRASENIA = 6

    private val formatoEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun errorDeNombre(nombre: String): String? =
        if (nombre.isBlank()) "Ingresá tu nombre" else null

    fun errorDeEmail(email: String): String? = when {
        email.isBlank() -> "Ingresá tu email"
        !formatoEmail.matches(email.trim()) -> "El email no tiene un formato válido"
        else -> null
    }

    fun errorDeContrasenia(contrasenia: String): String? = when {
        contrasenia.isEmpty() -> "Ingresá tu contraseña"
        contrasenia.length < LARGO_MINIMO_CONTRASENIA ->
            "La contraseña tiene que tener al menos $LARGO_MINIMO_CONTRASENIA caracteres"
        else -> null
    }
}
