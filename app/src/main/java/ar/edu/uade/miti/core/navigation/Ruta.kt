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

    /** Se abre desde el avatar de Hoy; sin barra inferior (#21). */
    @Serializable
    data object Ajustes : Ruta
}
