package ar.edu.uade.miti.presentation.components

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Montos como los escribe y los lee una persona en Argentina: "$ 18.750", "$ 1.234,50".
// En el dominio todo está en centavos (Long), para no arrastrar errores de punto flotante.

private val simbolos = DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))
private val sinDecimales = DecimalFormat("#,##0", simbolos)
private val conDecimales = DecimalFormat("#,##0.00", simbolos)

/**
 * "$ 18.750" si son pesos justos, "$ 18.750,50" si hay centavos. Entre "$" y el número va un espacio que no
 * corta renglón: con la fuente grande (RNF08) "Tomás le debe $" quedaba en un renglón y "74.500" en otro.
 */
fun formatearPesos(centavos: Long): String {
    val pesos = BigDecimal.valueOf(centavos, 2)
    return "$\u00A0" + if (centavos % 100 == 0L) sinDecimales.format(pesos) else conDecimales.format(pesos)
}

/** El número para un campo editable, sin "$": "18750" o "18750,50". */
fun montoEditable(centavos: Long): String {
    val pesos = BigDecimal.valueOf(centavos, 2)
    return if (centavos % 100 == 0L) pesos.toBigInteger().toString() else pesos.toPlainString().replace('.', ',')
}

/**
 * Lee lo que escribió la persona: "18750", "18.750", "18.750,5" o "18750,50". Los puntos son de miles y la
 * coma separa los centavos. Devuelve null si no es un monto válido (vacío, letras, más de dos decimales).
 */
fun leerMontoEnCentavos(texto: String): Long? {
    val limpio = texto.trim().replace("$", "").replace(" ", "")
    if (limpio.isEmpty() || !Regex("^[0-9.]+(,[0-9]{1,2})?$").matches(limpio)) return null
    val numero = limpio.replace(".", "").replace(',', '.')
    return try {
        BigDecimal(numero).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }
}
