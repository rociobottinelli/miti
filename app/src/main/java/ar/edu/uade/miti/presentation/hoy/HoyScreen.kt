package ar.edu.uade.miti.presentation.hoy

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import ar.edu.uade.miti.presentation.agregar.PasoDeAgregar
import ar.edu.uade.miti.presentation.agregar.NuevaTareaViewModel
import ar.edu.uade.miti.presentation.agregar.NuevaRutinaViewModel
import ar.edu.uade.miti.presentation.agregar.HojaDeAgregar
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R

/**
 * Pantalla Hoy con su ViewModel (RF02, RF03). Lee el estado con `collectAsStateWithLifecycle` (deja de
 * escuchar en segundo plano) y muestra los eventos de una sola vez en un snackbar.
 */
@Composable
fun HoyScreen(
    alAgregarCompra: () -> Unit,
    alAgregarGasto: () -> Unit,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HoyViewModel = hiltViewModel(),
    nuevaRutina: NuevaRutinaViewModel = hiltViewModel(),
    nuevaTarea: NuevaTareaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current
    // null: hoja cerrada. Se guarda para que girar la pantalla no cierre un formulario a medio llenar.
    var hoja by rememberSaveable { mutableStateOf<PasoDeAgregar?>(null) }

    // Al guardar, la hoja se cierra y queda el aviso: el ítem ya aparece en la lista (RF01).
    LaunchedEffect(nuevaRutina) {
        nuevaRutina.guardada.collect { titulo ->
            hoja = null
            snackbarHostState.showSnackbar(recursos.getString(R.string.rutina_agregada, titulo))
        }
    }
    LaunchedEffect(nuevaTarea) {
        nuevaTarea.guardada.collect { titulo ->
            hoja = null
            snackbarHostState.showSnackbar(recursos.getString(R.string.tarea_agregada, titulo))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            val mensaje = when (evento) {
                EventoDeHoy.RUTINA_YA_COMPLETA -> R.string.hoy_evento_ya_completa
                EventoDeHoy.NO_SE_PUDO_MARCAR -> R.string.hoy_evento_no_se_pudo_marcar
            }
            snackbarHostState.showSnackbar(recursos.getString(mensaje))
        }
    }

    HoyContenido(
        estado = estado,
        alMarcar = viewModel::marcar,
        alElegirFiltro = viewModel::elegirFiltro,
        alAgregar = { hoja = PasoDeAgregar.MENU },
        alAbrirAjustes = alAbrirAjustes,
        alReintentar = viewModel::reintentar,
        modifier = modifier,
        alSincronizar = viewModel::sincronizar,
        alConfirmarMarca = viewModel::confirmarMarca,
        alCancelarMarca = viewModel::cancelarMarca,
        snackbarHostState = snackbarHostState,
    )

    hoja?.let { paso ->
        HojaDeAgregar(
            paso = paso,
            rutina = nuevaRutina,
            tarea = nuevaTarea,
            alCambiarPaso = { hoja = it },
            alCerrar = { hoja = null },
            alAgregarCompra = alAgregarCompra,
            alAgregarGasto = alAgregarGasto,
        )
    }
}
