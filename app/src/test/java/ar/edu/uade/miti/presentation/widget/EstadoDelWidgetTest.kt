package ar.edu.uade.miti.presentation.widget

import app.cash.turbine.test
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakeRutinaRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.model.EstadoDeSincronizacion
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.model.RutinaDelDia
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.SesionRepository
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import ar.edu.uade.miti.domain.repository.SituacionDeSincronizacion
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.presentation.hoy.Marca
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoDelWidgetTest {

    private val miercoles = LocalDate.of(2026, 9, 30)
    private val integrantes = listOf(Integrante(ANA, HOGAR, "Ana", "ana@mail.com"), Integrante(TOMAS, HOGAR, "Tomás", "tomas@mail.com"))
    private val yo = Usuario(ANA, "ana@mail.com")
    private val alDia = EstadoDeSincronizacion(Instant.EPOCH, conConexion = true, sincronizando = false, reintentando = false, cambiosSinEnviar = 0, desactualizado = false)

    private val pancho = Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2)
    private val basura = Rutina("basura", HOGAR, "Sacar la basura", Frecuencia.DIARIA, vecesPorDia = 1)
    private val plantas = Rutina("plantas", HOGAR, "Regar las plantas", Frecuencia.DIARIA, vecesPorDia = 1)

    private fun marca(rutina: Rutina, quien: String, hora: LocalTime) = RegistroRutina("${rutina.id}-$hora", rutina.id, quien, miercoles.atTime(hora))

    private fun estado(rutinas: List<RutinaDelDia>, usuario: Usuario? = yo, sync: EstadoDeSincronizacion = alDia, sinEnviar: Set<String> = emptySet()) =
        EstadoDelWidget.desde(usuario, integrantes, rutinas, sync, sinEnviar, miercoles, ZoneOffset.UTC)

    @Test
    fun `sin sesion invita a abrir la app`() {
        assertEquals(EstadoDelWidget.SinHogar, estado(listOf(RutinaDelDia(basura, emptyList())), usuario = null))
    }

    @Test
    fun `sin rutinas hoy muestra el estado vacio`() {
        assertEquals(EstadoDelWidget.SinRutinas, estado(emptyList()))
    }

    @Test
    fun `las que faltan van primero y cada una dice quien la hizo y cuando`() {
        val e = estado(
            listOf(
                RutinaDelDia(basura, listOf(marca(basura, ANA, LocalTime.of(7, 40)))),
                RutinaDelDia(pancho, listOf(marca(pancho, TOMAS, LocalTime.of(8, 15)))),
                RutinaDelDia(plantas, emptyList()),
            ),
        ) as EstadoDelWidget.ConRutinas

        assertEquals(listOf("pancho", "plantas", "basura"), e.rutinas.map { it.id })
        assertEquals(Marca("Tomás", LocalTime.of(8, 15)), e.rutinas[0].ultimaMarca)
        assertEquals(1, e.rutinas[0].hechas)
        assertNull(e.rutinas[1].ultimaMarca)
        assertEquals(1, e.hechas)
        assertEquals(3, e.total)
        assertEquals("pancho", e.proxima?.id)
        assertFalse(e.todoAlDia)
    }

    @Test
    fun `con todo hecho esta al dia y no hay proxima`() {
        val e = estado(listOf(RutinaDelDia(basura, listOf(marca(basura, ANA, LocalTime.of(7, 40)))))) as EstadoDelWidget.ConRutinas

        assertTrue(e.todoAlDia)
        assertNull(e.proxima)
    }

    @Test
    fun `sin conexion avisa y marca lo que no se envio`() {
        val sinRed = alDia.copy(conConexion = false, ultimaSincronizacion = miercoles.atTime(14, 32).toInstant(ZoneOffset.UTC))
        val e = estado(listOf(RutinaDelDia(basura, emptyList())), sync = sinRed, sinEnviar = setOf("basura")) as EstadoDelWidget.ConRutinas

        assertEquals(AvisoDeSync.SinConexion(miercoles.atTime(14, 32)), e.aviso)
        assertTrue(e.rutinas.single().pendienteDeSync)
    }

    @Test
    fun `a la medianoche las rutinas se reinician solas`() {
        val planificador = TestCoroutineScheduler()
        runTest(planificador) {
            val reloj = object : Clock() {
                private val base = miercoles.atTime(23, 59).toInstant(ZoneOffset.UTC)
                override fun getZone(): ZoneId = ZoneOffset.UTC
                override fun withZone(zone: ZoneId?): Clock = this
                override fun instant(): Instant = base.plusMillis(planificador.currentTime)
            }
            val rutinas = FakeRutinaRepository().apply {
                this.rutinas.value = listOf(basura)
                registros.value = listOf(marca(basura, ANA, LocalTime.of(7, 40)))
            }
            val sync = object : SincronizacionRepository {
                override fun observar(): Flow<SituacionDeSincronizacion> =
                    MutableStateFlow(SituacionDeSincronizacion(Instant.EPOCH, true, false, false, emptySet()))
                override suspend fun sincronizarAhora() = Unit
            }
            val observar = ObservarEstadoDelWidget(
                ObservarUsuarioUseCase(SesionFija(yo)),
                ObservarIntegrantesUseCase(FakeHogarRepository()),
                ObservarRutinasDelDiaUseCase(rutinas),
                ObservarEstadoDeSincronizacionUseCase(sync, reloj),
                ObservarItemsSinEnviarUseCase(sync),
                reloj,
            )

            observar().test {
                assertTrue((awaitItem() as EstadoDelWidget.ConRutinas).todoAlDia)
                val jueves = awaitItem() as EstadoDelWidget.ConRutinas
                assertEquals(miercoles.plusDays(1), jueves.hoy)
                assertFalse(jueves.todoAlDia)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private class SesionFija(usuario: Usuario) : SesionRepository {
        private val usuario = MutableStateFlow<Usuario?>(usuario)
        override fun observarUsuario(): Flow<Usuario?> = usuario
        override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String) = error("no se usa")
        override suspend fun iniciarSesion(email: String, contrasenia: String) = error("no se usa")
        override suspend fun cerrarSesion() = error("no se usa")
    }
}
