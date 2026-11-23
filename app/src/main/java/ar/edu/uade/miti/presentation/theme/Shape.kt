package ar.edu.uade.miti.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Radios del Figma (el diseño está dibujado a 0,849 de escala; acá ya están en dp):
 *  - small: chips de filtro y checkbox de una rutina (8 dp).
 *  - medium: avisos como "Sin conexión" o "Notificaciones desactivadas" (10 dp).
 *  - large: cards de rutina y de tarea, y el botón flotante (16 dp).
 * extraSmall y extraLarge quedan como en Material 3.
 */
internal val MitiFormas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
)
