package ar.edu.uade.miti.presentation.hoy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ar.edu.uade.miti.presentation.components.PantallaEnConstruccion

@Composable
fun HoyScreen(modifier: Modifier = Modifier) {
    PantallaEnConstruccion(
        titulo = "Hoy",
        descripcion = "Rutinas de hoy y próximas tareas (RF02).",
        modifier = modifier,
    )
}
