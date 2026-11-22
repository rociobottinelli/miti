package ar.edu.uade.miti.domain.usecase.sesion

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.repository.SesionRepository
import javax.inject.Inject

/** Cierra la sesión y borra del celular los datos del hogar. */
class CerrarSesionUseCase @Inject constructor(private val sesion: SesionRepository) {
    suspend operator fun invoke(): Resource<Unit> = sesion.cerrarSesion()
}
