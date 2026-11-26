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
        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = emptySet())

        val alarma = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), alarma.triggerAtTime)
        assertTrue(alarma.isAllowWhileIdle)
        assertTrue(alarmas.permisos().alarmasExactas)
    }

    @Test
    fun `sin permiso de exactas igual programa una aproximada y lo informa`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = emptySet())

        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        assertFalse(alarmas.permisos().alarmasExactas)
    }

    @Test
    fun `cancela las tareas que ya no corresponden y no toca las demas`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1"), tarea("t2", cuando.plusHours(1))), avisosVigentes = emptySet())
        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)

        alarmas.programarSolo(listOf(tarea("t2", cuando.plusHours(1))), avisosVigentes = emptySet())

        val quedan = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, quedan.size)
        assertEquals(cuando.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), quedan.single().triggerAtTime)
    }

    @Test
    fun `reprogramar la misma tarea con otra hora no la duplica`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = emptySet())
        alarmas.programarSolo(listOf(tarea("t1", cuando.plusMinutes(30))), avisosVigentes = emptySet())

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
    fun `una tarea que ya sono y sigue pendiente conserva su notificacion`() = runTest {
        // La alarma arranca la app, la app reprograma y la tarea ya no se programa porque pasó su hora.
        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = setOf("t1"))
        Notificaciones.mostrarAvisoDeTarea(app, "t1", "Pagar ABL")
        alarmas.registrarAvisada("t1")

        alarmas.programarSolo(emptyList(), avisosVigentes = setOf("t1"))

        assertTrue("ya sonó: no vuelve a sonar", shadowOf(alarmManager).scheduledAlarms.isEmpty())
        assertEquals(1, shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size)
    }

    @Test
    fun `si la hora paso sin que sonara, por ejemplo durante un reinicio, suena ahora`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = setOf("t1"))

        // Al reprogramar después del reinicio, la tarea ya venció y sigue pendiente, pero nunca sonó.
        val antes = System.currentTimeMillis()
        alarmas.programarSolo(emptyList(), avisosVigentes = setOf("t1"))

        val alarma = shadowOf(alarmManager).scheduledAlarms.single()
        assertTrue(alarma.triggerAtTime in antes..System.currentTimeMillis())

        // Y no se repite en la siguiente reprogramación.
        alarmas.programarSolo(emptyList(), avisosVigentes = setOf("t1"))
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `si la tarea se resolvio se retira su notificacion`() = runTest {
        alarmas.programarSolo(listOf(tarea("t1")), avisosVigentes = setOf("t1"))
        Notificaciones.mostrarAvisoDeTarea(app, "t1", "Pagar ABL")

        alarmas.programarSolo(emptyList(), avisosVigentes = emptySet())

        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
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
