package ar.edu.uade.miti.data.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.PermisosDeAvisos
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.avisos: DataStore<Preferences> by preferencesDataStore(name = "avisos")

/**
 * Alarmas locales con AlarmManager (D10). Recuerda qué tareas dejó programadas (en DataStore) para poder
 * cancelar las que ya no corresponden: AlarmManager no permite listar las alarmas existentes.
 *
 * Con permiso de alarmas exactas usa setExactAndAllowWhileIdle (suena aunque el celular esté en reposo, RNF06).
 * Sin ese permiso (Android 12+ lo puede negar) usa una alarma aproximada: suena, pero el sistema puede
 * correrla unos minutos; Hoy lo informa (#23).
 */
@Singleton
class AlarmasDeTareasAndroid @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : AlarmasDeTareas {

    private val alarmManager get() = contexto.getSystemService(AlarmManager::class.java)
    private val programadas = stringSetPreferencesKey("tareas_programadas")

    override suspend fun programarSolo(tareas: List<Pendiente>) {
        val anteriores = contexto.avisos.data.first()[programadas].orEmpty()
        val nuevas = tareas.map { it.id }.toSet()

        (anteriores - nuevas).forEach { id ->
            alarmManager.cancel(intencion(id, titulo = ""))
            Notificaciones.cancelarAvisoDeTarea(contexto, id)
        }
        tareas.forEach { tarea -> programar(tarea) }

        contexto.avisos.edit { it[programadas] = nuevas }
    }

    override fun permisos() = PermisosDeAvisos(
        notificaciones = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        alarmasExactas = puedeExactas(),
    )

    private fun programar(tarea: Pendiente) {
        val hora = tarea.fechaHora ?: return
        val momento = hora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intencion = intencion(tarea.id, tarea.titulo)
        if (puedeExactas()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intencion)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intencion)
        }
    }

    private fun puedeExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Una intención por tarea. El dato "miti://tarea/<id>" la hace única (Android compara intenciones sin
     * mirar los extras), así cancelar o reprogramar una tarea no toca las demás.
     */
    private fun intencion(tareaId: String, titulo: String): PendingIntent = PendingIntent.getBroadcast(
        contexto,
        tareaId.hashCode(),
        Intent(contexto, ReceptorDeAlarma::class.java)
            .setData("miti://tarea/$tareaId".toUri())
            .putExtra(Notificaciones.EXTRA_TAREA_ID, tareaId)
            .putExtra(ReceptorDeAlarma.EXTRA_TITULO, titulo),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
