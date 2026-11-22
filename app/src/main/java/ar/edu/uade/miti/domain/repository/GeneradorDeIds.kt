package ar.edu.uade.miti.domain.repository

/** Genera los ids de las entidades nuevas. En la app son UUID (D07); en los tests, predecibles. */
fun interface GeneradorDeIds {
    fun nuevoId(): String
}
