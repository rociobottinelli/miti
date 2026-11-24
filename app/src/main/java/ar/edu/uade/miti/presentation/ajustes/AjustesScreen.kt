package ar.edu.uade.miti.presentation.ajustes

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvatarDeIntegrante
import ar.edu.uade.miti.presentation.components.EsqueletoDeCarga
import ar.edu.uade.miti.presentation.components.EstadoVacio
import ar.edu.uade.miti.presentation.components.compartirCodigoDelHogar
import ar.edu.uade.miti.presentation.components.copiarCodigoDelHogar
import ar.edu.uade.miti.presentation.theme.MitiTheme
import kotlinx.coroutines.launch

@Composable
fun AjustesScreen(alVolver: () -> Unit, modifier: Modifier = Modifier, viewModel: AjustesViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val copiado = stringResource(R.string.ajustes_codigo_copiado)

    // Las notificaciones las prende y apaga Android: se vuelve a mirar cada vez que la pantalla aparece,
    // por si la persona las cambió en la configuración del sistema.
    var notificaciones by remember { mutableStateOf(notificacionesPermitidas(contexto)) }
    LifecycleResumeEffect(Unit) {
        notificaciones = notificacionesPermitidas(contexto)
        onPauseOrDispose { }
    }

    AjustesContenido(
        estado = estado,
        notificacionesPermitidas = notificaciones,
        alVolver = alVolver,
        alCopiar = { codigo ->
            copiarCodigoDelHogar(contexto, codigo)
            alcance.launch { snackbar.showSnackbar(copiado) }
        },
        alCompartir = { compartirCodigoDelHogar(contexto, it) },
        alTocarNotificaciones = { abrirAjustesDeNotificaciones(contexto) },
        alPedirCerrarSesion = viewModel::pedirCerrarSesion,
        alConfirmarCierre = viewModel::confirmarCierre,
        alCancelarCierre = viewModel::cancelarCierre,
        snackbar = snackbar,
        modifier = modifier,
    )
}

private fun notificacionesPermitidas(contexto: Context) = NotificationManagerCompat.from(contexto).areNotificationsEnabled()

private fun abrirAjustesDeNotificaciones(contexto: Context) {
    contexto.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
    )
}

/** Figma 37:1023 (y 37:980 para el diálogo de cerrar sesión). Sin estado propio. */
@Composable
fun AjustesContenido(
    estado: AjustesUiState,
    notificacionesPermitidas: Boolean,
    alVolver: () -> Unit,
    alCopiar: (String) -> Unit,
    alCompartir: (String) -> Unit,
    alTocarNotificaciones: () -> Unit,
    alPedirCerrarSesion: () -> Unit,
    alConfirmarCierre: () -> Unit,
    alCancelarCierre: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val noSePudo = (estado as? AjustesUiState.Contenido)?.cierre as? CierreDeSesion.NoSePudo
    LaunchedEffect(noSePudo) { noSePudo?.let { snackbar.showSnackbar(it.mensaje) } }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ajustes_volver))
                }
                Text(
                    stringResource(R.string.ajustes_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            when (estado) {
                AjustesUiState.Cargando -> EsqueletoDeCarga(Modifier.padding(16.dp))
                AjustesUiState.Error -> EstadoVacio(
                    titulo = stringResource(R.string.ajustes_error),
                    descripcion = stringResource(R.string.ajustes_error_detalle),
                    icono = Icons.Outlined.ErrorOutline,
                )
                is AjustesUiState.Contenido -> Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Integrantes(estado.integrantes)
                    estado.codigo?.let { TarjetaDeInvitacion(it, alCopiar, alCompartir) }
                    FilaDeNotificaciones(notificacionesPermitidas, alTocarNotificaciones)
                    OutlinedButton(
                        onClick = alPedirCerrarSesion,
                        enabled = estado.cierre != CierreDeSesion.Cerrando,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            stringResource(R.string.ajustes_cerrar_sesion),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (estado.cierre == CierreDeSesion.Confirmando) DialogoCerrarSesion(alConfirmarCierre, alCancelarCierre)
                }
            }
        }
    }
}

