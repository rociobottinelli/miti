package ar.edu.uade.miti.di

import android.content.Context
import androidx.room.Room
import ar.edu.uade.miti.datos.local.GastoDao
import ar.edu.uade.miti.datos.local.HogarDao
import ar.edu.uade.miti.datos.local.AppDatabase
import ar.edu.uade.miti.datos.local.PendienteDao
import ar.edu.uade.miti.datos.local.RutinaDao
import ar.edu.uade.miti.datos.repositorio.GastoRepositoryImpl
import ar.edu.uade.miti.datos.repositorio.GeneradorDeUuid
import ar.edu.uade.miti.datos.repositorio.HogarRepositoryImpl
import ar.edu.uade.miti.datos.repositorio.PendienteRepositoryImpl
import ar.edu.uade.miti.datos.repositorio.RutinaRepositoryImpl
import ar.edu.uade.miti.datos.sesion.HogarActivo
import ar.edu.uade.miti.datos.sesion.HogarActivoDataStore
import ar.edu.uade.miti.dominio.repositorio.GastoRepository
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepository
import ar.edu.uade.miti.dominio.repositorio.PendienteRepository
import ar.edu.uade.miti.dominio.repositorio.RutinaRepository
import dagger.Binds
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
        Room.databaseBuilder(context, AppDatabase::class.java, "miti.db").build()

    @Provides fun hogarDao(base: AppDatabase): HogarDao = base.hogarDao()
    @Provides fun rutinaDao(base: AppDatabase): RutinaDao = base.rutinaDao()
    @Provides fun pendienteDao(base: AppDatabase): PendienteDao = base.pendienteDao()
    @Provides fun gastoDao(base: AppDatabase): GastoDao = base.gastoDao()

    /** La hora real. En los tests se reemplaza por un reloj fijo. */
    @Provides
    @Singleton
    fun reloj(): Clock = Clock.systemDefaultZone()
}

/** "Cuando alguien pida la interfaz X, dale la implementación Y" (con @Binds). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun hogarRepositorio(impl: HogarRepositoryImpl): HogarRepository
    @Binds abstract fun rutinaRepositorio(impl: RutinaRepositoryImpl): RutinaRepository
    @Binds abstract fun pendienteRepositorio(impl: PendienteRepositoryImpl): PendienteRepository
    @Binds abstract fun gastoRepositorio(impl: GastoRepositoryImpl): GastoRepository
    @Binds abstract fun generadorDeIds(impl: GeneradorDeUuid): GeneradorDeIds
    @Binds abstract fun hogarActivo(impl: HogarActivoDataStore): HogarActivo
}
