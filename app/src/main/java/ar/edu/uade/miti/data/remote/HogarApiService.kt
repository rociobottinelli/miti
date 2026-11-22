package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.data.remote.dto.CrearHogarDto
import ar.edu.uade.miti.data.remote.dto.HogarDto
import ar.edu.uade.miti.data.remote.dto.UnirseAHogarDto
import ar.edu.uade.miti.data.remote.dto.UsuarioConDatosDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Crear un hogar y unirse a uno. Son funciones de Postgres (supabase/migrations/…0002) y no INSERT
 * directos, porque el código y el límite de dos integrantes se validan en el servidor (#10).
 */
interface HogarApiService {

    /** El usuario logueado con sus datos de cuenta; de ahí sale el nombre que puso al registrarse. */
    @GET("auth/v1/user")
    suspend fun usuarioActual(@Header("Authorization") autorizacion: String): Response<UsuarioConDatosDto>

    /** Devuelve el hogar nuevo, con su código de 6 caracteres para compartir. */
    @POST("rest/v1/rpc/crear_hogar")
    suspend fun crearHogar(
        @Header("Authorization") autorizacion: String,
        @Body datos: CrearHogarDto,
    ): Response<HogarDto>

    @POST("rest/v1/rpc/unirse_a_hogar")
    suspend fun unirseAHogar(
        @Header("Authorization") autorizacion: String,
        @Body datos: UnirseAHogarDto,
    ): Response<HogarDto>
}
