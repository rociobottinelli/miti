package ar.edu.uade.miti.presentation.primeruso

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Se explica para qué son los avisos antes de que aparezca el diálogo del sistema (flujo 1). Responda
 * lo que responda, la app sigue: sin permiso las tareas funcionan igual, solo no suenan (RNF06).
 * En Android 12 o anterior no hace falta pedirlo, y si ya estaba concedido tampoco: se sigue de largo.
 */
@Composable
fun PermisoDeNotificacionesScreen(alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { alTerminar() }

    LaunchedEffect(Unit) {
        if (!hacePedirPermiso(contexto)) alTerminar()
    }

    PermisoDeNotificacionesContenido(
        alPermitir = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                alTerminar()
            }
        },
        alOmitir = alTerminar,
        modifier = modifier,
    )
}

private fun hacePedirPermiso(contexto: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** Figma 37:992. */
@Composable
fun PermisoDeNotificacionesContenido(alPermitir: () -> Unit, alOmitir: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.ilustracion_notificaciones), contentDescription = null)
        Text(
            stringResource(R.string.permiso_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp).semantics { heading() },
        )
        Text(
            stringResource(R.string.permiso_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 48.dp),
        )
        Button(onClick = alPermitir, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.permiso_permitir), Modifier.padding(start = 8.dp))
        }
        TextButton(onClick = alOmitir, modifier = Modifier.padding(top = 4.dp).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.permiso_ahora_no))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PermisoPreview() {
    MitiTheme { PermisoDeNotificacionesContenido({}, {}) }
}

@Preview(showSystemUi = true)
@Composable
private fun PermisoOscuroPreview() {
    MitiTheme(darkTheme = true) { PermisoDeNotificacionesContenido({}, {}) }
}
