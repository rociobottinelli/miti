package ar.edu.uade.miti.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DineroTest {
    @Test
    fun `formatea como en Argentina, sin decimales si son pesos justos`() {
        assertEquals("$ 18.750", formatearPesos(1_875_000))
        assertEquals("$ 1.234,50", formatearPesos(123_450))
        assertEquals("$ 0", formatearPesos(0))
    }

    @Test
    fun `lee lo que escribe la persona`() {
        assertEquals(1_875_000L, leerMontoEnCentavos("18750"))
        assertEquals(1_875_000L, leerMontoEnCentavos("18.750"))
        assertEquals(1_875_050L, leerMontoEnCentavos("18.750,5"))
        assertEquals(1_875_055L, leerMontoEnCentavos("$ 18750,55"))
    }

    @Test
    fun `lo que no es un monto da null`() {
        assertNull(leerMontoEnCentavos(""))
        assertNull(leerMontoEnCentavos("abc"))
        assertNull(leerMontoEnCentavos("10,555"))
        assertNull(leerMontoEnCentavos("1,2,3"))
    }

    @Test
    fun `el monto editable vuelve a leerse igual`() {
        listOf(1_875_000L, 123_450L, 5L).forEach { assertEquals(it, leerMontoEnCentavos(montoEditable(it))) }
    }
}
