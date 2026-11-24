package ar.edu.uade.miti.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.local.entity.RegistroRutinaEntity
import ar.edu.uade.miti.data.local.entity.RutinaEntity
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Reparto
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EstadoDeSyncDaoTest {

    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After fun cerrar() = base.close()

    private fun sync(estado: EstadoSync) = MetadatosSync(1, false, estado)

    @Test
    fun `junta los ids sin enviar de todas las tablas`() = runTest {
        val rutinas = base.rutinaDao()
        rutinas.guardarRutina(RutinaEntity("r-pendiente", "h1", "A", Frecuencia.DIARIA, emptySet(), 1, sync(EstadoSync.PENDIENTE)))
        rutinas.guardarRutina(RutinaEntity("r-ok", "h1", "B", Frecuencia.DIARIA, emptySet(), 1, sync(EstadoSync.SINCRONIZADO)))
        rutinas.guardarRutina(RutinaEntity("r-con-marca", "h1", "C", Frecuencia.DIARIA, emptySet(), 1, sync(EstadoSync.SINCRONIZADO)))
        rutinas.guardarRegistro(RegistroRutinaEntity("m1", "r-con-marca", "ana", LocalDateTime.of(2026, 11, 23, 8, 0), sync(EstadoSync.PENDIENTE)))
        base.gastoDao().guardarGasto(
            GastoEntity("g1", "h1", "Super", 100, "ana", Reparto.A_MEDIAS, null, 1, 1, YearMonth.of(2026, 11), sync(EstadoSync.PENDIENTE)),
        )

        val ids = base.estadoDeSyncDao().observarIdsSinEnviar().first().toSet()

        // La marca sin enviar se muestra en su rutina.
        assertEquals(setOf("r-pendiente", "r-con-marca", "g1"), ids)
    }

    @Test
    fun `sin cambios pendientes no hay ids`() = runTest {
        assertEquals(emptyList<String>(), base.estadoDeSyncDao().observarIdsSinEnviar().first())
    }
}
