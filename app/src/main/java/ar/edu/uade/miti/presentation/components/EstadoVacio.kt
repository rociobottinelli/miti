package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Pantalla o sección sin datos (Figma: "Tu hogar está listo", "Todo al día por hoy",
 * "No falta nada", "Todavía no hay gastos este mes"). Es una invitación a actuar, no un error:
 * un fallo al leer datos se muestra con un aviso de error, nunca como una lista vacía.
 *
 * @param accion texto del botón opcional ("Agregar algo"); sin [alTocarAccion] no se muestra.
 */
@Composable
fun EstadoVacio(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alTocarAccion: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 156.dp, height = 136.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), CircleShape),
        ) {
            // Ilustración decorativa: el título ya dice qué pasa, TalkBack no la lee.
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accion != null && alTocarAccion != null) {
            Spacer(Modifier.height(Espaciado.entreSecciones + 12.dp))
            Button(
                onClick = alTocarAccion,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(accion)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun EstadoVacioPreview() {
    MitiTheme {
        EstadoVacio(
            titulo = "Tu hogar está listo",
            descripcion = "Agregá tu primera rutina o tarea para arrancar.",
            icono = Icons.Outlined.AddHome,
            accion = "Agregar algo",
            alTocarAccion = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 480, backgroundColor = 0xFF0E1415)
@Composable
private fun EstadoVacioSinAccionOscuroPreview() {
    MitiTheme(darkTheme = true) {
        EstadoVacio(
            titulo = "Todo al día por hoy",
            descripcion = "Todas las rutinas y tareas del día están cubiertas.",
            icono = Icons.Outlined.AddHome,
        )
    }
}
