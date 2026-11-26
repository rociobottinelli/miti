package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.PermisosDeAvisos

/** Las alarmas locales del celular (D10). La implementación usa AlarmManager; el dominio no lo sabe. */
interface AlarmasDeTareas {
    /**
     * Deja programadas exactamente estas tareas: agrega las que faltan, actualiza la hora y cancela el resto.
     * De las canceladas, retira la notificación solo si su id no está en [avisosVigentes] (una tarea que ya
     * sonó y sigue pendiente conserva la suya).
     */
    suspend fun programarSolo(tareas: List<Pendiente>, avisosVigentes: Set<String>)

    fun permisos(): PermisosDeAvisos
}
