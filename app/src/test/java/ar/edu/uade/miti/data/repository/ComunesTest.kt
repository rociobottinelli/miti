package ar.edu.uade.miti.data.repository

import android.database.sqlite.SQLiteFullException
import ar.edu.uade.miti.core.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComunesTest {

    @Test
    fun `si la operacion sale bien devuelve Success con el dato`() = runTest {
        assertEquals(Resource.Success(42), enBase(NO_SE_LEYO) { 42 })
    }

    @Test
    fun `un error de SQLite se convierte en Error con un mensaje para la persona`() = runTest {
        val resultado = enBase(NO_SE_GUARDO) { throw SQLiteFullException("disco lleno") }

        assertEquals(NO_SE_GUARDO, (resultado as Resource.Error).message)
        assertTrue(resultado.cause is SQLiteFullException)
    }

    @Test(expected = CancellationException::class)
    fun `la cancelacion de la corrutina no se traga`() = runTest {
        enBase(NO_SE_GUARDO) { throw CancellationException("pantalla cerrada") }
    }
}
