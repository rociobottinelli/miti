package ar.edu.uade.miti.presentation.hoy

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    alAgregar: () -> Unit,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HoyViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current

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
        alAgregar = alAgregar,
        alAbrirAjustes = alAbrirAjustes,
        alReintentar = viewModel::reintentar,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
