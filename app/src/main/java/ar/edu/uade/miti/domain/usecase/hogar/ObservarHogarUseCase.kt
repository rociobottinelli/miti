package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.HogarRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * El hogar activo, con su código para compartir. Es null mientras sus datos no bajaron al celular
 * (por ejemplo, justo después de recuperar el hogar en un celular nuevo).
 */
class ObservarHogarUseCase @Inject constructor(private val hogares: HogarRepository) {
    operator fun invoke(): Flow<Hogar?> = hogares.observarHogar()
}
