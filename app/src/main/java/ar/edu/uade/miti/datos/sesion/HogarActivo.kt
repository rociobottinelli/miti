package ar.edu.uade.miti.datos.sesion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** El hogar con el que trabaja la app. Los repositorios solo leen y escriben datos de este hogar. */
interface HogarActivo {
    val id: Flow<String?>
    suspend fun establecer(hogarId: String?)
}

private val Context.preferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

/**
 * Guarda el hogar activo en DataStore (preentrega, sección 14). La sesión, el onboarding visto y
 * la última sincronización se suman acá cuando se implementen (#16).
 */
@Singleton
class HogarActivoDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HogarActivo {

    private val clave = stringPreferencesKey("hogar_activo")

    override val id: Flow<String?> = context.preferencias.data.map { it[clave] }

    override suspend fun establecer(hogarId: String?) {
        context.preferencias.edit { preferencias ->
            if (hogarId == null) preferencias.remove(clave) else preferencias[clave] = hogarId
        }
    }
}
