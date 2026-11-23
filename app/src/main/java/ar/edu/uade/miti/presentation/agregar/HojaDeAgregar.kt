package ar.edu.uade.miti.presentation.agregar

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Qué muestra la hoja: primero el menú y, después de elegir, el formulario. */
enum class PasoDeAgregar { MENU, RUTINA, TAREA }

/**
 * La hoja inferior que abre el botón + de Hoy. Compra y Gasto tienen su propia pantalla (#13 y #14), así
 * que esas opciones cierran la hoja y avisan hacia arriba para navegar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDeAgregar(
    paso: PasoDeAgregar,
    rutina: NuevaRutinaViewModel,
    tarea: NuevaTareaViewModel,
    alCambiarPaso: (PasoDeAgregar) -> Unit,
    alCerrar: () -> Unit,
    alAgregarCompra: () -> Unit,
    alAgregarGasto: () -> Unit,
) {
    // Abierta del todo: con el teclado, el formulario no queda tapado a medias.
    val estadoDeHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = estadoDeHoja,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        val contenido = Modifier.verticalScroll(rememberScrollState()).imePadding()
        when (paso) {
            PasoDeAgregar.MENU -> MenuDeAgregar(
                alElegir = { tipo ->
                    when (tipo) {
                        TipoDeItem.RUTINA -> alCambiarPaso(PasoDeAgregar.RUTINA)
                        TipoDeItem.TAREA -> alCambiarPaso(PasoDeAgregar.TAREA)
                        TipoDeItem.COMPRA -> { alCerrar(); alAgregarCompra() }
                        TipoDeItem.GASTO -> { alCerrar(); alAgregarGasto() }
                    }
                },
                modifier = contenido,
            )
            PasoDeAgregar.RUTINA -> {
                val estado by rutina.uiState.collectAsStateWithLifecycle()
                FormularioDeRutina(
                    estado = estado,
                    alCambiarNombre = rutina::cambiarNombre,
                    alElegirFrecuencia = rutina::elegirFrecuencia,
                    alAlternarDia = rutina::alternarDia,
                    alElegirVeces = rutina::elegirVeces,
                    alGuardar = rutina::guardar,
                    modifier = contenido,
                )
            }
            PasoDeAgregar.TAREA -> {
                val estado by tarea.uiState.collectAsStateWithLifecycle()
                FormularioDeTarea(
                    estado = estado,
                    alCambiarNombre = tarea::cambiarNombre,
                    alElegirResponsable = tarea::elegirResponsable,
                    alElegirFecha = tarea::elegirFecha,
                    alElegirHora = tarea::elegirHora,
                    alCambiarAviso = tarea::cambiarAviso,
                    alGuardar = tarea::guardar,
                    modifier = contenido,
                )
            }
        }
    }
}
