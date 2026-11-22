package ar.edu.uade.miti.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Espejo del JSON de Supabase Auth (GoTrue). Solo los campos que usa la app.

@JsonClass(generateAdapter = true)
data class CrearCuentaDto(
    val email: String,
    val password: String,
    /** Va a user_metadata; el nombre del integrante se guarda al crear o unirse a un hogar. */
    val data: MetadatosDeUsuarioDto,
)

@JsonClass(generateAdapter = true)
data class MetadatosDeUsuarioDto(val nombre: String)

@JsonClass(generateAdapter = true)
data class CredencialesDto(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RenovarSesionDto(@Json(name = "refresh_token") val refreshToken: String)

/**
 * Respuesta de ingresar, renovar y crear cuenta. Si el proyecto exige confirmar el email, crear cuenta
 * devuelve el usuario sin tokens: por eso todo es opcional.
 */
@JsonClass(generateAdapter = true)
data class SesionDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    /** Segundos hasta que vence el access token. */
    @Json(name = "expires_in") val expiresIn: Long? = null,
    val user: UsuarioDto? = null,
)

@JsonClass(generateAdapter = true)
data class UsuarioDto(val id: String, val email: String? = null)

/** Error de Supabase Auth. Las versiones nuevas mandan error_code/msg; las viejas, error/error_description. */
@JsonClass(generateAdapter = true)
data class ErrorDeAuthDto(
    @Json(name = "error_code") val errorCode: String? = null,
    val msg: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
)
