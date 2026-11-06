package ar.edu.uade.miti.di

import android.content.Context
import androidx.room.Room
import ar.edu.uade.miti.datos.local.GastoDao
import ar.edu.uade.miti.datos.local.HogarDao
import ar.edu.uade.miti.datos.local.MitiDatabase
import ar.edu.uade.miti.datos.local.PendienteDao
import ar.edu.uade.miti.datos.local.RutinaDao
import ar.edu.uade.miti.datos.repositorio.GastoRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.GeneradorDeUuid
import ar.edu.uade.miti.datos.repositorio.HogarRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.PendienteRepositorioRoom
import ar.edu.uade.miti.datos.repositorio.RutinaRepositorioRoom
import ar.edu.uade.miti.datos.sesion.HogarActivo
import ar.edu.uade.miti.datos.sesion.HogarActivoDataStore
import ar.edu.uade.miti.dominio.repositorio.GastoRepositorio
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepositorio
import ar.edu.uade.miti.dominio.repositorio.PendienteRepositorio
import ar.edu.uade.miti.dominio.repositorio.RutinaRepositorio
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
object BaseDeDatosModule {

    @Provides
    @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): MitiDatabase =
        Room.databaseBuilder(context, MitiDatabase::class.java, "miti.db").build()

    @Provides fun hogarDao(base: MitiDatabase): HogarDao = base.hogarDao()
    @Provides fun rutinaDao(base: MitiDatabase): RutinaDao = base.rutinaDao()
    @Provides fun pendienteDao(base: MitiDatabase): PendienteDao = base.pendienteDao()
    @Provides fun gastoDao(base: MitiDatabase): GastoDao = base.gastoDao()

    /** La hora real. En los tests se reemplaza por un reloj fijo. */
    @Provides
    @Singleton
    fun reloj(): Clock = Clock.systemDefaultZone()
}

/** "Cuando alguien pida la interfaz X, dale la implementación Y" (con @Binds). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds abstract fun hogarRepositorio(impl: HogarRepositorioRoom): HogarRepositorio
    @Binds abstract fun rutinaRepositorio(impl: RutinaRepositorioRoom): RutinaRepositorio
    @Binds abstract fun pendienteRepositorio(impl: PendienteRepositorioRoom): PendienteRepositorio
    @Binds abstract fun gastoRepositorio(impl: GastoRepositorioRoom): GastoRepositorio
    @Binds abstract fun generadorDeIds(impl: GeneradorDeUuid): GeneradorDeIds
    @Binds abstract fun hogarActivo(impl: HogarActivoDataStore): HogarActivo
}
