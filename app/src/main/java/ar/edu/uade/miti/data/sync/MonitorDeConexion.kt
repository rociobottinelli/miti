package ar.edu.uade.miti.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Si el celular tiene conexión a internet, y cuándo cambia (#19: banner "Sin conexión"). Escucha al
 * sistema mientras alguien observa y deja de escuchar al cancelarse el Flow (no queda nada registrado).
 */
@Singleton
class MonitorDeConexion @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) {
    fun observar(): Flow<Boolean> = callbackFlow {
        val conectividad = contexto.getSystemService(ConnectivityManager::class.java)
        fun hayInternet(): Boolean =
            conectividad.getNetworkCapabilities(conectividad.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(hayInternet()) }
            override fun onLost(network: Network) { trySend(hayInternet()) }
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                trySend(capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(hayInternet())
        conectividad.registerDefaultNetworkCallback(callback)
        awaitClose { conectividad.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
