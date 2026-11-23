package ar.edu.uade.miti.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Paleta "Calma ordenada" (preentrega, sección 11): esquema de Material 3 generado con
 * Material Theme Builder desde el color semilla #1F6F78 (petróleo), con el terciario
 * personalizado #E07A5F (coral). Los valores claros son los de la página "High Fidelity" del Figma.
 * Ningún composable usa estos colores directamente: se leen desde MaterialTheme.colorScheme.
 */

// Tema claro
private val PrimarioClaro = Color(0xFF006972)
private val SobrePrimarioClaro = Color(0xFFFFFFFF)
private val ContenedorPrimarioClaro = Color(0xFF9DF0FB)
private val SobreContenedorPrimarioClaro = Color(0xFF004F56)
private val PrimarioInversoClaro = Color(0xFF81D3DE)
private val SecundarioClaro = Color(0xFF4A6366)
private val SobreSecundarioClaro = Color(0xFFFFFFFF)
private val ContenedorSecundarioClaro = Color(0xFFCDE7EB)
private val SobreContenedorSecundarioClaro = Color(0xFF324B4E)
private val TerciarioClaro = Color(0xFF9B432C)
private val SobreTerciarioClaro = Color(0xFFFFFFFF)
private val ContenedorTerciarioClaro = Color(0xFFFFDBD2)
private val SobreContenedorTerciarioClaro = Color(0xFF3C0800)
private val ErrorClaro = Color(0xFFBA1A1A)
private val SobreErrorClaro = Color(0xFFFFFFFF)
private val ContenedorErrorClaro = Color(0xFFFFDAD6)
private val SobreContenedorErrorClaro = Color(0xFF93000A)
private val SuperficieClaro = Color(0xFFF5FAFB)
private val SobreSuperficieClaro = Color(0xFF171D1E)
private val VarianteSuperficieClaro = Color(0xFFDAE4E6)
private val SobreVarianteSuperficieClaro = Color(0xFF3F484A)
private val SuperficieInversaClaro = Color(0xFF2B3132)
private val SobreSuperficieInversaClaro = Color(0xFFECF2F3)
private val ContornoClaro = Color(0xFF6F797A)
private val VarianteContornoClaro = Color(0xFFBEC8CA)
private val SuperficieTenueClaro = Color(0xFFD5DBDC)
private val ContenedorSuperficieMinimoClaro = Color(0xFFFFFFFF)
private val ContenedorSuperficieBajoClaro = Color(0xFFEFF5F5)
private val ContenedorSuperficieClaro = Color(0xFFE9EFF0)
private val ContenedorSuperficieAltoClaro = Color(0xFFE3E9EA)
private val ContenedorSuperficieMaximoClaro = Color(0xFFDEE4E4)

// Tema oscuro (mismo esquema de Material Theme Builder, tonos oscuros)
private val PrimarioOscuro = Color(0xFF81D3DE)
private val SobrePrimarioOscuro = Color(0xFF00363C)
private val ContenedorPrimarioOscuro = Color(0xFF004F56)
private val SobreContenedorPrimarioOscuro = Color(0xFF9DF0FB)
private val PrimarioInversoOscuro = Color(0xFF006972)
private val SecundarioOscuro = Color(0xFFB1CBCF)
private val SobreSecundarioOscuro = Color(0xFF1C3437)
private val ContenedorSecundarioOscuro = Color(0xFF324B4E)
private val SobreContenedorSecundarioOscuro = Color(0xFFCDE7EB)
private val TerciarioOscuro = Color(0xFFFFB4A1)
private val SobreTerciarioOscuro = Color(0xFF5E1704)
private val ContenedorTerciarioOscuro = Color(0xFF7C2D17)
private val SobreContenedorTerciarioOscuro = Color(0xFFFFDBD2)
private val ErrorOscuro = Color(0xFFFFB4AB)
private val SobreErrorOscuro = Color(0xFF690005)
private val ContenedorErrorOscuro = Color(0xFF93000A)
private val SobreContenedorErrorOscuro = Color(0xFFFFDAD6)
private val SuperficieOscuro = Color(0xFF0E1415)
private val SobreSuperficieOscuro = Color(0xFFDEE4E4)
private val VarianteSuperficieOscuro = Color(0xFF3F484A)
private val SobreVarianteSuperficieOscuro = Color(0xFFBEC8CA)
private val SuperficieInversaOscuro = Color(0xFFDEE4E4)
private val SobreSuperficieInversaOscuro = Color(0xFF2B3132)
private val ContornoOscuro = Color(0xFF899294)
private val VarianteContornoOscuro = Color(0xFF3F484A)
private val SuperficieBrillanteOscuro = Color(0xFF343A3B)
private val ContenedorSuperficieMinimoOscuro = Color(0xFF090F10)
private val ContenedorSuperficieBajoOscuro = Color(0xFF171D1E)
private val ContenedorSuperficieOscuro = Color(0xFF1B2122)
private val ContenedorSuperficieAltoOscuro = Color(0xFF252B2C)
private val ContenedorSuperficieMaximoOscuro = Color(0xFF303637)

