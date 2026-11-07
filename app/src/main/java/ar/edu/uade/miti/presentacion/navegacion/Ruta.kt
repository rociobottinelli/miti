package ar.edu.uade.miti.presentacion.navegacion

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
}
