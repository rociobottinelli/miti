package ar.edu.uade.miti.di

import ar.edu.uade.miti.BuildConfig
import ar.edu.uade.miti.data.remote.AuthApiService
import ar.edu.uade.miti.data.remote.ClavePublicableInterceptor
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.HogarApiService
import ar.edu.uade.miti.data.remote.SyncApiService
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Red: un solo OkHttpClient, Moshi y Retrofit para toda la app (comparten el pool de conexiones).
 * Nunca se crean en un ViewModel ni en una pantalla.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun configuracionSupabase(): ConfiguracionSupabase =
        ConfiguracionSupabase(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun okHttpClient(configuracion: ConfiguracionSupabase): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(ClavePublicableInterceptor(configuracion.clavePublicable))
            .apply {
                // BASIC registra solo método, URL y código: nunca headers (tokens) ni cuerpos (contraseñas).
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    fun retrofit(configuracion: ConfiguracionSupabase, cliente: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(configuracion.urlBase)
            .client(cliente)
            // withNullSerialization: los campos en null viajan como null (ver SyncApiService).
            .addConverterFactory(MoshiConverterFactory.create(moshi).withNullSerialization())
            .build()

    @Provides
    @Singleton
    fun authApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun syncApiService(retrofit: Retrofit): SyncApiService = retrofit.create(SyncApiService::class.java)

    @Provides
    @Singleton
    fun hogarApiService(retrofit: Retrofit): HogarApiService = retrofit.create(HogarApiService::class.java)
}
