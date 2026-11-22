package ar.edu.uade.miti.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

/*
 * El Figma usa Roboto con la escala tipográfica estándar de Material 3. Equivalencias:
 *  - Fecha de Hoy ("1 de octubre") ............ titleLarge (22 sp)
 *  - Título de una card ("Pagar ABL") ......... titleSmall (14 sp, medium)
 *  - Detalle de una card ("Jueves 18:00") ..... bodySmall (12 sp)
 *  - Encabezado de sección ("RUTINAS DE HOY") . labelMedium (12 sp, en mayúsculas)
 *  - Día de la semana ("MIÉRCOLES") ........... labelSmall (11 sp, en mayúsculas)
 * Solo se ajusta el espaciado entre letras de los rótulos en mayúsculas, que en el Figma es más
 * abierto que el de Material para que se lean bien.
 */
private val Base = Typography()

internal val MitiTipografia = Base.copy(
    labelMedium = Base.labelMedium.copy(letterSpacing = 0.8.sp),
    labelSmall = Base.labelSmall.copy(letterSpacing = 1.sp),
)
