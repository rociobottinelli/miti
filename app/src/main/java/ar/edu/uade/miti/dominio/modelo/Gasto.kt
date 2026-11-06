package ar.edu.uade.miti.dominio.modelo

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class Reparto {
    /** 50/50 entre los dos integrantes. */
    A_MEDIAS,

    /** 100/0: el gasto es todo de [Gasto.beneficiarioId]. */
    UNO_SOLO,
}

/** Los montos se guardan en centavos para no arrastrar errores de punto flotante. */
data class Gasto(
    val id: String,
    val hogarId: String,
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String? = null,
    val cuotasTotales: Int = 1,
    /** Cuota que corresponde a [mesInicio] (permite cargar una compra en cuotas ya empezada). */
    val cuotaInicial: Int = 1,
    val mesInicio: YearMonth,
) {
    init {
        require(descripcion.isNotBlank()) { "La descripción no puede estar vacía" }
        require(montoTotalCentavos > 0) { "El monto tiene que ser mayor a cero" }
        require(cuotasTotales >= 1) { "Tiene que haber al menos una cuota" }
        require(cuotaInicial in 1..cuotasTotales) { "La cuota actual no puede ser mayor al total" }
        when (reparto) {
            Reparto.UNO_SOLO -> require(beneficiarioId != null) { "Un gasto de uno solo necesita beneficiario" }
            Reparto.A_MEDIAS -> require(beneficiarioId == null) { "Un gasto a medias no tiene beneficiario" }
        }
    }

    /**
     * Monto de la cuota [numero]. Si el total no se divide exacto, el resto se reparte de a un
     * centavo en las primeras cuotas, para que la suma de todas dé siempre el total.
     */
    fun montoDeCuota(numero: Int): Long {
        require(numero in 1..cuotasTotales) { "La cuota $numero no existe" }
        val base = montoTotalCentavos / cuotasTotales
        val resto = montoTotalCentavos % cuotasTotales
        return if (numero <= resto) base + 1 else base
    }

    /**
     * Número de cuota que cae en [mes], o null si en ese mes no se paga ninguna.
     * Las cuotas anteriores a [cuotaInicial] ya estaban pagadas antes de cargar el gasto.
     */
    fun cuotaEn(mes: YearMonth): Int? {
        val mesesDesdeInicio = ChronoUnit.MONTHS.between(mesInicio, mes).toInt()
        val numero = cuotaInicial + mesesDesdeInicio
        return numero.takeIf { mesesDesdeInicio >= 0 && it <= cuotasTotales }
    }

    /** Meses en los que se paga alguna cuota, de la [cuotaInicial] a la última. */
    fun mesesConCuota(): List<Pair<YearMonth, Int>> =
        (cuotaInicial..cuotasTotales).map { numero -> mesInicio.plusMonths((numero - cuotaInicial).toLong()) to numero }
}

/** Pago entre integrantes para saldar el balance; se descuenta del mes de su [fecha]. */
data class Pago(
    val id: String,
    val hogarId: String,
    val deIntegranteId: String,
    val aIntegranteId: String,
    val montoCentavos: Long,
    val fecha: LocalDate,
) {
    init {
        require(montoCentavos > 0) { "El monto tiene que ser mayor a cero" }
        require(deIntegranteId != aIntegranteId) { "Un pago tiene que ser entre dos integrantes distintos" }
    }
}
