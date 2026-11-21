package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Cuenta y sesión del usuario (preentrega, sección 8: email y contraseña). La contraseña solo viaja al
 * servidor para obtener la sesión: nunca se guarda en el celular (RNF10).
 */
interface SesionRepository {
    /** El usuario logueado, o null si no hay sesión. Se actualiza solo al ingresar o salir. */
    fun observarUsuario(): Flow<Usuario?>
    suspend fun crearCuenta(nombre: String, email: String, contrasenia: String): Resource<Usuario>
    suspend fun iniciarSesion(email: String, contrasenia: String): Resource<Usuario>

    /** Cierra la sesión y borra los datos locales del hogar, aunque no haya conexión. */
    suspend fun cerrarSesion(): Resource<Unit>
}
