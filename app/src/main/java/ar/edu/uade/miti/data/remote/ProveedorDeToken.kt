package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.local.datastore.SesionLocal
import ar.edu.uade.miti.data.remote.dto.RenovarSesionDto
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Da un access token vigente para hablar con Supabase. Si vence en menos de [MARGEN_MS], lo renueva antes
 * con el refresh token. El Mutex evita que dos pedidos a la vez lo renueven dos veces (el segundo refresh
 * fallaría porque Supabase invalida el refresh token usado).
 */
@Singleton
class ProveedorDeToken @Inject constructor(
    private val api: AuthApiService,
    private val sesionLocal: SesionLocal,
    private val reloj: Clock,
) {
    private val mutex = Mutex()

    suspend fun tokenVigente(): Resource<String> = mutex.withLock {
        val actual = sesionLocal.sesion.first() ?: return@withLock Resource.Error(SESION_VENCIDA)
        if (actual.venceEn - reloj.millis() > MARGEN_MS) return@withLock Resource.Success(actual.accessToken)
        renovar(actual)
    }

    private suspend fun renovar(actual: SesionGuardada): Resource<String> {
        val respuesta = try {
            api.renovarSesion(RenovarSesionDto(actual.refreshToken))
        } catch (e: JsonEncodingException) {
            // JSON roto. Va antes que IOException porque hereda de ella: no es un problema de conexión.
            return Resource.Error(RESPUESTA_INESPERADA, e)
        } catch (e: IOException) {
            return Resource.Error(SIN_CONEXION, e)
        } catch (e: JsonDataException) {
            return Resource.Error(RESPUESTA_INESPERADA, e)
        }
        val cuerpo = respuesta.body()
        val access = cuerpo?.accessToken
        val refresh = cuerpo?.refreshToken
        return when {
            respuesta.isSuccessful && access != null && refresh != null -> {
                sesionLocal.guardar(actual.copy(accessToken = access, refreshToken = refresh, venceEn = venceEn(cuerpo.expiresIn)))
                Resource.Success(access)
            }
            // 400/401: el refresh token ya no sirve (se cerró sesión en otro lado o venció). Hay que volver a ingresar.
            respuesta.code() in 400..499 -> {
                sesionLocal.borrar()
                Resource.Error(SESION_VENCIDA)
            }
            else -> Resource.Error(mensajeDeErrorDeAuth(respuesta.code(), null))
        }
    }

    internal fun venceEn(segundos: Long?): Long = reloj.millis() + (segundos ?: UNA_HORA_S) * 1000

    companion object {
        /** Se renueva un minuto antes de que venza, para que no venza a mitad de un pedido. */
        const val MARGEN_MS = 60_000L
        private const val UNA_HORA_S = 3600L
    }
}
