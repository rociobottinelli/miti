package ar.edu.uade.miti.domain.usecase.sesion

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import javax.inject.Inject

/** Crea la cuenta y deja la sesión iniciada. Con datos inválidos no llama al servidor. */
class CrearCuentaUseCase @Inject constructor(private val sesion: SesionRepository) {
    suspend operator fun invoke(nombre: String, email: String, contrasenia: String): Resource<Usuario> {
        val error = ValidacionDeCuenta.errorDeNombre(nombre)
            ?: ValidacionDeCuenta.errorDeEmail(email)
            ?: ValidacionDeCuenta.errorDeContrasenia(contrasenia)
        if (error != null) return Resource.Error(error)
        return sesion.crearCuenta(nombre.trim(), email.trim().lowercase(), contrasenia)
    }
}
