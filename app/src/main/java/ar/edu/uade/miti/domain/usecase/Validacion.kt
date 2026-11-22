package ar.edu.uade.miti.domain.usecase

import ar.edu.uade.miti.core.Resource

/**
 * Crea un modelo de dominio y, si no pasa sus validaciones (los `require` del init), devuelve
 * [Resource.Error] con el motivo en lugar de lanzar. Así un dato inválido nunca llega al repositorio.
 */
inline fun <T> validar(crear: () -> T): Resource<T> =
    try {
        Resource.Success(crear())
    } catch (e: IllegalArgumentException) {
        Resource.Error(e.message ?: "Los datos no son válidos", e)
    }
