package ar.edu.uade.miti.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Hasta dónde se descargó cada tabla y cuándo fue la última sincronización completa
 * (preentrega, sección 14: "DataStore guarda … la fecha de la última sincronización").
 */
interface EstadoDeSyncLocal {
    /** Hora del servidor (server_updated_at) del último cambio descargado de [tabla], o null si nunca se descargó. */
    suspend fun cursor(tabla: String): String?
    suspend fun guardarCursor(tabla: String, valor: String)

    /** Momento de la última sincronización completa, en milisegundos. RF02 pide mostrarla (#19). */
    val ultimaSincronizacion: Flow<Long?>
    suspend fun registrarSincronizacion(momento: Long)

    /** Al cerrar sesión: el próximo usuario descarga todo de nuevo. */
    suspend fun borrar()
}

private val Context.estadoDeSync: DataStore<Preferences> by preferencesDataStore(name = "sincronizacion")

@Singleton
class EstadoDeSyncDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : EstadoDeSyncLocal {

    private val ultima = longPreferencesKey("ultima_sincronizacion")

    override suspend fun cursor(tabla: String): String? =
        context.estadoDeSync.data.first()[stringPreferencesKey("cursor_$tabla")]

    override suspend fun guardarCursor(tabla: String, valor: String) {
        context.estadoDeSync.edit { it[stringPreferencesKey("cursor_$tabla")] = valor }
    }

    override val ultimaSincronizacion: Flow<Long?> = context.estadoDeSync.data.map { it[ultima] }

    override suspend fun registrarSincronizacion(momento: Long) {
        context.estadoDeSync.edit { it[ultima] = momento }
    }

    override suspend fun borrar() {
        context.estadoDeSync.edit { it.clear() }
    }
}
