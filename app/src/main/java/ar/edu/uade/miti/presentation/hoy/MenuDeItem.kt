package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ar.edu.uade.miti.R

/** Una opción del menú de una tarjeta. Si [confirmar] no es null, antes de hacerla se pregunta. */
data class OpcionDeMenu(val texto: String, val confirmar: Confirmacion? = null, val alElegir: () -> Unit)

data class Confirmacion(val titulo: String, val texto: String, val boton: String)

/**
 * El botón "⋮" de una tarjeta de Hoy, con sus opciones (deshacer una marca, borrar). Es un botón visible y de
 * 48 dp, no un toque largo, para que se encuentre y TalkBack lo lea ("Más opciones de Regar las plantas").
 */
@Composable
fun MenuDeItem(titulo: String, opciones: List<OpcionDeMenu>, modifier: Modifier = Modifier) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    var porConfirmar by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(modifier) {
        IconButton(onClick = { abierto = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.hoy_mas_opciones, titulo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { indice, opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.texto) },
                    onClick = {
                        abierto = false
                        if (opcion.confirmar == null) opcion.alElegir() else porConfirmar = indice
                    },
                )
            }
        }
    }

    porConfirmar?.let { opciones.getOrNull(it) }?.let { opcion ->
        val confirmacion = requireNotNull(opcion.confirmar)
        AlertDialog(
            onDismissRequest = { porConfirmar = null },
            title = { Text(confirmacion.titulo) },
            text = { Text(confirmacion.texto) },
            confirmButton = {
                TextButton(onClick = { porConfirmar = null; opcion.alElegir() }) { Text(confirmacion.boton) }
            },
            dismissButton = {
                TextButton(onClick = { porConfirmar = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}
