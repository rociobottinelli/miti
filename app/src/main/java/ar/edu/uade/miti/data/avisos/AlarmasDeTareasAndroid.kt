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
 * El Manifest pide USE_EXACT_ALARM, que Android 13+ otorga solo, así que en la práctica siempre son exactas.
 * Si igual faltara (Android 12 con el permiso revocado a mano), usa una aproximada: suena, pero el sistema
 * puede correrla varios minutos.
 */
@Singleton
class AlarmasDeTareasAndroid @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : AlarmasDeTareas {

    private val alarmManager get() = contexto.getSystemService(AlarmManager::class.java)
    private val programadas = stringSetPreferencesKey("tareas_programadas")

    /** Tareas cuyo aviso ya se mostró (lo anota [ReceptorDeAlarma]), para no repetirlo. */
    private val avisadas = stringSetPreferencesKey("tareas_avisadas")

    override suspend fun programarSolo(tareas: List<Pendiente>, avisosVigentes: Set<String>) {
        val guardado = contexto.avisos.data.first()
        val anteriores = guardado[programadas].orEmpty()
        val yaAvisadas = guardado[avisadas].orEmpty()
        val nuevas = tareas.map { it.id }.toSet()

        (anteriores - nuevas).forEach { id ->
            alarmManager.cancel(intencion(id, titulo = ""))
            when {
                // Se resolvió, se borró o cambió de responsable: el aviso ya no corresponde.
                id !in avisosVigentes -> Notificaciones.cancelarAvisoDeTarea(contexto, id)
                // Sigue pendiente y su hora pasó sin que sonara: por ejemplo, el teléfono se reinició justo a
                // esa hora y Android avisó del reinicio cuando ya era tarde (probado en un emulador). Suena ahora,
                // tarde, que es mejor que nunca. Si ya sonó, su notificación se queda como está.
                id !in yaAvisadas -> sonarAhora(id)
            }
        }
        tareas.forEach { tarea -> programar(tarea) }

        contexto.avisos.edit {
            it[programadas] = nuevas
            // Una tarea que se vuelve a programar (por ejemplo, le cambiaron la hora) puede volver a sonar.
            it[avisadas] = (yaAvisadas intersect avisosVigentes) - nuevas
        }
    }

    /** Lo llama [ReceptorDeAlarma] después de mostrar el aviso. */
    suspend fun registrarAvisada(tareaId: String) {
        contexto.avisos.edit { it[avisadas] = it[avisadas].orEmpty() + tareaId }
    }

    private fun sonarAhora(tareaId: String) {
        // Sin título: al sonar, el receptor lo lee de Room.
        val intencion = intencion(tareaId, titulo = "")
        val ahora = System.currentTimeMillis()
        if (puedeExactas()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ahora, intencion)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ahora, intencion)
        }
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
