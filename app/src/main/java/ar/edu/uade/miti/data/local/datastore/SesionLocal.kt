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
import kotlinx.coroutines.flow.map

/**
 * Lo que queda guardado de una sesión. Nunca la contraseña (RNF10): solo los tokens que devuelve
 * Supabase, que se pueden revocar cerrando sesión.
 */
data class SesionGuardada(
    val usuarioId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    /** Momento en que vence el access token, en milisegundos. */
    val venceEn: Long,
)

/** La sesión del usuario en este celular (preentrega, sección 14: "DataStore guarda la sesión"). */
interface SesionLocal {
    val sesion: Flow<SesionGuardada?>
    suspend fun guardar(sesion: SesionGuardada)
    suspend fun borrar()
}

// Archivo propio, separado de "preferencias": DataStore no admite dos instancias sobre el mismo archivo.
private val Context.sesion: DataStore<Preferences> by preferencesDataStore(name = "sesion")

@Singleton
class SesionLocalDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SesionLocal {

    private val usuarioId = stringPreferencesKey("usuario_id")
    private val email = stringPreferencesKey("email")
    private val accessToken = stringPreferencesKey("access_token")
    private val refreshToken = stringPreferencesKey("refresh_token")
    private val venceEn = longPreferencesKey("vence_en")

    override val sesion: Flow<SesionGuardada?> = context.sesion.data.map { p ->
        val id = p[usuarioId] ?: return@map null
        SesionGuardada(
            usuarioId = id,
            email = p[email].orEmpty(),
            accessToken = p[accessToken] ?: return@map null,
            refreshToken = p[refreshToken] ?: return@map null,
            venceEn = p[venceEn] ?: 0,
        )
    }

    override suspend fun guardar(sesion: SesionGuardada) {
        context.sesion.edit { p ->
            p[usuarioId] = sesion.usuarioId
            p[email] = sesion.email
            p[accessToken] = sesion.accessToken
            p[refreshToken] = sesion.refreshToken
            p[venceEn] = sesion.venceEn
        }
    }

    override suspend fun borrar() {
        context.sesion.edit { it.clear() }
    }
}
