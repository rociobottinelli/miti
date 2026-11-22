package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import java.time.LocalDateTime
import javax.inject.Inject

class RegistrarTareaUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String, responsableId: String, fechaHora: LocalDateTime, avisar: Boolean): Pendiente {
        val tarea = Pendiente(
            id = ids.nuevoId(),
            hogarId = hogares.requerirHogarActivo(),
            tipo = TipoPendiente.TAREA,
            titulo = titulo.trim(),
            responsableId = responsableId,
            fechaHora = fechaHora,
            avisar = avisar,
        )
        pendientes.guardarPendientes(listOf(tarea))
        return tarea
    }
}
