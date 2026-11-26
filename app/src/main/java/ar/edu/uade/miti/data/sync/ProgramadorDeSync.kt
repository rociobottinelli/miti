package ar.edu.uade.miti.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Avisa que hubo un cambio local que hay que subir. Los repositorios lo llaman después de cada escritura. */
fun interface AvisoDeCambios {
    suspend fun huboCambios()
}

/**
 * Programa el [SyncWorker] (D06). Disparadores de la preentrega (sección 14, "cuándo se actualiza"):
 * - **Después de cada cambio** y **al abrir la app**: [sincronizarAhora].
 * - **Al recuperar la red**: un trabajo programado sin red espera solo hasta que vuelva (restricción CONNECTED).
 * - **Cada 15 minutos**: [programarPeriodica] (el mínimo de WorkManager).
 * Si falla, WorkManager reintenta con espera creciente (RNF09).
 */
@Singleton
class ProgramadorDeSync @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : AvisoDeCambios {

    private val workManager get() = WorkManager.getInstance(contexto)

    private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override suspend fun huboCambios() = sincronizarAhora()

    /**
     * Si ya hay una sincronización esperando para arrancar, no agrega otra: esa va a subir este cambio también.
     * Si hay una corriendo, encola la siguiente detrás (APPEND), así un cambio hecho a mitad de una sincronización
     * no queda esperando hasta la periódica.
     */
    suspend fun sincronizarAhora() {
        val enCola = workManager.getWorkInfosForUniqueWorkFlow(UNICA).first()
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
        if (enCola) return
        val pedido = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(UNICA, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
    }

    /** KEEP: si ya estaba programada (por ejemplo, de una apertura anterior), se deja como está. */
    fun programarPeriodica() {
        val pedido = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODICA, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /**
     * Qué está haciendo WorkManager con la sincronización: si hay una corriendo y si la última falló y
     * espera para reintentar (#19).
     */
    fun observarTrabajo(): Flow<EstadoDelTrabajo> =
        combine(
            workManager.getWorkInfosForUniqueWorkFlow(UNICA),
            workManager.getWorkInfosForUniqueWorkFlow(PERIODICA),
        ) { unica, periodica ->
            val todos = unica + periodica
            EstadoDelTrabajo(
                sincronizando = todos.any { it.state == WorkInfo.State.RUNNING },
                reintentando = unica.any { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount > 0 },
            )
        }

    data class EstadoDelTrabajo(val sincronizando: Boolean, val reintentando: Boolean)

    private companion object {
        const val UNICA = "sincronizar"
        const val PERIODICA = "sincronizar-periodica"
        const val ESPERA_INICIAL_S = 30L
    }
}
