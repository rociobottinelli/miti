package ar.edu.uade.miti.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema de Miti. No usa colores dinámicos (Material You): la paleta es la del Figma para que la
 * app se vea igual en todos los celulares y la diferencia entre rutinas y tareas no dependa del
 * fondo de pantalla de cada persona.
 */
@Composable
fun MitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = MitiTipografia,
        shapes = MitiFormas,
        content = content,
    )
}
