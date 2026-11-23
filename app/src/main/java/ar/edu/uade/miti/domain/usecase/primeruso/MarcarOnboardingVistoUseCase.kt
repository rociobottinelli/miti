package ar.edu.uade.miti.domain.usecase.primeruso

import ar.edu.uade.miti.domain.repository.OnboardingRepository
import javax.inject.Inject

/** Al terminar o al omitir el onboarding: no vuelve a mostrarse en este celular. */
class MarcarOnboardingVistoUseCase @Inject constructor(private val onboarding: OnboardingRepository) {
    suspend operator fun invoke() = onboarding.marcarVisto()
}
