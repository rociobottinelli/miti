package ar.edu.uade.miti.core.navigation

import kotlinx.serialization.Serializable

/** Rutas tipadas de Navigation Compose. */
@Serializable
sealed interface Ruta {
    @Serializable
    data object Hoy : Ruta

    @Serializable
    data object Compras : Ruta

    @Serializable
    data object Balance : Ruta

    /** Registrar un gasto, o editarlo si viene [gastoId] (#14). Se pasa el id, nunca el gasto entero. */
    @Serializable
    data class FormularioDeGasto(val gastoId: String? = null) : Ruta

    /** Se abre desde el avatar de Hoy; sin barra inferior (#21). */
    @Serializable
    data object Ajustes : Ruta

    // Primer uso (#17). La raíz elige entre estas y Principal según la sesión y el hogar.

    @Serializable
    data object Onboarding : Ruta

    @Serializable
    data object Ingresar : Ruta

    @Serializable
    data object CrearCuenta : Ruta

    @Serializable
    data object TuHogar : Ruta

    @Serializable
    data object PermisoDeNotificaciones : Ruta

    /** La app con la barra inferior (Hoy, Compras, Balance), que tiene su propia navegación. */
    @Serializable
    data object Principal : Ruta
}
