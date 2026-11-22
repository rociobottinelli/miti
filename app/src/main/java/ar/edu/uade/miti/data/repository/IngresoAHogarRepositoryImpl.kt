package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.data.local.dao.HogarDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.HogarApiService
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.RESPUESTA_INESPERADA
import ar.edu.uade.miti.data.remote.SIN_CONEXION_HOGAR
import ar.edu.uade.miti.data.remote.SIN_CONFIGURAR
import ar.edu.uade.miti.data.remote.SyncApiService
import ar.edu.uade.miti.data.remote.dto.CrearHogarDto
import ar.edu.uade.miti.data.remote.dto.ErrorDePostgrestDto
import ar.edu.uade.miti.data.remote.dto.HogarDto
import ar.edu.uade.miti.data.remote.dto.IntegranteDto
import ar.edu.uade.miti.data.remote.dto.UnirseAHogarDto
import ar.edu.uade.miti.data.remote.dto.UsuarioConDatosDto
import ar.edu.uade.miti.data.remote.mensajeDeErrorDeHogar
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.IOException
import javax.inject.Inject
import retrofit2.Response

/**
 * Crea o se une a un hogar en Supabase y deja el resultado en Room como ya sincronizado, así Hoy lo
 * muestra sin esperar al SyncWorker. Después pide una sincronización, que trae a la otra persona
 * (si se unió) y todo lo que ya hubiera en el hogar.
 */
class IngresoAHogarRepositoryImpl @Inject constructor(
    private val api: HogarApiService,
    private val syncApi: SyncApiService,
    private val configuracion: ConfiguracionSupabase,
    private val proveedorDeToken: ProveedorDeToken,
    private val dao: HogarDao,
    private val hogarActivo: HogarActivo,
    private val avisoDeCambios: AvisoDeCambios,
    moshi: Moshi,
) : IngresoAHogarRepository {

    private val adaptadorDeError = moshi.adapter(ErrorDePostgrestDto::class.java)

    override suspend fun crearHogar(nombreHogar: String): Resource<Hogar> = entrar { autorizacion, yo ->
        api.crearHogar(autorizacion, CrearHogarDto(nombreHogar, yo.nombre))
    }

    override suspend fun unirseAHogar(codigo: String): Resource<Hogar> = entrar { autorizacion, yo ->
        api.unirseAHogar(autorizacion, UnirseAHogarDto(codigo, yo.nombre))
    }

    override suspend fun recuperarMiHogar(): Resource<Boolean> {
        if (!configuracion.configurado) return Resource.Error(SIN_CONFIGURAR)
        val autorizacion = autorizacion().getOrElse { return it }
        val yo = llamar { api.usuarioActual(autorizacion) }.getOrElse { return it }
        val integrante = llamar { syncApi.buscarIntegrante(autorizacion, "eq.${yo.id}") }.getOrElse { return it }
            .firstOrNull() ?: return Resource.Success(false)
        hogarActivo.establecer(integrante.hogarId)
        avisoDeCambios.huboCambios()
        return Resource.Success(true)
    }

    /** Los pasos comunes de crear y unirse: token, nombre de la persona, la llamada y guardar el hogar. */
    private suspend fun entrar(pedido: suspend (autorizacion: String, yo: Yo) -> Response<HogarDto>): Resource<Hogar> {
        if (!configuracion.configurado) return Resource.Error(SIN_CONFIGURAR)
        val autorizacion = autorizacion().getOrElse { return it }
        val yo = llamar { api.usuarioActual(autorizacion) }.getOrElse { return it }.let(::Yo)
        val hogar = llamar { pedido(autorizacion, yo) }.getOrElse { return it }
        return guardar(hogar, yo)
    }

    private suspend fun guardar(hogar: HogarDto, yo: Yo): Resource<Hogar> {
        val guardado = enBase(NO_SE_GUARDO) {
            dao.guardarHogaresDescargados(listOf(hogar.toEntity()))
            // El servidor ya creó a este integrante; se guarda igual para que la app lo conozca ya.
            dao.guardarIntegrantesDescargados(
                listOf(IntegranteDto(yo.id, hogar.id, yo.nombre, yo.email, hogar.updatedAt).toEntity()),
            )
        }
        if (guardado is Resource.Error) return guardado
        hogarActivo.establecer(hogar.id)
        avisoDeCambios.huboCambios()
        return Resource.Success(Hogar(hogar.id, hogar.nombre, hogar.codigoUnion))
    }

    private suspend fun autorizacion(): Resource<String> = when (val token = proveedorDeToken.tokenVigente()) {
        is Resource.Success -> Resource.Success("Bearer ${token.data}")
        is Resource.Error -> token
    }

    /** Hace el pedido y traduce los tres tipos de falla: sin red, respuesta rota y error del servidor. */
    private suspend fun <T> llamar(pedido: suspend () -> Response<T>): Resource<T> {
        val respuesta = try {
            pedido()
        } catch (e: JsonEncodingException) {
            // JSON roto. Va antes que IOException porque hereda de ella: no es un problema de conexión.
            return Resource.Error(RESPUESTA_INESPERADA, e)
        } catch (e: IOException) {
            return Resource.Error(SIN_CONEXION_HOGAR, e)
        } catch (e: JsonDataException) {
            return Resource.Error(RESPUESTA_INESPERADA, e)
        }
        if (!respuesta.isSuccessful) return Resource.Error(mensajeDeErrorDeHogar(respuesta.code(), leerError(respuesta)))
        return respuesta.body()?.let { Resource.Success(it) } ?: Resource.Error(RESPUESTA_INESPERADA)
    }

    private fun leerError(respuesta: Response<*>): ErrorDePostgrestDto? =
        try {
            respuesta.errorBody()?.string()?.let(adaptadorDeError::fromJson)
        } catch (_: JsonDataException) {
            null
        } catch (_: JsonEncodingException) {
            null
        } catch (_: IOException) {
            null
        }

    /** Quién usa el celular. Si la cuenta no tiene nombre (no debería pasar), se usa el principio del email. */
    private class Yo(usuario: UsuarioConDatosDto) {
        val id = usuario.id
        val email = usuario.email.orEmpty()
        val nombre = usuario.datos?.nombre?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')
    }
}
