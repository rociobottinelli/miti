package ar.edu.uade.miti.data.repository

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
