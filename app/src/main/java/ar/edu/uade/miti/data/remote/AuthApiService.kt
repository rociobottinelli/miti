package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.data.remote.dto.CredencialesDto
import ar.edu.uade.miti.data.remote.dto.CrearCuentaDto
import ar.edu.uade.miti.data.remote.dto.RenovarSesionDto
import ar.edu.uade.miti.data.remote.dto.SesionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/** Supabase Auth. Las rutas son relativas a la URL del proyecto (ver NetworkModule). */
interface AuthApiService {
    @POST("auth/v1/signup")
    suspend fun crearCuenta(@Body cuenta: CrearCuentaDto): Response<SesionDto>

    @POST("auth/v1/token")
    suspend fun iniciarSesion(
        @Body credenciales: CredencialesDto,
        @Query("grant_type") tipo: String = "password",
    ): Response<SesionDto>

    @POST("auth/v1/token")
    suspend fun renovarSesion(
        @Body renovar: RenovarSesionDto,
        @Query("grant_type") tipo: String = "refresh_token",
    ): Response<SesionDto>

    @POST("auth/v1/logout")
    suspend fun cerrarSesion(@Header("Authorization") bearer: String): Response<Unit>
}
