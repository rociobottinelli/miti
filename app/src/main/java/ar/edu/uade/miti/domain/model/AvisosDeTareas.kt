package ar.edu.uade.miti.domain.model

import java.time.LocalDateTime

/**
 * Qué tareas tienen que sonar en este celular (D10, #23): las que piden aviso, siguen sin resolver, son
 * de quien usa este celular (el aviso va al responsable) y todavía no llegó su hora. Todo lo demás se
 * cancela: si una tarea se resolvió, se borró o se le cambió el responsable, su alarma deja de sonar.
 */
object TareasConAviso {
    fun paraProgramar(tareas: List<Pendiente>, yoId: String?, ahora: LocalDateTime): List<Pendiente> =
        tareas.filter { tarea -> tarea.avisoVigente(yoId) && tarea.fechaHora?.isAfter(ahora) == true }
            .sortedBy { it.fechaHora }

    /**
     * Las tareas cuyo aviso todavía tiene sentido, haya llegado su hora o no. Una tarea que ya sonó deja de
     * programarse, pero su notificación sigue a la vista hasta que alguien la resuelva: solo se retira si la
     * tarea se resolvió, se borró o cambió de responsable.
     */
    fun conAvisoVigente(tareas: List<Pendiente>, yoId: String?): Set<String> =
        tareas.filter { it.avisoVigente(yoId) }.map { it.id }.toSet()

    private fun Pendiente.avisoVigente(yoId: String?) =
        yoId != null && tipo == TipoPendiente.TAREA && avisar && !resuelto && responsableId == yoId
}

/** Qué puede hacer el celular con los avisos. Hoy muestra un aviso si alguno falta (preentrega, sección 11). */
data class PermisosDeAvisos(
    /** Android 13+ pide permiso para mostrar notificaciones. Sin él, los avisos no se ven. */
    val notificaciones: Boolean,
    /** Android 12+ pide permiso para alarmas exactas. Sin él suenan, pero con un desvío que no garantiza RNF06. */
    val alarmasExactas: Boolean,
)
