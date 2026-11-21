package ar.edu.uade.miti.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Supabase pide la clave del proyecto en el header "apikey" de cada pedido. */
class ClavePublicableInterceptor(private val clave: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("apikey", clave).build())
}
