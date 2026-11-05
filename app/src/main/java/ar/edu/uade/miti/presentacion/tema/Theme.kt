package ar.edu.uade.miti.presentacion.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta provisoria; se reemplaza por la del Figma (miti-UI).
private val VerdeMiti = Color(0xFF2E7D5B)
private val VerdeMitiClaro = Color(0xFF8FD6B4)
private val Arena = Color(0xFF6D5E4B)
private val ArenaClara = Color(0xFFD8C4A8)

private val EsquemaClaro = lightColorScheme(
    primary = VerdeMiti,
    secondary = Arena,
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeMitiClaro,
    secondary = ArenaClara,
)

@Composable
fun MitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        content = content,
    )
}
