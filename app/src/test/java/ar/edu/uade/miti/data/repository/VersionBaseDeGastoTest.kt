package ar.edu.uade.miti.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.entity.ConflictoGastoEntity
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.data.remote.FakeHogarActivo
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La versión base que acompaña a cada edición de gasto (C2, #20). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VersionBaseDeGastoTest {

    private val base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val reloj = relojEn(LocalDateTime.of(2026, 11, 23, 12, 0))
    private val repo = GastoRepositoryImpl(base.gastoDao(), FakeHogarActivo("h1"), reloj, {}, base.conflictoGastoDao())
    private val heladera = Gasto("g1", "h1", "Heladera", 3_000_000, "ana", Reparto.A_MEDIAS, mesInicio = YearMonth.of(2026, 11))

    @After fun cerrar() = base.close()

    private suspend fun guardadoEnServidor(updatedAt: Long) =
        base.gastoDao().guardarGasto(
            heladera.toEntity(MetadatosSync(updatedAt, false, EstadoSync.SINCRONIZADO)),
        )

    @Test
    fun `un gasto nuevo no tiene version base`() = runTest {
        repo.guardarGasto(heladera)
        assertNull(base.gastoDao().gastosParaSync("g1")!!.baseUpdatedAt)
    }

    @Test
    fun `editar un gasto sincronizado guarda la version sobre la que se edito`() = runTest {
        guardadoEnServidor(updatedAt = 500)

        repo.guardarGasto(heladera.copy(montoTotalCentavos = 3_100_000))

        val local = base.gastoDao().gastosParaSync("g1")!!
        assertEquals(500L, local.baseUpdatedAt)
        assertEquals(EstadoSync.PENDIENTE, local.sync.syncState)
    }

    @Test
    fun `una segunda edicion sin subir mantiene la base de la primera`() = runTest {
        guardadoEnServidor(updatedAt = 500)
        repo.guardarGasto(heladera.copy(montoTotalCentavos = 3_100_000))

        repo.guardarGasto(heladera.copy(montoTotalCentavos = 3_200_000))

        assertEquals(500L, base.gastoDao().gastosParaSync("g1")!!.baseUpdatedAt)
    }

    @Test
    fun `borrar tambien lleva la version base`() = runTest {
        guardadoEnServidor(updatedAt = 500)

        repo.eliminarGasto("g1")

        val local = base.gastoDao().gastosParaSync("g1")!!
        assertTrue(local.sync.deleted)
        assertEquals(500L, local.baseUpdatedAt)
        assertNull(repo.obtenerGasto("g1").let { (it as ar.edu.uade.miti.core.Resource.Success).data })
    }

    @Test
    fun `los conflictos abiertos llegan con las dos versiones y se cierran al resolverlos`() = runTest {
        guardadoEnServidor(updatedAt = 700)
        base.conflictoGastoDao().guardarConflictosDescargados(
            listOf(
                ConflictoGastoEntity(
                    "c1", "g1", "Heladera", 2_900_000, "ana", Reparto.A_MEDIAS, null, 1, 1, YearMonth.of(2026, 11),
                    eraBorrado = false, sync = MetadatosSync(800, false, EstadoSync.SINCRONIZADO),
                ),
            ),
        )

        val conflicto = repo.observarConflictos().first().single()
        assertEquals(3_000_000L, conflicto.actual!!.montoTotalCentavos)
        assertEquals(2_900_000L, conflicto.miVersion.montoTotalCentavos)

        repo.resolverConflicto("c1")
        assertTrue(repo.observarConflictos().first().isEmpty())
    }
}
