package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.usecase.validar
import javax.inject.Inject

/**
 * Edita un gasto (RF04: "al guardar o editar un gasto, el balance se actualiza"). Conserva el id, el hogar
 * y el mes de inicio si no se indica otro. Si el otro integrante editó el mismo gasto antes de que este
 * cambio llegue al servidor, no se pierde en silencio: vuelve como conflicto (C2).
 */
class EditarGastoUseCase @Inject constructor(private val gastos: GastoRepository) {
    suspend operator fun invoke(gastoId: String, datos: DatosDeGasto): Resource<Gasto> {
        val actual = gastos.obtenerGasto(gastoId).getOrElse { return it }
            ?: return Resource.Error("El gasto ya no existe")
        val editado = validar {
            actual.copy(
                descripcion = datos.descripcion.trim(),
                montoTotalCentavos = datos.montoTotalCentavos,
                pagadorId = datos.pagadorId,
                reparto = datos.reparto,
                beneficiarioId = datos.beneficiarioId,
                cuotasTotales = datos.cuotasTotales,
                cuotaInicial = datos.cuotaInicial,
                mesInicio = datos.mesInicio ?: actual.mesInicio,
            )
        }.getOrElse { return it }
        return gastos.guardarGasto(editado).map { editado }
    }
}
