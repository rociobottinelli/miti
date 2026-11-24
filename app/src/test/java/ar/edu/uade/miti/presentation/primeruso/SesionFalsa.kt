package ar.edu.uade.miti.presentation.primeruso

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Sesión falsa para los ViewModels del primer uso: anota los pedidos y puede fallar a pedido. */
class SesionFalsa : SesionRepository {
    val usuario = MutableStateFlow<Usuario?>(null)
    val pedidos = mutableListOf<String>()
    var fallarSinConexion = false
    var errorDelServidor: String? = null

    override fun observarUsuario(): Flow<Usuario?> = usuario

    override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String): Resource<Usuario> {
        pedidos += "crear:$nombre:$email"
        return responder(email)
    }

    override suspend fun iniciarSesion(email: String, contrasenia: String): Resource<Usuario> {
        pedidos += "ingresar:$email"
        return responder(email)
    }

    override suspend fun cerrarSesion(): Resource<Unit> {
        usuario.value = null
        return Resource.Success(Unit)
    }

    private fun responder(email: String): Resource<Usuario> {
        if (fallarSinConexion) return Resource.Error("Necesitás conexión para ingresar.", IOException("sin red"))
        errorDelServidor?.let { return Resource.Error(it) }
        return Resource.Success(Usuario("u-ana", email)).also { usuario.value = it.data }
    }
}
