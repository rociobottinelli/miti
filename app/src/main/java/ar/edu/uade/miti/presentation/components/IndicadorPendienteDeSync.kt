package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Marca que se muestra en un ítem guardado en el celular que todavía no llegó al servidor
 * (`syncState` pendiente, preentrega sección 14). Le avisa a la persona que el otro integrante
 * todavía no lo ve, sin alarmarla: el cambio no se perdió.
 *
 * @param conTexto si es true muestra "Sin sincronizar" junto al ícono; si es false, solo el ícono
 * (para cards angostas). En los dos casos TalkBack lee la descripción completa.
 */
@Composable
fun IndicadorPendienteDeSync(modifier: Modifier = Modifier, conTexto: Boolean = false) {
    val descripcion = stringResource(R.string.pendiente_de_sincronizar)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = descripcion },
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudUpload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        if (conTexto) {
            Text(
                text = stringResource(R.string.sin_sincronizar),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IndicadorPendienteDeSyncPreview() {
    MitiTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            IndicadorPendienteDeSync()
            IndicadorPendienteDeSync(conTexto = true)
        }
    }
}
