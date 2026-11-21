package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import javax.inject.Inject

class RegistrarCompraUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String): Pendiente {
        val compra = Pendiente(ids.nuevoId(), hogares.requerirHogarActivo(), TipoPendiente.COMPRA, titulo.trim())
        pendientes.guardarPendientes(listOf(compra))
        return compra
    }
}
