package ar.edu.uade.miti.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versión 1 → 2 (C2, #20): conflictos de edición en gastos.
 * - gastos.baseUpdatedAt: versión sobre la que se hizo la edición local pendiente. Null para todo lo que
 *   ya existía, que es justo lo correcto: no hay ediciones a medio subir con versión base.
 * - conflictos_gasto: ediciones propias que no entraron, para avisarlas.
 * No se borra ni se transforma ningún dato existente.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `gastos` ADD COLUMN `baseUpdatedAt` INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conflictos_gasto` (
                `id` TEXT NOT NULL, `gastoId` TEXT NOT NULL, `descripcion` TEXT NOT NULL,
                `montoTotalCentavos` INTEGER NOT NULL, `pagadorId` TEXT NOT NULL, `reparto` TEXT NOT NULL,
                `beneficiarioId` TEXT, `cuotasTotales` INTEGER NOT NULL, `cuotaInicial` INTEGER NOT NULL,
                `mesInicio` TEXT NOT NULL, `eraBorrado` INTEGER NOT NULL, `resuelto` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conflictos_gasto_gastoId` ON `conflictos_gasto` (`gastoId`)")
    }
}

/** Todas las migraciones, en orden. La base nunca se borra para "arreglar" una versión (AGENTS.md §11). */
val MIGRACIONES = arrayOf(MIGRACION_1_2)
