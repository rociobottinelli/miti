package ar.edu.uade.miti.data.repository

import android.database.sqlite.SQLiteException
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.entity.MetadatosSync
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

// Implementaciones de los contratos del dominio con Room (D05). Cada escritura marca la fila
// como PENDIENTE de sincronizar, con la hora del cambio; el SyncWorker (#18) las va a enviar.

/**
 * Consulta [consulta] sobre el hogar activo y, si cambia el hogar, vuelve a consultar sobre el nuevo.
 * Sin hogar activo devuelve [vacio].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> HogarActivo.observar(vacio: T, consulta: (hogarId: String) -> Flow<T>): Flow<T> =
    id.flatMapLatest { hogarId -> if (hogarId == null) flowOf(vacio) else consulta(hogarId) }

internal fun Clock.cambioPendiente() = MetadatosSync(updatedAt = millis())

internal const val NO_SE_GUARDO = "No se pudo guardar en el celular. Probá de nuevo."
internal const val NO_SE_LEYO = "No se pudieron leer los datos guardados en el celular."

/**
 * Ejecuta [operacion] sobre Room y traduce el resultado a [Resource]. Solo atrapa [SQLiteException]
 * (base llena, corrupta o bloqueada): cualquier otra excepción, incluida la cancelación de la corrutina,
 * sigue su curso. Room ya corre las funciones suspend fuera del hilo principal.
 */
internal suspend fun <T> enBase(mensajeDeError: String, operacion: suspend () -> T): Resource<T> =
    try {
        Resource.Success(operacion())
    } catch (e: SQLiteException) {
        Resource.Error(mensajeDeError, e)
    }
