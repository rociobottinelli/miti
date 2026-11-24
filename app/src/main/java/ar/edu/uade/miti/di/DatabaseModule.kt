package ar.edu.uade.miti.di

import android.content.Context
import androidx.room.Room
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.migration.MIGRACIONES
import ar.edu.uade.miti.data.local.dao.ConflictoGastoDao
import ar.edu.uade.miti.data.local.dao.GastoDao
import ar.edu.uade.miti.data.local.dao.HogarDao
import ar.edu.uade.miti.data.local.dao.PendienteDao
import ar.edu.uade.miti.data.local.dao.RutinaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

// Recetas de Hilt: le dicen cómo construir lo que piden los casos de uso.
// SingletonComponent = viven mientras viva la app (una sola base de datos para todos).

/** Lo que hay que construir "a mano" (con @Provides): la base, sus DAOs y el reloj. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "miti.db")
            .addMigrations(*MIGRACIONES)
            .build()

    @Provides fun hogarDao(base: AppDatabase): HogarDao = base.hogarDao()
    @Provides fun rutinaDao(base: AppDatabase): RutinaDao = base.rutinaDao()
    @Provides fun pendienteDao(base: AppDatabase): PendienteDao = base.pendienteDao()
    @Provides fun gastoDao(base: AppDatabase): GastoDao = base.gastoDao()
    @Provides fun conflictoGastoDao(base: AppDatabase): ConflictoGastoDao = base.conflictoGastoDao()

    /** La hora real. En los tests se reemplaza por un reloj fijo. */
    @Provides
    @Singleton
    fun reloj(): Clock = Clock.systemDefaultZone()
}