@Composable
private fun Integrantes(integrantes: List<IntegranteDeAjustes>) {
    Column {
        Text(
            stringResource(R.string.ajustes_integrantes).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).semantics { heading() },
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                integrantes.forEachIndexed { i, integrante ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FilaDeIntegrante(integrante)
                }
            }
        }
    }
}

@Composable
private fun FilaDeIntegrante(integrante: IntegranteDeAjustes) {
    val vos = stringResource(R.string.ajustes_vos)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarDeIntegrante(nombre = integrante.nombre, posicion = integrante.posicion, tamanio = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(integrante.nombre, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(integrante.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (integrante.esYo) {
            Text(
                vos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** "Invitá a tu hogar": el código con Copiar y Compartir (criterio de aceptación de #21). */
@Composable
private fun TarjetaDeInvitacion(codigo: String, alCopiar: (String) -> Unit, alCompartir: (String) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val deletreado = stringResource(R.string.ajustes_codigo_descripcion, codigo.toCharArray().joinToString(" "))
    Column(
        Modifier
            .fillMaxWidth()
            .background(colores.secondaryContainer, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.GroupAdd, contentDescription = null, tint = colores.onSecondaryContainer, modifier = Modifier.size(22.dp))
            Text(
                stringResource(R.string.ajustes_invitar_titulo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colores.onSecondaryContainer,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            stringResource(R.string.ajustes_invitar_texto),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSecondaryContainer,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .background(colores.surface, RoundedCornerShape(14.dp))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                codigo,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                color = colores.primary,
                // TalkBack lo deletrea en vez de leerlo como una palabra.
                modifier = Modifier.weight(1f).semantics { contentDescription = deletreado },
            )
            TextButton(onClick = { alCopiar(codigo) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.ajustes_copiar), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
        OutlinedButton(
            onClick = { alCompartir(codigo) },
            border = BorderStroke(1.dp, colores.outline),
            modifier = Modifier.padding(top = 10.dp).heightIn(min = 48.dp),
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null, tint = colores.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.ajustes_compartir),
                color = colores.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * El interruptor muestra si Android deja que Miti notifique; tocarlo abre esa configuración del sistema,
 * porque una app no puede prenderse ni apagarse las notificaciones sola.
 */
@Composable
private fun FilaDeNotificaciones(permitidas: Boolean, alTocar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = permitidas, role = Role.Switch, onValueChange = { alTocar() })
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ajustes_notificaciones), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (permitidas) R.string.ajustes_notificaciones_detalle else R.string.ajustes_notificaciones_apagadas),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = permitidas, onCheckedChange = null)
        }
    }
}

@Composable
private fun DialogoCerrarSesion(alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
        title = { Text(stringResource(R.string.ajustes_cerrar_titulo)) },
        text = { Text(stringResource(R.string.ajustes_cerrar_texto)) },
        confirmButton = {
            TextButton(onClick = alConfirmar) {
                Text(stringResource(R.string.ajustes_cerrar_sesion), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text(stringResource(R.string.ajustes_cancelar)) } },
    )
}

private val ejemplo = AjustesUiState.Contenido(
    integrantes = listOf(
        IntegranteDeAjustes("a", "Ana", "ana@mail.com", 0, esYo = true),
        IntegranteDeAjustes("t", "Tomás", "tomas@mail.com", 1, esYo = false),
    ),
    codigo = "K7P2QX",
)

@Composable
private fun VistaPrevia(estado: AjustesUiState, notificaciones: Boolean = true, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) { AjustesContenido(estado, notificaciones, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(showSystemUi = true)
@Composable
private fun AjustesPreview() = VistaPrevia(ejemplo)

@Preview(showSystemUi = true)
@Composable
private fun AjustesOscuroPreview() = VistaPrevia(ejemplo, notificaciones = false, oscuro = true)

@Preview(name = "Cerrar sesión (37:980)", showSystemUi = true)
@Composable
private fun AjustesCerrarSesionPreview() = VistaPrevia(ejemplo.copy(cierre = CierreDeSesion.Confirmando))

@Preview(name = "Solo en el hogar", showSystemUi = true)
@Composable
private fun AjustesSoloPreview() = VistaPrevia(ejemplo.copy(integrantes = ejemplo.integrantes.take(1)))
