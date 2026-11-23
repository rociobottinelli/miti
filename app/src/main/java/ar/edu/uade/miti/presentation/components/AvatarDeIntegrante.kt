package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.presentation.theme.MitiTema
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Inicial de un integrante en un círculo con su color (Hoy, Ajustes, Balance).
 *
 * @param nombre nombre del integrante; se muestra su inicial y TalkBack lee el nombre completo.
 * @param posicion posición del integrante en el hogar (0 o 1), que define su color.
 */
@Composable
fun AvatarDeIntegrante(
    nombre: String,
    posicion: Int,
    modifier: Modifier = Modifier,
    tamanio: Dp = 32.dp,
) {
    val colores = MitiTema.colores.deIntegrante(posicion)
    Box(
        modifier = modifier
            .size(tamanio)
            .background(colores.fondo, CircleShape)
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nombre.take(1).uppercase(),
            color = colores.contenido,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarDeIntegrantePreview() {
    MitiTheme {
        Row(modifier = Modifier.padding(8.dp)) {
            AvatarDeIntegrante(nombre = "Ana", posicion = 0, tamanio = 44.dp)
            AvatarDeIntegrante(nombre = "Tomás", posicion = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
private fun AvatarDeIntegranteOscuroPreview() {
    MitiTheme(darkTheme = true) {
        Row(modifier = Modifier.padding(8.dp)) {
            AvatarDeIntegrante(nombre = "Ana", posicion = 0)
            AvatarDeIntegrante(nombre = "Tomás", posicion = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
