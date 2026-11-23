package ar.edu.uade.miti.domain.usecase.primeruso

import ar.edu.uade.miti.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservarOnboardingVistoUseCase @Inject constructor(private val onboarding: OnboardingRepository) {
    operator fun invoke(): Flow<Boolean> = onboarding.observarVisto()
}
