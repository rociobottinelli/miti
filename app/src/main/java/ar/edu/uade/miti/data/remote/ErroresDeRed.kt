package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.data.remote.dto.ErrorDeAuthDto

// Mensajes para personas (qué pasó y qué hacer), según los estados de la preentrega (sección 11).
internal const val SIN_CONEXION = "Necesitás conexión para ingresar. Revisá tu internet y probá de nuevo."
internal const val RESPUESTA_INESPERADA = "El servidor respondió algo inesperado. Probá de nuevo en un rato."
internal const val SIN_CONFIGURAR = "La app no tiene configurado el servidor (falta SUPABASE_URL en local.properties)."
internal const val SESION_VENCIDA = "Tu sesión venció. Volvé a ingresar."

/** Traduce un error de Supabase Auth a un mensaje para la persona. */
internal fun mensajeDeErrorDeAuth(codigoHttp: Int, error: ErrorDeAuthDto?): String {
    val codigo = error?.errorCode ?: error?.error
    return when (codigo) {
        "invalid_credentials", "invalid_grant" -> "Email o contraseña incorrectos"
        "user_already_exists", "email_exists" -> "Ya existe una cuenta con ese email. Probá ingresar."
        "weak_password" -> "La contraseña es muy débil. Usá al menos 6 caracteres."
        "email_address_invalid", "validation_failed" -> "El email no tiene un formato válido"
        "email_not_confirmed" -> "Tenés que confirmar tu email antes de ingresar. Revisá tu correo."
        "over_request_rate_limit", "over_email_send_rate_limit" ->
            "Hubo demasiados intentos. Esperá un minuto y probá de nuevo."
        "signup_disabled" -> "Por ahora no se pueden crear cuentas nuevas."
        else -> when {
            codigoHttp == 429 -> "Hubo demasiados intentos. Esperá un minuto y probá de nuevo."
            codigoHttp >= 500 -> "El servidor no está respondiendo ($codigoHttp). Probá de nuevo en un rato."
            else -> "No se pudo completar la operación ($codigoHttp)."
        }
    }
}
