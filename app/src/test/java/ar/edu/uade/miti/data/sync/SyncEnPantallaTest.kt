package ar.edu.uade.miti.data.sync

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncEnPantallaTest {

    @Test
    fun `sincroniza al entrar y despues cada 15 segundos`() = runTest {
        var veces = 0
        val ciclo = launch { repetirCada(SYNC_EN_PANTALLA) { veces++ } }

        runCurrent()
        assertEquals("al volver a la pantalla sincroniza enseguida", 1, veces)

        advanceTimeBy(45.seconds)
        runCurrent()
        assertEquals(4, veces)
        ciclo.cancel()
    }

    @Test
    fun `al salir de la pantalla deja de sincronizar`() = runTest {
        var veces = 0
        val ciclo = launch { repetirCada(SYNC_EN_PANTALLA) { veces++ } }
        runCurrent()

        ciclo.cancel()
        advanceTimeBy(60.seconds)
        runCurrent()

        assertEquals(1, veces)
    }
}
