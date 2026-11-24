package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.repository.HogarRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Integrantes del hogar activo, siempre en el mismo orden (por nombre). La posición en esta lista es la
 * que decide el color de cada persona, así que es la misma en los dos celulares.
 */
class ObservarIntegrantesUseCase @Inject constructor(private val hogares: HogarRepository) {
    operator fun invoke(): Flow<List<Integrante>> = hogares.observarIntegrantes()
}
