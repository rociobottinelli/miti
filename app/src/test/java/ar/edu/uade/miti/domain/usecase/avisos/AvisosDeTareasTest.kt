package ar.edu.uade.miti.domain.usecase.avisos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakePendienteRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.PermisosDeAvisos
import ar.edu.uade.miti.domain.model.TareasConAviso
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.AlarmasDeTareas
import ar.edu.uade.miti.domain.repository.SesionRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisosDeTareasTest {

    private val ahora = LocalDateTime.of(2026, 11, 23, 12, 0)

    private fun tarea(id: String, responsable: String = ANA, cuando: LocalDateTime = ahora.plusHours(2), avisar: Boolean = true, resuelto: Boolean = false) =
        Pendiente(id, HOGAR, TipoPendiente.TAREA, "Tarea $id", responsable, cuando, avisar, resuelto = resuelto)

    @Test
    fun `solo suenan las tareas propias, con aviso, sin resolver y que todavia no vencieron`() {
        val tareas = listOf(
            tarea("ok"),
            tarea("de-tomas", responsable = TOMAS),
            tarea("sin-aviso", avisar = false),
            tarea("resuelta", resuelto = true),
            tarea("vencida", cuando = ahora.minusMinutes(1)),
            Pendiente("compra", HOGAR, TipoPendiente.COMPRA, "Leche"),
        )

        assertEquals(listOf("ok"), TareasConAviso.paraProgramar(tareas, ANA, ahora).map { it.id })
    }

    @Test
    fun `sin sesion no suena nada`() {
        assertTrue(TareasConAviso.paraProgramar(listOf(tarea("ok")), null, ahora).isEmpty())
    }

    @Test
    fun `salen ordenadas por hora`() {
        val tarde = tarea("tarde", cuando = ahora.plusHours(5))
        val pronto = tarea("pronto", cuando = ahora.plusMinutes(10))
        assertEquals(listOf("pronto", "tarde"), TareasConAviso.paraProgramar(listOf(tarde, pronto), ANA, ahora).map { it.id })
    }

    private class FakeAlarmas : AlarmasDeTareas {
        var programadas: List<String>? = null
        override suspend fun programarSolo(tareas: List<Pendiente>) { programadas = tareas.map { it.id } }
        override fun permisos() = PermisosDeAvisos(notificaciones = true, alarmasExactas = false)
    }

    private class FakeSesion(private val usuario: Usuario?) : SesionRepository {
        override fun observarUsuario(): kotlinx.coroutines.flow.Flow<Usuario?> = MutableStateFlow(usuario)
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = Resource.Error("no")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = Resource.Error("no")
        override suspend fun cerrarSesion() = Resource.Success(Unit)
    }

    @Test
    fun `reprogramar deja exactamente las alarmas que corresponden`() = runTest {
        val pendientes = FakePendienteRepository()
        pendientes.pendientes.value = listOf(tarea("mia"), tarea("de-tomas", responsable = TOMAS))
        val alarmas = FakeAlarmas()

        ReprogramarAvisosUseCase(pendientes, FakeSesion(Usuario(ANA, "ana@mail.com")), alarmas, relojEn(ahora))()

        assertEquals(listOf("mia"), alarmas.programadas)
    }

    @Test
    fun `al cerrar sesion reprogramar cancela todo`() = runTest {
        val pendientes = FakePendienteRepository()
        pendientes.pendientes.value = listOf(tarea("mia"))
        val alarmas = FakeAlarmas()

        ReprogramarAvisosUseCase(pendientes, FakeSesion(null), alarmas, relojEn(ahora))()

        assertEquals(emptyList<String>(), alarmas.programadas)
    }

    @Test
    fun `los permisos llegan tal cual`() {
        assertEquals(PermisosDeAvisos(true, false), ConsultarPermisosDeAvisosUseCase(FakeAlarmas())())
    }
}
