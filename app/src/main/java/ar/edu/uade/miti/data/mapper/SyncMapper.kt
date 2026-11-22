package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.converter.Convertidores
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.MetadatosSync

// Piezas comunes de los mappers entre Room y Supabase.

/** Los mismos formatos de texto que usa Room, así una fila viaja igual a la base local y al servidor. */
internal val convertidores = Convertidores()

/** Lo que llega del servidor ya está sincronizado; se conserva su updated_at para resolver conflictos (D08). */
internal fun sincronizado(updatedAt: Long, deleted: Boolean) =
    MetadatosSync(updatedAt = updatedAt, deleted = deleted, syncState = EstadoSync.SINCRONIZADO)
