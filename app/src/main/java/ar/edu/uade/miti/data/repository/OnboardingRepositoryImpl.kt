package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.data.local.datastore.PreferenciasLocales
import ar.edu.uade.miti.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class OnboardingRepositoryImpl @Inject constructor(
    private val preferencias: PreferenciasLocales,
) : OnboardingRepository {
    override fun observarVisto(): Flow<Boolean> = preferencias.onboardingVisto
    override suspend fun marcarVisto() = preferencias.marcarOnboardingVisto()
}
