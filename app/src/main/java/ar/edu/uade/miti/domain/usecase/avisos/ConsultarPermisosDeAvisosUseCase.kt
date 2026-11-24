package ar.edu.uade.miti.domain.usecase.avisos

import ar.edu.uade.miti.domain.model.PermisosDeAvisos
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import javax.inject.Inject

/**
 * Si el celular puede mostrar notificaciones y programar alarmas exactas. Conviene consultarlo al volver a
 * la pantalla (la persona puede haber cambiado el permiso en Ajustes del sistema).
 */
class ConsultarPermisosDeAvisosUseCase @Inject constructor(private val alarmas: AlarmasDeTareas) {
    operator fun invoke(): PermisosDeAvisos = alarmas.permisos()
}
