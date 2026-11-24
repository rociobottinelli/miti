package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.PermisosDeAvisos

/** Las alarmas locales del celular (D10). La implementación usa AlarmManager; el dominio no lo sabe. */
interface AlarmasDeTareas {
    /** Deja programadas exactamente estas tareas: agrega las que faltan, actualiza la hora y cancela el resto. */
    suspend fun programarSolo(tareas: List<Pendiente>)

    fun permisos(): PermisosDeAvisos
}
