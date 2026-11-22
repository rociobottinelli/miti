package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.domain.model.Pago
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Registra que [deIntegranteId] le pagó a [aIntegranteId], total o parcialmente. */
class RegistrarPagoUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val gastos: GastoRepository,
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(
        deIntegranteId: String,
        aIntegranteId: String,
        montoCentavos: Long,
        fecha: LocalDate = LocalDate.now(reloj),
    ): Pago {
        val pago = Pago(ids.nuevoId(), hogares.requerirHogarActivo(), deIntegranteId, aIntegranteId, montoCentavos, fecha)
        gastos.guardarPago(pago)
        return pago
    }
}
