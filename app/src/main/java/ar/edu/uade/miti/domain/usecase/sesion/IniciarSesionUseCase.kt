package ar.edu.uade.miti.domain.usecase.sesion

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import javax.inject.Inject

/**
 * Ingresa con email y contraseña. Solo exige que estén completos: el largo mínimo no se valida acá para
 * no revelar nada sobre la contraseña de una cuenta existente.
 */
class IniciarSesionUseCase @Inject constructor(private val sesion: SesionRepository) {
    suspend operator fun invoke(email: String, contrasenia: String): Resource<Usuario> {
        val error = ValidacionDeCuenta.errorDeEmail(email)
            ?: if (contrasenia.isEmpty()) "Ingresá tu contraseña" else null
        if (error != null) return Resource.Error(error)
        return sesion.iniciarSesion(email.trim().lowercase(), contrasenia)
    }
}
