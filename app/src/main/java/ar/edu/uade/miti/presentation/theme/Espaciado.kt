package ar.edu.uade.miti.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Espaciados que se repiten en las pantallas del Figma. Tenerlos en un solo lugar evita números
 * sueltos en cada composable y mantiene la grilla de 8 dp de Material.
 */
object Espaciado {
    /** Margen lateral de todas las pantallas. */
    val margenPantalla = 16.dp

    /** Separación entre cards de una lista. */
    val entreCards = 8.dp

    /** Separación entre bloques de una pantalla (por ejemplo, entre "Rutinas de hoy" y "Próximas tareas"). */
    val entreSecciones = 16.dp

    /** Relleno interno de una card o de un aviso. */
    val rellenoInterno = 12.dp

    /** Lado mínimo de cualquier elemento tocable (RNF07). */
    val areaTactilMinima = 48.dp
}
