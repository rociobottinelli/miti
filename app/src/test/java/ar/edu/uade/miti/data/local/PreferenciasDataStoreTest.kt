package ar.edu.uade.miti.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.data.local.datastore.HogarActivoDataStore
import ar.edu.uade.miti.data.local.datastore.PreferenciasDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferenciasDataStoreTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `el onboarding arranca sin ver y queda visto`() = runTest {
        val preferencias = PreferenciasDataStore(contexto)
        assertFalse(preferencias.onboardingVisto.first())

        preferencias.marcarOnboardingVisto()

        assertTrue(PreferenciasDataStore(contexto).onboardingVisto.first())
    }

    @Test
    fun `comparte el archivo con el hogar activo sin pisarlo`() = runTest {
        val hogarActivo = HogarActivoDataStore(contexto)
        hogarActivo.establecer("h1")

        PreferenciasDataStore(contexto).marcarOnboardingVisto()

        assertEquals("h1", hogarActivo.id.first())
    }
}
