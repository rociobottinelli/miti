package ar.edu.uade.miti.domain.model

/**
 * Una edición propia de un gasto que no se guardó porque la otra persona lo cambió antes (C2, #20).
 * Se muestra con los dos valores para que la persona elija: rehacer su edición o quedarse con la otra.
 */
data class ConflictoDeGasto(
    val id: String,
    /** Cómo quedó el gasto (la versión que ganó). Null si ya no está en el celular. */
    val actual: Gasto?,
    /** Cómo lo había dejado esta persona. */
    val miVersion: Gasto,
    /** La edición que no entró era borrar el gasto. */
    val eraBorrado: Boolean,
) {
    /**
     * Los campos en los que mi versión y la que quedó no coinciden, en el orden del formulario (#51). Así la
     * tarjeta dice qué se perdió aunque el monto sea el mismo. Vacío si el gasto ya no está.
     */
    val camposDistintos: List<CampoDeGasto>
        get() {
            val quedo = actual ?: return emptyList()
            return buildList {
                if (miVersion.descripcion != quedo.descripcion) add(CampoDeGasto.DESCRIPCION)
                if (miVersion.montoTotalCentavos != quedo.montoTotalCentavos) add(CampoDeGasto.MONTO)
                if (miVersion.pagadorId != quedo.pagadorId) add(CampoDeGasto.PAGADOR)
                if (miVersion.reparto != quedo.reparto || miVersion.beneficiarioId != quedo.beneficiarioId) add(CampoDeGasto.REPARTO)
                if (miVersion.cuotasTotales != quedo.cuotasTotales || miVersion.cuotaInicial != quedo.cuotaInicial) add(CampoDeGasto.CUOTAS)
            }
        }
}

/** Lo que se puede editar de un gasto, como lo agrupa el formulario. */
enum class CampoDeGasto { DESCRIPCION, MONTO, PAGADOR, REPARTO, CUOTAS }
