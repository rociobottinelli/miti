package ar.edu.uade.miti.dominio.casouso.rutinas

import ar.edu.uade.miti.dominio.fakes.ANA
import ar.edu.uade.miti.dominio.fakes.FakeHogarRepositorio
import ar.edu.uade.miti.dominio.fakes.FakeRutinaRepositorio
import ar.edu.uade.miti.dominio.fakes.HOGAR
import ar.edu.uade.miti.dominio.fakes.IdsSecuenciales
import ar.edu.uade.miti.dominio.fakes.TOMAS
import ar.edu.uade.miti.dominio.fakes.relojEn
import ar.edu.uade.miti.dominio.modelo.Frecuencia
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class CasosDeUsoRutinasTest {

    private val jueves = LocalDate.of(2026, 11, 5)
    private val juevesALas8 = jueves.atTime(8, 15)

    private val repositorio = FakeRutinaRepositorio()
    private val pancho = Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2)
    private val basura = Rutina(
        "basura", HOGAR, "Sacar la basura", Frecuencia.DIAS_DE_LA_SEMANA, setOf(DayOfWeek.MONDAY), vecesPorDia = 1,
    )

    private fun marcar(rutina: Rutina, quien: String, cuando: LocalDateTime) =
        RegistroRutina("${rutina.id}-$quien-$cuando", rutina.id, quien, cuando)

    @Test
    fun `registrar una rutina diaria la guarda en el hogar activo sin dias`() = runTest {
        val registrar = RegistrarRutina(FakeHogarRepositorio(), repositorio, IdsSecuenciales())

        val rutina = registrar(" Regar las plantas ", Frecuencia.DIARIA, setOf(DayOfWeek.MONDAY), vecesPorDia = 1)

        assertEquals(Rutina("id-1", HOGAR, "Regar las plantas", Frecuencia.DIARIA, emptySet(), 1), rutina)
        assertEquals(listOf(rutina), repositorio.rutinas.value)
    }

    @Test
    fun `solo muestra las rutinas que corresponden al dia`() = runTest {
        repositorio.rutinas.value = listOf(pancho, basura)

        val delJueves = ObservarRutinasDelDia(repositorio)(jueves).first()

        assertEquals(listOf(pancho), delJueves.map { it.rutina })
    }

    @Test
    fun `calcula el progreso con las marcas del dia, ordenadas por hora`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val deLaTarde = marcar(pancho, ANA, jueves.atTime(20, 0))
        val deLaManana = marcar(pancho, TOMAS, juevesALas8)
        repositorio.registros.value = listOf(deLaTarde, deLaManana)

        val rutina = ObservarRutinasDelDia(repositorio)(jueves).first().single()

        assertEquals(2, rutina.hechas)
        assertTrue(rutina.completa)
        assertEquals(listOf(deLaManana, deLaTarde), rutina.registros)
    }

    @Test
    fun `al dia siguiente la rutina vuelve a estar pendiente`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        repositorio.registros.value = listOf(marcar(pancho, TOMAS, juevesALas8), marcar(pancho, ANA, jueves.atTime(20, 0)))

        val viernes = ObservarRutinasDelDia(repositorio)(jueves.plusDays(1)).first().single()

        assertEquals(0, viernes.hechas)
        assertFalse(viernes.completa)
    }

    @Test
    fun `si los dos marcan sin conexion quedan ambas marcas y no pasa del maximo`() = runTest {
        val unaVez = pancho.copy(vecesPorDia = 1)
        repositorio.rutinas.value = listOf(unaVez)
        repositorio.registros.value = listOf(marcar(unaVez, TOMAS, juevesALas8), marcar(unaVez, ANA, juevesALas8))

        val rutina = ObservarRutinasDelDia(repositorio)(jueves).first().single()

        assertEquals(2, rutina.registros.size)
        assertEquals(1, rutina.hechas)
        assertTrue(rutina.completa)
    }

    @Test
    fun `marcar registra quien y a que hora`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val marcarRutina = MarcarRutina(repositorio, relojEn(juevesALas8), IdsSecuenciales())

        assertTrue(marcarRutina("pancho", TOMAS))

        assertEquals(listOf(RegistroRutina("id-1", "pancho", TOMAS, juevesALas8)), repositorio.registros.value)
    }

    @Test
    fun `marcar una rutina ya completa no agrega otra marca`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val marcarRutina = MarcarRutina(repositorio, relojEn(juevesALas8), IdsSecuenciales())

        assertTrue(marcarRutina("pancho", TOMAS))
        assertTrue(marcarRutina("pancho", ANA))
        assertFalse(marcarRutina("pancho", TOMAS))

        assertEquals(2, repositorio.registros.value.size)
    }
}
