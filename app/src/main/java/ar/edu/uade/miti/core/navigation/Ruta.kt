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
