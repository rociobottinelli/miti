package ar.edu.uade.miti.data.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.usecase.pendientes.CompletarTareaUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Corre [bloque] en segundo plano sin que Android corte el receptor a mitad de camino (goAsync da unos
 * segundos más). Es lo que permite usar casos de uso suspend desde una notificación o una alarma.
 */
private fun BroadcastReceiver.enSegundoPlano(bloque: suspend () -> Unit) {
    val pendiente = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            bloque()
        } finally {
            pendiente.finish()
        }
    }
}

/**
 * Sonó la alarma de una tarea. Antes de avisar se fija en Room que siga sin resolver: puede haberla resuelto
 * la otra persona y haber llegado con la sincronización después de que se programó la alarma.
 */
@AndroidEntryPoint
class ReceptorDeAlarma : BroadcastReceiver() {
    @Inject lateinit var pendientes: PendienteRepository

    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(Notificaciones.EXTRA_TAREA_ID) ?: return
        val tituloProgramado = intent.getStringExtra(EXTRA_TITULO).orEmpty()
        enSegundoPlano {
            val tarea = (pendientes.obtenerPendiente(id) as? Resource.Success)?.data
            // Si no se pudo leer la base, se avisa igual con el título programado: es preferible a no avisar.
            if (tarea != null && (tarea.resuelto || tarea.tipo != TipoPendiente.TAREA)) return@enSegundoPlano
            Notificaciones.mostrarAvisoDeTarea(contexto, id, tarea?.titulo ?: tituloProgramado)
        }
    }

    companion object {
        const val EXTRA_TITULO = "titulo"
    }
}

/** "Marcar como hecha" desde la notificación: usa el mismo caso de uso que la app (D03). */
@AndroidEntryPoint
class ReceptorDeCompletarTarea : BroadcastReceiver() {
    @Inject lateinit var completarTarea: CompletarTareaUseCase

    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(Notificaciones.EXTRA_TAREA_ID) ?: return
        enSegundoPlano {
            if (completarTarea(id) is Resource.Success) Notificaciones.cancelarAvisoDeTarea(contexto, id)
        }
    }
}

/**
 * Android borra las alarmas al apagarse, y un cambio de hora o de zona las corre: en esos casos se
 * vuelven a programar todas (D10, RNF06).
 */
@AndroidEntryPoint
class ReceptorDeReinicio : BroadcastReceiver() {
    @Inject lateinit var programador: ProgramadorDeAvisos

    override fun onReceive(contexto: Context, intent: Intent) {
        // Solo las acciones del sistema que lo justifican: así otra app no puede dispararlo con otra acción.
        if (intent.action in ACCIONES) programador.pedir()
    }

    private companion object {
        val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