internal val EsquemaClaro = lightColorScheme(
    primary = PrimarioClaro,
    onPrimary = SobrePrimarioClaro,
    primaryContainer = ContenedorPrimarioClaro,
    onPrimaryContainer = SobreContenedorPrimarioClaro,
    inversePrimary = PrimarioInversoClaro,
    secondary = SecundarioClaro,
    onSecondary = SobreSecundarioClaro,
    secondaryContainer = ContenedorSecundarioClaro,
    onSecondaryContainer = SobreContenedorSecundarioClaro,
    tertiary = TerciarioClaro,
    onTertiary = SobreTerciarioClaro,
    tertiaryContainer = ContenedorTerciarioClaro,
    onTertiaryContainer = SobreContenedorTerciarioClaro,
    error = ErrorClaro,
    onError = SobreErrorClaro,
    errorContainer = ContenedorErrorClaro,
    onErrorContainer = SobreContenedorErrorClaro,
    background = SuperficieClaro,
    onBackground = SobreSuperficieClaro,
    surface = SuperficieClaro,
    onSurface = SobreSuperficieClaro,
    surfaceVariant = VarianteSuperficieClaro,
    onSurfaceVariant = SobreVarianteSuperficieClaro,
    surfaceTint = PrimarioClaro,
    inverseSurface = SuperficieInversaClaro,
    inverseOnSurface = SobreSuperficieInversaClaro,
    outline = ContornoClaro,
    outlineVariant = VarianteContornoClaro,
    scrim = Color.Black,
    surfaceBright = SuperficieClaro,
    surfaceDim = SuperficieTenueClaro,
    surfaceContainerLowest = ContenedorSuperficieMinimoClaro,
    surfaceContainerLow = ContenedorSuperficieBajoClaro,
    surfaceContainer = ContenedorSuperficieClaro,
    surfaceContainerHigh = ContenedorSuperficieAltoClaro,
    surfaceContainerHighest = ContenedorSuperficieMaximoClaro,
)

internal val EsquemaOscuro = darkColorScheme(
    primary = PrimarioOscuro,
    onPrimary = SobrePrimarioOscuro,
    primaryContainer = ContenedorPrimarioOscuro,
    onPrimaryContainer = SobreContenedorPrimarioOscuro,
    inversePrimary = PrimarioInversoOscuro,
    secondary = SecundarioOscuro,
    onSecondary = SobreSecundarioOscuro,
    secondaryContainer = ContenedorSecundarioOscuro,
    onSecondaryContainer = SobreContenedorSecundarioOscuro,
    tertiary = TerciarioOscuro,
    onTertiary = SobreTerciarioOscuro,
    tertiaryContainer = ContenedorTerciarioOscuro,
    onTertiaryContainer = SobreContenedorTerciarioOscuro,
    error = ErrorOscuro,
    onError = SobreErrorOscuro,
    errorContainer = ContenedorErrorOscuro,
    onErrorContainer = SobreContenedorErrorOscuro,
    background = SuperficieOscuro,
    onBackground = SobreSuperficieOscuro,
    surface = SuperficieOscuro,
    onSurface = SobreSuperficieOscuro,
    surfaceVariant = VarianteSuperficieOscuro,
    onSurfaceVariant = SobreVarianteSuperficieOscuro,
    surfaceTint = PrimarioOscuro,
    inverseSurface = SuperficieInversaOscuro,
    inverseOnSurface = SobreSuperficieInversaOscuro,
    outline = ContornoOscuro,
    outlineVariant = VarianteContornoOscuro,
    scrim = Color.Black,
    surfaceBright = SuperficieBrillanteOscuro,
    surfaceDim = SuperficieOscuro,
    surfaceContainerLowest = ContenedorSuperficieMinimoOscuro,
    surfaceContainerLow = ContenedorSuperficieBajoOscuro,
    surfaceContainer = ContenedorSuperficieOscuro,
    surfaceContainerHigh = ContenedorSuperficieAltoOscuro,
    surfaceContainerHighest = ContenedorSuperficieMaximoOscuro,
)
