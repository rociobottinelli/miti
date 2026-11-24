package ar.edu.uade.miti.domain.model

import java.time.LocalDateTime

/**
 * Qué tareas tienen que sonar en este celular (D10, #23): las que piden aviso, siguen sin resolver, son
 * de quien usa este celular (el aviso va al responsable) y todavía no llegó su hora. Todo lo demás se
 * cancela: si una tarea se resolvió, se borró o se le cambió el responsable, su alarma deja de sonar.
 */
object TareasConAviso {
    fun paraProgramar(tareas: List<Pendiente>, yoId: String?, ahora: LocalDateTime): List<Pendiente> {
        if (yoId == null) return emptyList()
        return tareas.filter { tarea ->
            val hora = tarea.fechaHora
            tarea.tipo == TipoPendiente.TAREA && tarea.avisar && !tarea.resuelto &&
                tarea.responsableId == yoId && hora != null && hora.isAfter(ahora)
        }.sortedBy { it.fechaHora }
    }
}

/** Qué puede hacer el celular con los avisos. Hoy muestra un aviso si alguno falta (preentrega, sección 11). */
data class PermisosDeAvisos(
    /** Android 13+ pide permiso para mostrar notificaciones. Sin él, los avisos no se ven. */
    val notificaciones: Boolean,
    /** Android 12+ pide permiso para alarmas exactas. Sin él suenan, pero con un desvío que no garantiza RNF06. */
    val alarmasExactas: Boolean,
)
