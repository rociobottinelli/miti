package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.BorradorDeDatosLocales
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.local.datastore.SesionLocal
import ar.edu.uade.miti.data.remote.AuthApiService
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.RESPUESTA_INESPERADA
import ar.edu.uade.miti.data.remote.SIN_CONEXION
import ar.edu.uade.miti.data.remote.SIN_CONFIGURAR
import ar.edu.uade.miti.data.remote.dto.CredencialesDto
import ar.edu.uade.miti.data.remote.dto.CrearCuentaDto
import ar.edu.uade.miti.data.remote.dto.ErrorDeAuthDto
import ar.edu.uade.miti.data.remote.dto.MetadatosDeUsuarioDto
import ar.edu.uade.miti.data.remote.dto.SesionDto
import ar.edu.uade.miti.data.remote.mensajeDeErrorDeAuth
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Response

/** Cuenta y sesión contra Supabase Auth; la sesión queda en DataStore. */
class SesionRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val configuracion: ConfiguracionSupabase,
    private val sesionLocal: SesionLocal,
    private val proveedorDeToken: ProveedorDeToken,
    private val hogarActivo: HogarActivo,
    private val datosLocales: BorradorDeDatosLocales,
    moshi: Moshi,
) : SesionRepository {

    private val adaptadorDeError = moshi.adapter(ErrorDeAuthDto::class.java)

    override fun observarUsuario(): Flow<Usuario?> =
        sesionLocal.sesion.map { it?.let { s -> Usuario(s.usuarioId, s.email) } }

    override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String): Resource<Usuario> =
        autenticar { api.crearCuenta(CrearCuentaDto(email, contrasenia, MetadatosDeUsuarioDto(nombre))) }

    override suspend fun iniciarSesion(email: String, contrasenia: String): Resource<Usuario> =
        autenticar { api.iniciarSesion(CredencialesDto(email, contrasenia)) }

    override suspend fun cerrarSesion(): Resource<Unit> {
        val actual = sesionLocal.sesion.first()
        if (actual != null && configuracion.configurado) {
            // Revocar el token en el servidor es "lo mejor posible": sin conexión, la sesión se cierra igual
            // en el celular y el token vence solo.
            try {
                api.cerrarSesion("Bearer ${actual.accessToken}")
            } catch (_: IOException) {
            }
        }
        sesionLocal.borrar()
        hogarActivo.establecer(null)
        datosLocales.borrarTodo()
        return Resource.Success(Unit)
    }

    /** Llama a Supabase y, si devuelve una sesión, la guarda. Maneja los tres tipos de fallo de red. */
    private suspend fun autenticar(pedido: suspend () -> Response<SesionDto>): Resource<Usuario> {
        if (!configuracion.configurado) return Resource.Error(SIN_CONFIGURAR)
        val respuesta = try {
            pedido()
        } catch (e: JsonEncodingException) {
            // JSON roto. Va antes que IOException porque hereda de ella: no es un problema de conexión.
            return Resource.Error(RESPUESTA_INESPERADA, e)
        } catch (e: IOException) {
            return Resource.Error(SIN_CONEXION, e)
        } catch (e: JsonDataException) {
            return Resource.Error(RESPUESTA_INESPERADA, e)
        }
        if (!respuesta.isSuccessful) {
            return Resource.Error(mensajeDeErrorDeAuth(respuesta.code(), leerError(respuesta)))
        }
        val cuerpo = respuesta.body() ?: return Resource.Error(RESPUESTA_INESPERADA)
        val access = cuerpo.accessToken
        val refresh = cuerpo.refreshToken
        val usuario = cuerpo.user
        if (access == null || refresh == null || usuario == null) {
            // El proyecto exige confirmar el email: la cuenta se creó, pero todavía no hay sesión.
            return Resource.Error("Te mandamos un email para confirmar la cuenta. Confirmala y después ingresá.")
        }
        val guardada = SesionGuardada(
            usuarioId = usuario.id,
            email = usuario.email.orEmpty(),
            accessToken = access,
            refreshToken = refresh,
            venceEn = proveedorDeToken.venceEn(cuerpo.expiresIn),
        )
        sesionLocal.guardar(guardada)
        return Resource.Success(Usuario(guardada.usuarioId, guardada.email))
    }

    private fun leerError(respuesta: Response<*>): ErrorDeAuthDto? =
        try {
            respuesta.errorBody()?.string()?.let(adaptadorDeError::fromJson)
        } catch (_: JsonDataException) {
            null
        } catch (_: JsonEncodingException) {
            null
        } catch (_: IOException) {
            null
        }
}
