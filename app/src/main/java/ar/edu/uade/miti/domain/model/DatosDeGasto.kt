package ar.edu.uade.miti.domain.model

import java.time.YearMonth

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
