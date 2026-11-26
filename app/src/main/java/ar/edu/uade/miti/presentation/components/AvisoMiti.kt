package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme

/** Qué tan importante es un aviso; define sus colores. */
enum class TipoDeAviso {
    /** Estado que no requiere acción, como estar sin conexión. */
    INFORMATIVO,

    /** Algo que conviene resolver, como tener las notificaciones desactivadas. */
    ADVERTENCIA,

    /** Algo que falló, como una sincronización. */
    ERROR,
}

/**
 * Franja de aviso que va arriba del contenido de una pantalla (Figma: "Sin conexión",
 * "Notificaciones desactivadas"). El aviso no tapa la lista: los datos locales siguen visibles
 * (preentrega, sección 14), y TalkBack lo anuncia cuando aparece.
 *
 * @param accion texto del botón opcional ("Reintentar", "Abrir configuración").
 * @param alTocarAccion qué hacer al tocar el botón; si es null, no se muestra el botón.
 */
@Composable
fun AvisoMiti(
    texto: String,
    icono: ImageVector,
    tipo: TipoDeAviso,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alTocarAccion: (() -> Unit)? = null,
) {
    val colores = coloresDeAviso(tipo)
    Surface(
        color = colores.fondo,
        contentColor = colores.contenido,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .heightIn(min = Espaciado.areaTactilMinima)
                .padding(start = Espaciado.rellenoInterno, end = 4.dp),
        ) {
            // El ícono acompaña al texto, que ya dice lo mismo: TalkBack no lo lee aparte.
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
            )
            if (accion != null && alTocarAccion != null) {
                TextButton(onClick = alTocarAccion) {
                    Text(text = accion, color = colores.accion, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private data class ColoresDeAviso(val fondo: Color, val contenido: Color, val accion: Color)

@Composable
@ReadOnlyComposable
private fun coloresDeAviso(tipo: TipoDeAviso): ColoresDeAviso {
    val esquema = MaterialTheme.colorScheme
    return when (tipo) {
        TipoDeAviso.INFORMATIVO ->
            ColoresDeAviso(esquema.surfaceContainerHigh, esquema.onSurfaceVariant, esquema.primary)
        TipoDeAviso.ADVERTENCIA ->
            ColoresDeAviso(esquema.tertiaryContainer, esquema.onTertiaryContainer, esquema.tertiary)
        TipoDeAviso.ERROR ->
            ColoresDeAviso(esquema.errorContainer, esquema.onErrorContainer, esquema.error)
    }
}

@Preview(showBackground = true)
@Composable
private fun AvisoMitiPreview() {
    MitiTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            AvisoMiti(texto = "Sin conexión", icono = Icons.Outlined.CloudOff, tipo = TipoDeAviso.INFORMATIVO)
            AvisoMiti(
                texto = "Notificaciones desactivadas",
                icono = Icons.Outlined.NotificationsOff,
                tipo = TipoDeAviso.ADVERTENCIA,
                accion = "Abrir configuración",
                alTocarAccion = {},
            )
        }
    }
}
