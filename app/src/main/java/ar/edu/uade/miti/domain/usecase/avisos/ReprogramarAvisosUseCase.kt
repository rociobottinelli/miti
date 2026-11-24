package ar.edu.uade.miti.domain.usecase.avisos

import ar.edu.uade.miti.domain.model.TareasConAviso
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.SesionRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Pone las alarmas al día con lo que hay en el celular. Se llama después de cada cambio en tareas, al
 * terminar una sincronización, al abrir la app y al reiniciar el teléfono (Android borra las alarmas al
 * apagarse). Siempre parte de Room, así que llamarlo de más no cambia nada.
 */
class ReprogramarAvisosUseCase @Inject constructor(
    private val pendientes: PendienteRepository,
    private val sesion: SesionRepository,
    private val alarmas: AlarmasDeTareas,
    private val reloj: Clock,
) {
    suspend operator fun invoke() {
        val yo = sesion.observarUsuario().first()?.id
        val tareas = TareasConAviso.paraProgramar(pendientes.observarPendientes().first(), yo, LocalDateTime.now(reloj))
        alarmas.programarSolo(tareas)
    }
}
