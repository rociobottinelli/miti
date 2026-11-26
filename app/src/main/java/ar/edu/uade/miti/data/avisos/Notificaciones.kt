package ar.edu.uade.miti.data.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ar.edu.uade.miti.MainActivity
import ar.edu.uade.miti.R

/** Canales y notificaciones locales de Miti (D10). No hay push desde un servidor: todo sale del celular. */
object Notificaciones {
    const val CANAL_AVISOS = "avisos_de_tareas"
    const val CANAL_TAREAS_NUEVAS = "tareas_nuevas"
    const val EXTRA_TAREA_ID = "tarea_id"

    /** Se crean al arrancar la app. Crear un canal que ya existe no hace nada. */
    fun crearCanales(contexto: Context) {
        val manager = contexto.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CANAL_AVISOS, contexto.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = contexto.getString(R.string.canal_avisos_descripcion) },
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_TAREAS_NUEVAS, contexto.getString(R.string.canal_tareas_nuevas), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = contexto.getString(R.string.canal_tareas_nuevas_descripcion) },
        )
    }

    /** "Pagar ABL" a la hora fijada, con la acción "Marcar como hecha" (RF03, D03). */
    fun mostrarAvisoDeTarea(contexto: Context, tareaId: String, titulo: String) {
        val completar = PendingIntent.getBroadcast(
            contexto,
            tareaId.hashCode(),
            Intent(contexto, ReceptorDeCompletarTarea::class.java).putExtra(EXTRA_TAREA_ID, tareaId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(titulo)
            .setContentText(contexto.getString(R.string.aviso_tarea_texto))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp(contexto))
            .setAutoCancel(true)
            // Si el mismo aviso se vuelve a publicar (por ejemplo, reprogramar justo cuando sonaba), no suena dos veces.
            .setOnlyAlertOnce(true)
            .addAction(0, contexto.getString(R.string.accion_marcar_hecha), completar)
            .build()
        mostrar(contexto, idDeNotificacion(tareaId), notificacion)
    }

    /** #24: una descarga trajo tareas nuevas para esta persona. */
    fun mostrarTareasNuevas(contexto: Context, titulos: List<String>) {
        if (titulos.isEmpty()) return
        val texto = if (titulos.size == 1) titulos.single() else titulos.joinToString(" · ")
        val notificacion = NotificationCompat.Builder(contexto, CANAL_TAREAS_NUEVAS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(contexto.resources.getQuantityString(R.plurals.tareas_nuevas_titulo, titulos.size, titulos.size))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(titulos.joinToString("\n")))
            .setContentIntent(abrirApp(contexto))
            .setAutoCancel(true)
            .build()
        mostrar(contexto, ID_TAREAS_NUEVAS, notificacion)
    }

    fun cancelarAvisoDeTarea(contexto: Context, tareaId: String) =
        NotificationManagerCompat.from(contexto).cancel(idDeNotificacion(tareaId))

    /** Sin permiso de notificaciones (Android 13+) no se muestra nada; Hoy ya avisa que falta (#23). */
    private fun mostrar(contexto: Context, id: Int, notificacion: android.app.Notification) {
        val permitido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (permitido) NotificationManagerCompat.from(contexto).notify(id, notificacion)
    }

    private fun abrirApp(contexto: Context): PendingIntent = PendingIntent.getActivity(
        contexto,
        0,
        Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun idDeNotificacion(tareaId: String) = tareaId.hashCode()
    private const val ID_TAREAS_NUEVAS = 1
}
