package ar.edu.uade.miti.data.avisos

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ar.edu.uade.miti.domain.usecase.avisos.ReprogramarAvisosUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reprograma las alarmas fuera del hilo principal (lee Room y DataStore). No necesita red. */
@HiltWorker
class AvisosWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val reprogramar: ReprogramarAvisosUseCase,
) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        reprogramar()
        return Result.success()
    }
}

/**
 * Pide reprogramar los avisos. Varios pedidos seguidos (por ejemplo, tachar cinco tareas) se juntan en uno:
 * REPLACE deja solo el último, y como siempre parte de lo que hay en Room, el resultado es el mismo.
 */
@Singleton
class ProgramadorDeAvisos @Inject constructor(@param:ApplicationContext private val contexto: Context) {
    fun pedir() {
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork("reprogramar-avisos", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AvisosWorker>().build())
    }
}
