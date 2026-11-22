package ar.edu.uade.miti.core

/**
 * Resultado de una operación que puede fallar, del repositorio al ViewModel.
 * El ViewModel lo traduce a su UiState: así un error nunca se confunde con "no hay datos".
 */
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}

/** Transforma el dato de un [Resource.Success]; un [Resource.Error] pasa tal cual. */
inline fun <T, R> Resource<T>.map(transformar: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transformar(data))
    is Resource.Error -> this
}

/**
 * El dato de un [Resource.Success]; con un [Resource.Error] ejecuta [siFalla]. En los casos de uso se usa
 * como `val x = algo().getOrElse { return it }` para cortar y devolver el error tal cual.
 */
inline fun <T> Resource<T>.getOrElse(siFalla: (Resource.Error) -> T): T = when (this) {
    is Resource.Success -> data
    is Resource.Error -> siFalla(this)
}
