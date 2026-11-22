package ar.edu.uade.miti.data.local.entity

// Tablas de Room. Son el reflejo de los modelos de dominio más los datos de sincronización,
// que el dominio no conoce. No hay claves foráneas a propósito: con la sincronización, una fila
// puede llegar antes que la fila a la que apunta (por ejemplo, una marca antes que su rutina).

enum class EstadoSync {
    /** Igual que en el servidor. */
    SINCRONIZADO,

    /** Cambió en este celular y todavía no se envió (D06). */
    PENDIENTE,
}

/** Columnas que comparten todas las tablas sincronizables (preentrega, sección 14). */
data class MetadatosSync(
    /** Momento del último cambio, en milisegundos. Decide qué cambio gana en un conflicto (D08). */
    val updatedAt: Long,
    /** Borrado lógico: la fila queda para que el borrado viaje al otro celular (D07). */
    val deleted: Boolean = false,
    val syncState: EstadoSync = EstadoSync.PENDIENTE,
)
