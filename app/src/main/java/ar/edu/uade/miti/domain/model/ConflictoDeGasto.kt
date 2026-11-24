package ar.edu.uade.miti.domain.model

/**
 * Una edición propia de un gasto que no se guardó porque la otra persona lo cambió antes (C2, #20).
 * Se muestra con los dos valores para que la persona elija: rehacer su edición o quedarse con la otra.
 */
data class ConflictoDeGasto(
    val id: String,
    /** Cómo quedó el gasto (la versión que ganó). Null si ya no está en el celular. */
    val actual: Gasto?,
    /** Cómo lo había dejado esta persona. */
    val miVersion: Gasto,
    /** La edición que no entró era borrar el gasto. */
    val eraBorrado: Boolean,
)
