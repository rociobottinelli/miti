package ar.edu.uade.miti

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ar.edu.uade.miti.data.avisos.Notificaciones
import ar.edu.uade.miti.data.avisos.ProgramadorDeAvisos
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MitiApp : Application(), Configuration.Provider {

    /** Permite que WorkManager cree el SyncWorker con sus dependencias de Hilt (D03, D06). */
    @Inject lateinit var fabricaDeWorkers: HiltWorkerFactory

    @Inject lateinit var programadorDeSync: ProgramadorDeSync

    @Inject lateinit var programadorDeAvisos: ProgramadorDeAvisos

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaDeWorkers).build()

    override fun onCreate() {
        super.onCreate()
        // Solo encola un trabajo (no hace red ni disco acá): la sincronización periódica cada 15 minutos.
        programadorDeSync.programarPeriodica()
        // Avisos de tareas (D10): los canales de notificación y las alarmas al día (también tras actualizar la app).
        Notificaciones.crearCanales(this)
        programadorDeAvisos.pedir()
    }
}
