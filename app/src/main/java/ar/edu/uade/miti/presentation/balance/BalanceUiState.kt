package ar.edu.uade.miti.presentation.balance

import androidx.annotation.StringRes
import ar.edu.uade.miti.domain.model.CampoDeGasto
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import java.time.LocalDate
import java.time.YearMonth

/** Estado de la pantalla Balance (#15). Variantes excluyentes: sealed (AGENTS.md §5). */
sealed interface BalanceUiState {
    /** Solo la primera vez: después Room responde al instante. */
    data object Cargando : BalanceUiState

    /** No se pudieron leer los datos del celular. */
    data object Error : BalanceUiState

    /** El hogar todavía no tiene dos integrantes: no hay entre quiénes repartir. */
    data object SinOtraPersona : BalanceUiState

    data class Contenido(
        val mes: YearMonth,
        /** No se puede ir a meses futuros: todavía no tienen gastos. */
        val puedeAvanzar: Boolean,
        /** Quién le debe a quién este mes, o null si "están a mano". */
        val resultado: DeudaEnPantalla?,
        /** "Ana pagó $ 41.900 | Tomás pagó $ 65.200": lo que puso cada uno en las cuotas del mes. */
        val pagadoPor: List<TotalDePersona>,
        /** Lo que quedó sin saldar de los meses anteriores (preentrega: "saldo pendiente"). */
        val saldoAnterior: DeudaEnPantalla?,
        val movimientos: List<Movimiento>,
        /** Ediciones propias que no entraron por un conflicto (C2): se muestran arriba de todo. */
        val conflictos: List<ConflictoEnPantalla>,
        /** La hoja de "Registrar pago" abierta, o null. */
        val pago: PagoEnCurso? = null,
    ) : BalanceUiState
}

data class DeudaEnPantalla(
    val deudorId: String,
    val deudor: String,
    val acreedorId: String,
    val acreedor: String,
    val montoCentavos: Long,
)

data class TotalDePersona(val id: String, val nombre: String, val montoCentavos: Long)

sealed interface Movimiento {
    val id: String

    /** La parte de un gasto que cae en el mes: el gasto entero o la cuota del mes. */
    data class CuotaDeGasto(
        val gastoId: String,
        val descripcion: String,
        val montoCentavos: Long,
        val pagador: String,
        /** Null: a medias. Si no, el nombre de quien se beneficia (100/0). */
        val soloDe: String?,
        val numeroCuota: Int,
        val cuotasTotales: Int,
    ) : Movimiento {
        override val id get() = "gasto-$gastoId"
        val enCuotas get() = cuotasTotales > 1
    }

    data class PagoHecho(
        val pagoId: String,
        val de: String,
        val a: String,
        val montoCentavos: Long,
        val fecha: LocalDate,
    ) : Movimiento {
        override val id get() = "pago-$pagoId"
    }
}

data class ConflictoEnPantalla(
    val conflicto: ConflictoDeGasto,
    val descripcion: String,
    val miMontoCentavos: Long,
    /** Null si el gasto ya no está (lo borró la otra persona). */
    val montoActualCentavos: Long?,
    /** Qué campos difieren entre lo que puso esta persona y lo que quedó (#51). Vacío si ya no está. */
    val diferencias: List<DiferenciaDeGasto> = emptyList(),
)

/** Un campo del gasto con el valor que puso esta persona y el que quedó. */
data class DiferenciaDeGasto(val campo: CampoDeGasto, val mio: ValorDeCampo, val quedo: ValorDeCampo)

/** El valor de un campo, sin texto armado: la pantalla lo escribe con sus recursos. */
sealed interface ValorDeCampo {
    data class Texto(val texto: String) : ValorDeCampo
    data class Pesos(val centavos: Long) : ValorDeCampo
    data class Persona(val nombre: String) : ValorDeCampo
    data object AMedias : ValorDeCampo
    data class SoloDe(val nombre: String) : ValorDeCampo

    /** [total] 1 = sin cuotas. */
    data class Cuotas(val actual: Int, val total: Int) : ValorDeCampo
}

/** Lo que se está escribiendo en "Registrar pago". */
data class PagoEnCurso(
    val deId: String,
    val de: String,
    val aId: String,
    val a: String,
    val monto: String,
    val fecha: LocalDate,
    @param:StringRes val error: Int? = null,
    val guardando: Boolean = false,
)

sealed interface EventoDeBalance {
    data class Mensaje(@param:StringRes val texto: Int) : EventoDeBalance
    data class NoSePudo(val mensaje: String) : EventoDeBalance
}
