package ar.edu.uade.miti.di

import ar.edu.uade.miti.data.local.BorradorDeDatosLocales
import ar.edu.uade.miti.data.local.BorradorDeDatosLocalesRoom
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncDataStore
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.datastore.HogarActivoDataStore
import ar.edu.uade.miti.data.local.datastore.PreferenciasDataStore
import ar.edu.uade.miti.data.local.datastore.PreferenciasLocales
import ar.edu.uade.miti.data.local.datastore.SesionLocal
import ar.edu.uade.miti.data.local.datastore.SesionLocalDataStore
import ar.edu.uade.miti.data.repository.SesionRepositoryImpl
import ar.edu.uade.miti.data.avisos.AlarmasDeTareasAndroid
import ar.edu.uade.miti.data.avisos.AvisoDeCambiosLocales
import ar.edu.uade.miti.data.avisos.AvisoDeTareasNuevas
import ar.edu.uade.miti.data.avisos.AvisoDeTareasNuevasAndroid
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.data.repository.GastoRepositoryImpl
import ar.edu.uade.miti.data.repository.GeneradorDeUuid
import ar.edu.uade.miti.data.repository.HogarRepositoryImpl
import ar.edu.uade.miti.data.repository.IngresoAHogarRepositoryImpl
import ar.edu.uade.miti.data.repository.OnboardingRepositoryImpl
import ar.edu.uade.miti.data.repository.PendienteRepositoryImpl
import ar.edu.uade.miti.data.repository.RutinaRepositoryImpl
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import ar.edu.uade.miti.domain.repository.OnboardingRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.RutinaRepository
import ar.edu.uade.miti.domain.repository.SesionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** "Cuando alguien pida la interfaz X, dale la implementación Y" (con @Binds). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun hogarRepositorio(impl: HogarRepositoryImpl): HogarRepository
    @Binds abstract fun ingresoAHogar(impl: IngresoAHogarRepositoryImpl): IngresoAHogarRepository
    @Binds abstract fun rutinaRepositorio(impl: RutinaRepositoryImpl): RutinaRepository
    @Binds abstract fun pendienteRepositorio(impl: PendienteRepositoryImpl): PendienteRepository
    @Binds abstract fun gastoRepositorio(impl: GastoRepositoryImpl): GastoRepository
    @Binds abstract fun generadorDeIds(impl: GeneradorDeUuid): GeneradorDeIds
    @Binds abstract fun hogarActivo(impl: HogarActivoDataStore): HogarActivo
    @Binds abstract fun sesionRepositorio(impl: SesionRepositoryImpl): SesionRepository
    @Binds abstract fun sesionLocal(impl: SesionLocalDataStore): SesionLocal
    @Binds abstract fun borradorDeDatosLocales(impl: BorradorDeDatosLocalesRoom): BorradorDeDatosLocales
    @Binds abstract fun estadoDeSync(impl: EstadoDeSyncDataStore): EstadoDeSyncLocal
    @Binds abstract fun avisoDeCambios(impl: AvisoDeCambiosLocales): AvisoDeCambios
    @Binds abstract fun alarmasDeTareas(impl: AlarmasDeTareasAndroid): AlarmasDeTareas
    @Binds abstract fun avisoDeTareasNuevas(impl: AvisoDeTareasNuevasAndroid): AvisoDeTareasNuevas
    @Binds abstract fun preferenciasLocales(impl: PreferenciasDataStore): PreferenciasLocales
    @Binds abstract fun onboarding(impl: OnboardingRepositoryImpl): OnboardingRepository
}
