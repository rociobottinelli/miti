package ar.edu.uade.miti.dominio.casouso.gastos

import ar.edu.uade.miti.dominio.modelo.BalanceDelMes
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Reparto
import ar.edu.uade.miti.dominio.repositorio.GastoRepositorio
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepositorio
import ar.edu.uade.miti.dominio.repositorio.requerirHogarActivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Lo que completa el usuario en el formulario de gasto (sin id ni hogar, que los pone el caso de uso). */
data class DatosDeGasto(
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String? = null,
    val cuotasTotales: Int = 1,
    val cuotaInicial: Int = 1,
    /** null = el mes actual. */
    val mesInicio: YearMonth? = null,
)

class RegistrarGasto @Inject constructor(
    private val hogares: HogarRepositorio,
    private val gastos: GastoRepositorio,
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

/** Registra que [deIntegranteId] le pagó a [aIntegranteId], total o parcialmente. */
class RegistrarPago @Inject constructor(
    private val hogares: HogarRepositorio,
    private val gastos: GastoRepositorio,
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

/**
 * Balance de [mes], que se recalcula solo cada vez que cambia un gasto, un pago o un integrante (RF04).
 * Emite null mientras el hogar no tenga dos integrantes, porque no hay entre quiénes repartir.
 */
class ObtenerBalanceDelMes @Inject constructor(
    private val hogares: HogarRepositorio,
    private val gastos: GastoRepositorio,
) {
    operator fun invoke(mes: YearMonth): Flow<BalanceDelMes?> =
        combine(hogares.observarIntegrantes(), gastos.observarGastos(), gastos.observarPagos()) { integrantes, todosLosGastos, todosLosPagos ->
            if (integrantes.size < 2) return@combine null
            // Orden fijo por id para que A y B sean siempre los mismos en los dos celulares.
            val (a, b) = integrantes.map { it.id }.sorted()
            CalculadoraDeBalance.calcular(mes, a, b, todosLosGastos, todosLosPagos)
        }
}
