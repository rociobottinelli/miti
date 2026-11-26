package ar.edu.uade.miti.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Fondo y texto del avatar de un integrante (la inicial en un círculo). */
@Immutable
data class ColorDeIntegrante(val fondo: Color, val contenido: Color)

/**
 * Colores que Material 3 no tiene como rol: en el Figma cada integrante del hogar tiene su color
 * de avatar (Ana en rosa, Tomás en verde), y se repite en Hoy, Ajustes y Balance. El hogar tiene
 * como máximo dos integrantes (preentrega, sección 8), así que alcanza con dos pares.
 */
@Immutable
data class ColoresMiti(val integrantes: List<ColorDeIntegrante>) {
    /** Color del integrante según su posición en el hogar (0 o 1). */
    fun deIntegrante(posicion: Int): ColorDeIntegrante = integrantes[posicion.mod(integrantes.size)]
}

internal val ColoresMitiClaro = ColoresMiti(
    integrantes = listOf(
        ColorDeIntegrante(fondo = Color(0xFFFFDAD6), contenido = Color(0xFF4B0056)),
        ColorDeIntegrante(fondo = Color(0xFFCCFB9D), contenido = Color(0xFF004F56)),
    ),
)

internal val ColoresMitiOscuro = ColoresMiti(
    integrantes = listOf(
        ColorDeIntegrante(fondo = Color(0xFF673B37), contenido = Color(0xFFFFD6FC)),
        ColorDeIntegrante(fondo = Color(0xFF2B5003), contenido = Color(0xFFAFEDF5)),
    ),
)

internal val LocalColoresMiti = staticCompositionLocalOf { ColoresMitiClaro }

/** Acceso a los colores propios de Miti, igual que `MaterialTheme.colorScheme`. */
object MitiTema {
    val colores: ColoresMiti
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresMiti.current
}
