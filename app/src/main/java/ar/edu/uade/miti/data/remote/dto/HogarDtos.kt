package ar.edu.uade.miti.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Parámetros de las funciones crear_hogar y unirse_a_hogar, con los nombres de Postgres.

@JsonClass(generateAdapter = true)
data class CrearHogarDto(
    @Json(name = "nombre_hogar") val nombreHogar: String,
    @Json(name = "mi_nombre") val miNombre: String,
)

@JsonClass(generateAdapter = true)
data class UnirseAHogarDto(
    val codigo: String,
    @Json(name = "mi_nombre") val miNombre: String,
)

/** Respuesta de auth/v1/user. El nombre va en user_metadata desde que se creó la cuenta. */
@JsonClass(generateAdapter = true)
data class UsuarioConDatosDto(
    val id: String,
    val email: String? = null,
    @Json(name = "user_metadata") val datos: DatosDeUsuarioDto? = null,
)

@JsonClass(generateAdapter = true)
data class DatosDeUsuarioDto(val nombre: String? = null)

/** Error de PostgREST. Las funciones del hogar usan códigos propios: MITI1, MITI2 y MITI3. */
@JsonClass(generateAdapter = true)
data class ErrorDePostgrestDto(val code: String? = null, val message: String? = null)
