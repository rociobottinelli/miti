package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class RegistrarGastoUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val gastos: GastoRepository,
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(datos: DatosDeGasto): Gasto {
        val gasto = Gasto(
            id = ids.nuevoId(),
            hogarId = hogares.requerirHogarActivo(),
            descripcion = datos.descripcion.trim(),
            montoTotalCentavos = datos.montoTotalCentavos,
            pagadorId = datos.pagadorId,
            reparto = datos.reparto,
            beneficiarioId = datos.beneficiarioId,
            cuotasTotales = datos.cuotasTotales,
            cuotaInicial = datos.cuotaInicial,
            mesInicio = datos.mesInicio ?: YearMonth.now(reloj),
        )
        gastos.guardarGasto(gasto)
        return gasto
    }
}
