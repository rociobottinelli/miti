package ar.edu.uade.miti.presentation.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * A la medianoche las rutinas se reinician. Si la app está cerrada nadie se entera, así que se programa
 * una alarma que actualiza los widgets. No es exacta (no necesita el permiso de alarmas exactas): puede
 * llegar unos minutos después, y además está el respaldo de `updatePeriodMillis` (30 minutos).
 */
object AlarmaDeMedianoche {
    const val ACCION = "ar.edu.uade.miti.widget.MEDIANOCHE"

    private val VENTANA = Duration.ofMinutes(5)

    fun programar(contexto: Context) {
        val alarmas = contexto.getSystemService(AlarmManager::class.java) ?: return
        val medianoche = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // RTC y no RTC_WAKEUP: si el celular está dormido nadie mira el widget; se actualiza al despertar.
        alarmas.setWindow(AlarmManager.RTC, medianoche, VENTANA.toMillis(), intencion(contexto))
    }

    private fun intencion(contexto: Context): PendingIntent = PendingIntent.getBroadcast(
        contexto,
        0,
        Intent(contexto, ReceptorWidgetDeRutinas::class.java).setAction(ACCION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
