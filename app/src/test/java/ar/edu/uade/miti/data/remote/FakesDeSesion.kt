package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.local.datastore.SesionLocal
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class FakeSesionLocal(inicial: SesionGuardada? = null) : SesionLocal {
    override val sesion = MutableStateFlow(inicial)
    override suspend fun guardar(sesion: SesionGuardada) { this.sesion.value = sesion }
    override suspend fun borrar() { sesion.value = null }
}

class FakeHogarActivo(inicial: String? = "h1") : HogarActivo {
    override val id = MutableStateFlow(inicial)
    override suspend fun establecer(hogarId: String?) { id.value = hogarId }
}

const val CLAVE_DE_PRUEBA = "sb_publishable_prueba"

/** El mismo armado que NetworkModule, pero apuntando al servidor falso. */
fun authApiContra(servidor: MockWebServer, moshi: Moshi = Moshi.Builder().build()): AuthApiService =
    Retrofit.Builder()
        .baseUrl(servidor.url("/"))
        .client(
            OkHttpClient.Builder()
                .addInterceptor(ClavePublicableInterceptor(CLAVE_DE_PRUEBA))
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build(),
        )
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AuthApiService::class.java)

fun sesionJson(access: String = "access-1", refresh: String = "refresh-1", expiraEn: Long = 3600) =
    """{"access_token":"$access","refresh_token":"$refresh","expires_in":$expiraEn,"token_type":"bearer",""" +
        """"user":{"id":"u-ana","email":"ana@mail.com","aud":"authenticated"}}"""
