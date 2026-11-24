package ar.edu.uade.miti.presentation.hoy

import ar.edu.uade.miti.presentation.components.AvisoDeSync
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Todo lo que la pantalla Hoy puede mostrar (D01). Es un sealed interface y no un data class con
 * booleanos sueltos: así no puede pasar que esté "cargando" y "con error" a la vez, y un error nunca
 * se muestra como una lista vacía.
 */
sealed interface HoyUiState {

    /** Solo la primera vez: después Room responde al instante (RNF04). */
    data object Cargando : HoyUiState

    /** No se pudieron leer los datos del celular. */
    data object Error : HoyUiState

    /** El hogar no tiene ninguna rutina ni tarea todavía. */
    data class Vacio(val encabezado: Encabezado) : HoyUiState

    /** Hay ítems, pero hoy no queda nada por hacer. */
    data class TodoAlDia(val encabezado: Encabezado) : HoyUiState

    data class Contenido(
        val encabezado: Encabezado,
        val filtro: FiltroDeTareas,
        val rutinas: List<RutinaDeHoy>,
        val tareas: List<TareaDeHoy>,
        /** Datos desactualizados y la persona tocó una rutina: se le pregunta antes de marcar (C1). */
        val confirmarMarca: ConfirmacionDeMarca? = null,
    ) : HoyUiState {
        val rutinasCompletas: Int get() = rutinas.count { it.completa }
    }
}

/** Lo que va arriba en todos los estados con datos: la fecha, el avatar propio y los filtros. */
data class Encabezado(
    val fecha: LocalDate,
    /** Quien usa este celular. Null solo si todavía no se descargaron los integrantes. */
    val yo: IntegranteEnPantalla?,
    /** La otra persona del hogar, o null si todavía no se unió. Sin ella no tiene sentido filtrar. */
    val otro: IntegranteEnPantalla?,
    /** Qué tan al día está este celular; null si está todo bien (C1, #19). */
    val aviso: AvisoDeSync? = null,
)

/** [posicion] decide el color (ver `ColoresMiti`). */
data class IntegranteEnPantalla(val id: String, val nombre: String, val posicion: Int)

enum class FiltroDeTareas { TODAS, MIAS, DEL_OTRO }

data class RutinaDeHoy(
    val id: String,
    val titulo: String,
    val hechas: Int,
    val vecesPorDia: Int,
    /** La marca más reciente, para mostrar quién la hizo y cuándo (RF03). */
    val ultimaMarca: Marca?,
    /** Tiene una marca o un cambio que todavía no llegó al otro celular (RNF09). */
    val pendienteDeSync: Boolean = false,
) {
    val completa: Boolean get() = hechas >= vecesPorDia
}

/** [quien] es null si la marca es de alguien que ya no está en el hogar. */
data class Marca(val quien: String?, val hora: LocalTime)

data class TareaDeHoy(
    val id: String,
    val titulo: String,
    val cuando: CuandoVence,
    val responsable: IntegranteEnPantalla?,
    val conAviso: Boolean,
    val pendienteDeSync: Boolean = false,
)

/** "Puede que Tomás ya la haya hecho": se pide confirmar antes de marcar con datos viejos (C1). */
data class ConfirmacionDeMarca(
    val rutinaId: String,
    val titulo: String,
    /** Quien pudo haberla hecho; null si la persona está sola en el hogar. */
    val otro: String?,
    val ultimaSincronizacion: LocalDateTime?,
)

/** Mensajes que se muestran una sola vez (snackbar), no son parte del estado. */
enum class EventoDeHoy { RUTINA_YA_COMPLETA, NO_SE_PUDO_MARCAR }
