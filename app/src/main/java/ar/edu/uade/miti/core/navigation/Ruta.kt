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
}
