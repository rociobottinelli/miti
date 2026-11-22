package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import ar.edu.uade.miti.domain.usecase.validar
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/** Crea un gasto en el hogar activo. Si los datos no son válidos, devuelve el error sin guardar nada. */
class RegistrarGastoUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val gastos: GastoRepository,
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(datos: DatosDeGasto): Resource<Gasto> {
        val hogarId = hogares.requerirHogarActivo().getOrElse { return it }
        val gasto = validar {
            Gasto(
                id = ids.nuevoId(),
                hogarId = hogarId,
                descripcion = datos.descripcion.trim(),
                montoTotalCentavos = datos.montoTotalCentavos,
                pagadorId = datos.pagadorId,
                reparto = datos.reparto,
                beneficiarioId = datos.beneficiarioId,
                cuotasTotales = datos.cuotasTotales,
                cuotaInicial = datos.cuotaInicial,
                mesInicio = datos.mesInicio ?: YearMonth.now(reloj),
            )
        }.getOrElse { return it }
        return gastos.guardarGasto(gasto).map { gasto }
    }
}
