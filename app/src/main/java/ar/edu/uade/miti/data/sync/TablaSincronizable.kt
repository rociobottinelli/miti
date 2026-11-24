package ar.edu.uade.miti.data.sync

import ar.edu.uade.miti.data.local.entity.MetadatosSync
import retrofit2.Response

/**
 * Cómo se sincroniza una tabla: qué funciones de Room y de la API usar y cómo traducir entre ellas.
 * El algoritmo es el mismo para todas (ver [Sincronizador]); esta clase solo junta las piezas, así no
 * se repite siete veces.
 *
 * @param E entidad de Room
 * @param D DTO de Supabase
 */
class TablaSincronizable<E, D>(
    val nombre: String,
    val aDto: (E) -> D,
    val aEntidad: (D) -> E,
    val id: (E) -> String,
    val metadatos: (E) -> MetadatosSync,
    val horaDelServidor: (D) -> String?,
    val obtenerLocal: suspend (id: String) -> E?,
    val guardarDescargadas: suspend (List<E>) -> Unit,
    val descargar: suspend (autorizacion: String, desde: String, limite: Int, offset: Int) -> Response<List<D>>,
    /** Null si la tabla solo se descarga (hogares e integrantes se crean en el servidor). */
    val subida: Subida<E, D>? = null,
    /** Filas que no estaban en el celular, después de guardarlas. [primeraVez]: la tabla nunca se había bajado. */
    val alBajarNuevas: (suspend (nuevas: List<E>, primeraVez: Boolean) -> Unit)? = null,
) {
    class Subida<E, D>(
        val porSubir: suspend () -> List<E>,
        val marcarSincronizada: suspend (id: String, updatedAt: Long) -> Int,
        val subir: suspend (autorizacion: String, filas: List<D>) -> Response<Unit>,
    )
}
