package ar.edu.uade.miti.di

import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.datastore.HogarActivoDataStore
import ar.edu.uade.miti.data.repository.GastoRepositoryImpl
import ar.edu.uade.miti.data.repository.GeneradorDeUuid
import ar.edu.uade.miti.data.repository.HogarRepositoryImpl
import ar.edu.uade.miti.data.repository.PendienteRepositoryImpl
import ar.edu.uade.miti.data.repository.RutinaRepositoryImpl
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.RutinaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
