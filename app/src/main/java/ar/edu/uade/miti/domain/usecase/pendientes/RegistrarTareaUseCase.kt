package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import ar.edu.uade.miti.domain.usecase.validar
import java.time.LocalDateTime
import javax.inject.Inject

class RegistrarTareaUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(
        titulo: String,
        responsableId: String,
        fechaHora: LocalDateTime,
        avisar: Boolean,
    ): Resource<Pendiente> {
        val hogarId = hogares.requerirHogarActivo().getOrElse { return it }
        val tarea = validar {
            Pendiente(
                id = ids.nuevoId(),
                hogarId = hogarId,
                tipo = TipoPendiente.TAREA,
                titulo = titulo.trim(),
                responsableId = responsableId,
                fechaHora = fechaHora,
                avisar = avisar,
            )
        }.getOrElse { return it }
        return pendientes.guardarPendientes(listOf(tarea)).map { tarea }
    }
}
