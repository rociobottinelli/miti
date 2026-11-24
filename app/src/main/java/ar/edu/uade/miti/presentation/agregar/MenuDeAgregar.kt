package ar.edu.uade.miti.presentation.agregar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme

/** Lo que se puede agregar desde el botón + de Hoy (RF01). */
enum class TipoDeItem { RUTINA, TAREA, COMPRA, GASTO }

/**
 * Contenido de la hoja "Agregar" (Figma 37:2494). Un toque elige el tipo; con el formulario y "Guardar"
 * son 3 toques más el texto (RNF03).
 */
@Composable
fun MenuDeAgregar(alElegir: (TipoDeItem) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.agregar_pregunta),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
        )
        OpcionDeAgregar(Icons.Outlined.Repeat, R.string.agregar_rutina, R.string.agregar_rutina_detalle) { alElegir(TipoDeItem.RUTINA) }
        OpcionDeAgregar(Icons.Outlined.Event, R.string.agregar_tarea, R.string.agregar_tarea_detalle) { alElegir(TipoDeItem.TAREA) }
        OpcionDeAgregar(Icons.Outlined.ShoppingCart, R.string.agregar_compra, R.string.agregar_compra_detalle) { alElegir(TipoDeItem.COMPRA) }
        OpcionDeAgregar(Icons.Outlined.AccountBalanceWallet, R.string.agregar_gasto, R.string.agregar_gasto_detalle) { alElegir(TipoDeItem.GASTO) }
    }
}

@Composable
private fun OpcionDeAgregar(icono: ImageVector, titulo: Int, detalle: Int, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(detalle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun MenuDeAgregarPreview() {
    MitiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) { MenuDeAgregar(alElegir = {}) }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun MenuDeAgregarOscuroPreview() {
    MitiTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) { MenuDeAgregar(alElegir = {}) }
    }
}
