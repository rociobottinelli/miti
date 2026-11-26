package ar.edu.uade.miti.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferencias del celular que no dependen de la cuenta (preentrega, sección 14: "DataStore guarda …
 * el onboarding visto"). Cerrar sesión no las borra: el onboarding se ve una sola vez por celular.
 */
interface PreferenciasLocales {
    val onboardingVisto: Flow<Boolean>
    suspend fun marcarOnboardingVisto()
}

@Singleton
class PreferenciasDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PreferenciasLocales {

    private val clave = booleanPreferencesKey("onboarding_visto")

    override val onboardingVisto: Flow<Boolean> = context.preferencias.data.map { it[clave] ?: false }

    override suspend fun marcarOnboardingVisto() {
        context.preferencias.edit { it[clave] = true }
    }
}
