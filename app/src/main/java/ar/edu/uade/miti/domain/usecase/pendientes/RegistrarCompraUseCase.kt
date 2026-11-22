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
import javax.inject.Inject

class RegistrarCompraUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String): Resource<Pendiente> {
        val hogarId = hogares.requerirHogarActivo().getOrElse { return it }
        val compra = validar { Pendiente(ids.nuevoId(), hogarId, TipoPendiente.COMPRA, titulo.trim()) }
            .getOrElse { return it }
        return pendientes.guardarPendientes(listOf(compra)).map { compra }
    }
}
