package ar.edu.uade.miti.presentation.gastos

import androidx.annotation.StringRes

/**
 * El formulario de gasto (#14). Todo coexiste (lo que se escribe, los errores de cada campo, si se está
 * guardando), por eso es una data class y no un sealed (AGENTS.md §5). Los números se guardan como texto,
 * tal como los escribe la persona: se validan al guardar.
 */
data class FormularioDeGastoUiState(
    val cargando: Boolean = true,
    val editando: Boolean = false,
    /** No se pudo cargar el gasto a editar (por ejemplo, la otra persona lo borró). */
    @param:StringRes val errorDeCarga: Int? = null,
    val integrantes: List<Persona> = emptyList(),
    val monto: String = "",
    val descripcion: String = "",
    val pagadorId: String? = null,
    /** Null: a medias. Si no, el gasto es todo de esta persona (100/0). */
    val beneficiarioId: String? = null,
    val enCuotas: Boolean = false,
    val cuotasTotales: String = "3",
    val cuotaActual: String = "1",
    val errores: ErroresDeGasto = ErroresDeGasto(),
    val guardando: Boolean = false,
)

data class Persona(val id: String, val nombre: String)

/** Errores al lado de cada campo (no un diálogo). */
data class ErroresDeGasto(
    @param:StringRes val monto: Int? = null,
    @param:StringRes val descripcion: Int? = null,
    @param:StringRes val cuotas: Int? = null,
) {
    val hayErrores: Boolean get() = monto != null || descripcion != null || cuotas != null
}

/** Lo que pasa una sola vez: volver a Balance o avisar un error que no es de un campo. */
sealed interface EventoDeFormulario {
    data object Guardado : EventoDeFormulario
    data object Borrado : EventoDeFormulario
    data class NoSePudo(val mensaje: String) : EventoDeFormulario
}
