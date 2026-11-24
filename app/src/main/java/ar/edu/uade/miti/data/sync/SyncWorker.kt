package ar.edu.uade.miti.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.edu.uade.miti.data.avisos.ProgramadorDeAvisos
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Corre el [Sincronizador] en segundo plano con WorkManager (D06): funciona con la app cerrada y después
 * de reiniciar el teléfono, y solo arranca cuando hay red (ver [ProgramadorDeSync]).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val sincronizador: Sincronizador,
    private val avisos: ProgramadorDeAvisos,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val resultado = sincronizador.sincronizar()
        // Lo que bajó puede traer tareas con aviso nuevas, resueltas o reasignadas (#23).
        if (resultado is ResultadoDeSync.Completa && resultado.descargadas > 0) avisos.pedir()
        return resultadoPara(resultado, runAttemptCount)
    }

    companion object {
        /** Después de tantos intentos fallidos se deja: lo pendiente sigue en Room y sube con el próximo disparo. */
        const val MAXIMO_DE_INTENTOS = 10

        internal fun resultadoPara(resultado: ResultadoDeSync, intento: Int): Result = when (resultado) {
            is ResultadoDeSync.Completa, ResultadoDeSync.SinSesion, ResultadoDeSync.SinHogar -> Result.success()
            is ResultadoDeSync.Reintentar -> if (intento < MAXIMO_DE_INTENTOS) Result.retry() else Result.failure()
        }
    }
}
