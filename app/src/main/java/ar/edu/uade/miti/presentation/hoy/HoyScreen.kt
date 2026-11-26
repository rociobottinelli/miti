package ar.edu.uade.miti.presentation.hoy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
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

    // Al volver a Hoy (por ejemplo, desde la configuración del sistema) se miran de nuevo los permisos.
    LifecycleResumeEffect(viewModel) {
        viewModel.revisarPermisos()
        onPauseOrDispose { }
    }
    val contexto = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDeHoy.NoSePudoMarcar -> snackbarHostState.showSnackbar(recursos.getString(R.string.hoy_evento_no_se_pudo_marcar))
                is EventoDeHoy.NoSePudo -> snackbarHostState.showSnackbar(evento.mensaje)
                is EventoDeHoy.Borrado -> snackbarHostState.showSnackbar(recursos.getString(R.string.hoy_borrado, evento.titulo))
                is EventoDeHoy.MarcadaPorOtro -> snackbarHostState.showSnackbar(
                    evento.quien?.let { recursos.getString(R.string.hoy_marcada_por, it) }
                        ?: recursos.getString(R.string.hoy_marcada_por_la_otra_persona),
                )
                is EventoDeHoy.TareaHecha -> {
                    val resultado = snackbarHostState.showSnackbar(
                        message = recursos.getString(R.string.hoy_tarea_hecha, evento.titulo),
                        actionLabel = recursos.getString(R.string.accion_deshacer),
                        duration = SnackbarDuration.Short,
                    )
                    if (resultado == SnackbarResult.ActionPerformed) viewModel.reabrir(evento.tareaId)
                }
            }
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
        alConfigurarAvisos = { abrirConfiguracionDeAvisos(contexto, it) },
        alCompletarTarea = viewModel::completar,
        acciones = AccionesDeItems(
            deshacerMarca = viewModel::deshacerMarca,
            borrarRutina = viewModel::borrarRutina,
            borrarTarea = viewModel::borrarTarea,
        ),
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

/**
 * Abre el ajuste del sistema que falta para los avisos (preentrega, sección 11: "con acceso a la
 * configuración"). Es navegación hacia afuera de la app, por eso vive en la pantalla y no en el ViewModel.
 */
private fun abrirConfiguracionDeAvisos(contexto: Context, falta: AvisoDePermisos) {
    val paquete = contexto.packageName
    val intencion = when {
        falta == AvisoDePermisos.SIN_ALARMAS_EXACTAS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$paquete"))
        else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, paquete)
    }
    try {
        contexto.startActivity(intencion)
    } catch (e: ActivityNotFoundException) {
        // Algunos fabricantes no tienen esa pantalla: la ficha de la app tiene los mismos ajustes.
        contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$paquete")))
    }
}
