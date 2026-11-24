package ar.edu.uade.miti

import ar.edu.uade.miti.presentation.widget.ActualizadorDeWidgets
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MitiApp : Application(), Configuration.Provider {

    /** Permite que WorkManager cree el SyncWorker con sus dependencias de Hilt (D03, D06). */
    @Inject lateinit var fabricaDeWorkers: HiltWorkerFactory

    @Inject lateinit var programadorDeSync: ProgramadorDeSync

    @Inject lateinit var actualizadorDeWidgets: ActualizadorDeWidgets

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaDeWorkers).build()

    override fun onCreate() {
        super.onCreate()
        // Solo encola un trabajo (no hace red ni disco acá): la sincronización periódica cada 15 minutos.
        programadorDeSync.programarPeriodica()
        // Tampoco bloquea: escucha Room en segundo plano para que los widgets reflejen cada cambio (RNF05).
        actualizadorDeWidgets.empezar()
    }
}
