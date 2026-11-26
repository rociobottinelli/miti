package ar.edu.uade.miti.domain.usecase.sesion

import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** El usuario logueado, o null. Decide si la app abre en el primer uso o directo en Hoy (flujo 1). */
class ObservarUsuarioUseCase @Inject constructor(private val sesion: SesionRepository) {
    operator fun invoke(): Flow<Usuario?> = sesion.observarUsuario()
}
