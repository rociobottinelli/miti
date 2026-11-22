package ar.edu.uade.miti.domain.repository

import kotlinx.coroutines.flow.Flow

/** Si en este celular ya se vio (u omitió) el onboarding. Decide si la app arranca en él (flujo 1). */
interface OnboardingRepository {
    fun observarVisto(): Flow<Boolean>
    suspend fun marcarVisto()
}
