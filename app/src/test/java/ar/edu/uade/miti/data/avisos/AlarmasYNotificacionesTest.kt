package ar.edu.uade.miti.data.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmasYNotificacionesTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = app.getSystemService(AlarmManager::class.java)
    private val alarmas = AlarmasDeTareasAndroid(app)
    private val cuando = LocalDateTime.now().plusHours(3).withNano(0)

    private fun tarea(id: String, hora: LocalDateTime = cuando) =
        Pendiente(id, "h1", TipoPendiente.TAREA, "Pagar ABL", "ana", hora, avisar = true)

    @Before
    fun preparar() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Notificaciones.crearCanales(app)
    }

    @Test
    fun `programa una alarma exacta a la hora de la tarea`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1")))

        val alarma = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), alarma.triggerAtTime)
        assertTrue(alarma.isAllowWhileIdle)
        assertTrue(alarmas.permisos().alarmasExactas)
    }

    @Test
    fun `sin permiso de exactas igual programa una aproximada y lo informa`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        alarmas.programarSolo(listOf(tarea("t1")))

        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        assertFalse(alarmas.permisos().alarmasExactas)
    }

    @Test
    fun `cancela las tareas que ya no corresponden y no toca las demas`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1"), tarea("t2", cuando.plusHours(1))))
        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)

        alarmas.programarSolo(listOf(tarea("t2", cuando.plusHours(1))))

        val quedan = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, quedan.size)
        assertEquals(cuando.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), quedan.single().triggerAtTime)
    }

    @Test
    fun `reprogramar la misma tarea con otra hora no la duplica`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1")))
        alarmas.programarSolo(listOf(tarea("t1", cuando.plusMinutes(30))))

        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `la notificacion de la tarea trae la accion marcar como hecha`() {
        Notificaciones.mostrarAvisoDeTarea(app, "t1", "Pagar ABL")

        val notificacion = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("Pagar ABL", shadowOf(notificacion).contentTitle)
        assertEquals("Marcar como hecha", notificacion.actions.single().title)
        assertEquals(Notificaciones.CANAL_AVISOS, notificacion.channelId)
    }

    @Test
    fun `sin permiso de notificaciones no se muestra nada`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        Notificaciones.mostrarAvisoDeTarea(app, "t1", "Pagar ABL")

        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
        assertFalse(alarmas.permisos().notificaciones)
    }

    @Test
    fun `las tareas nuevas se juntan en una sola notificacion`() {
        Notificaciones.mostrarTareasNuevas(app, listOf("Pagar ABL", "Sacar turno"))

        val notificacion = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("Te asignaron 2 tareas", shadowOf(notificacion).contentTitle)
    }
}
