package ar.edu.uade.miti.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import ar.edu.uade.miti.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Las migraciones se prueban con una base real de la versión anterior: se crea con el esquema exportado
 * (app/schemas), se cargan datos, se migra y se verifica que no se perdió nada (AGENTS.md §11 y §14).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigracionesTest {

    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val nombre = "migracion-test.db"

    @Test
    fun `de 1 a 2 conserva los gastos y agrega la version base vacia`() {
        ayudante.createDatabase(nombre, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO gastos (id, hogarId, descripcion, montoTotalCentavos, pagadorId, reparto, beneficiarioId,
                    cuotasTotales, cuotaInicial, mesInicio, updatedAt, deleted, syncState)
                VALUES ('g1', 'h1', 'Heladera', 3000000, 'ana', 'A_MEDIAS', NULL, 3, 1, '2026-11', 100, 0, 'PENDIENTE')
                """.trimIndent(),
            )
        }

        ayudante.runMigrationsAndValidate(nombre, 2, true, MIGRACION_1_2).use { db ->
            db.query("SELECT descripcion, montoTotalCentavos, syncState, baseUpdatedAt FROM gastos WHERE id = 'g1'").use { c ->
                c.moveToFirst()
                assertEquals("Heladera", c.getString(0))
                assertEquals(3_000_000L, c.getLong(1))
                assertEquals("PENDIENTE", c.getString(2))
                assertNull(c.getString(3))
            }
            db.query("SELECT COUNT(*) FROM conflictos_gasto").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test
    fun `la base migrada abre con Room y todas las migraciones`() {
        ayudante.createDatabase(nombre, 1).close()
        Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, nombre)
            .addMigrations(*MIGRACIONES)
            .allowMainThreadQueries()
            .build()
            .apply { openHelper.writableDatabase.close() }
            .close()
    }
}
