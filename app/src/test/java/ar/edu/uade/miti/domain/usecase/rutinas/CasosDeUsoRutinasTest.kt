package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.exito
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeHogarRepository
import ar.edu.uade.miti.domain.fakes.FakeRutinaRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.IdsSecuenciales
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.relojEn
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.usecase.rutinas.DeshacerMarcaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.MarcarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.RegistrarRutinaUseCase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CasosDeUsoRutinasTest {

    private val jueves = LocalDate.of(2026, 11, 5)
    private val juevesALas8 = jueves.atTime(8, 15)

    private val repositorio = FakeRutinaRepository()
    private val pancho = Rutina("pancho", HOGAR, "Darle de comer a Pancho", Frecuencia.DIARIA, vecesPorDia = 2)
    private val basura = Rutina(
        "basura", HOGAR, "Sacar la basura", Frecuencia.DIAS_DE_LA_SEMANA, setOf(DayOfWeek.MONDAY), vecesPorDia = 1,
    )

    private fun marcar(rutina: Rutina, quien: String, cuando: LocalDateTime) =
        RegistroRutina("${rutina.id}-$quien-$cuando", rutina.id, quien, cuando)

    @Test
    fun `registrar una rutina diaria la guarda en el hogar activo sin dias`() = runTest {
        val registrar = RegistrarRutinaUseCase(FakeHogarRepository(), repositorio, IdsSecuenciales())

        val rutina = registrar(" Regar las plantas ", Frecuencia.DIARIA, setOf(DayOfWeek.MONDAY), vecesPorDia = 1).exito()

        assertEquals(Rutina("id-1", HOGAR, "Regar las plantas", Frecuencia.DIARIA, emptySet(), 1), rutina)
        assertEquals(listOf(rutina), repositorio.rutinas.value)
    }

    @Test
    fun `solo muestra las rutinas que corresponden al dia`() = runTest {
        repositorio.rutinas.value = listOf(pancho, basura)

        val delJueves = ObservarRutinasDelDiaUseCase(repositorio)(jueves).first()

        assertEquals(listOf(pancho), delJueves.map { it.rutina })
    }

    @Test
    fun `calcula el progreso con las marcas del dia, ordenadas por hora`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val deLaTarde = marcar(pancho, ANA, jueves.atTime(20, 0))
        val deLaManana = marcar(pancho, TOMAS, juevesALas8)
        repositorio.registros.value = listOf(deLaTarde, deLaManana)

        val rutina = ObservarRutinasDelDiaUseCase(repositorio)(jueves).first().single()

        assertEquals(2, rutina.hechas)
        assertTrue(rutina.completa)
        assertEquals(listOf(deLaManana, deLaTarde), rutina.registros)
    }

    @Test
    fun `al dia siguiente la rutina vuelve a estar pendiente`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        repositorio.registros.value = listOf(marcar(pancho, TOMAS, juevesALas8), marcar(pancho, ANA, jueves.atTime(20, 0)))

        val viernes = ObservarRutinasDelDiaUseCase(repositorio)(jueves.plusDays(1)).first().single()

        assertEquals(0, viernes.hechas)
        assertFalse(viernes.completa)
    }

    @Test
    fun `si los dos marcan sin conexion quedan ambas marcas y no pasa del maximo`() = runTest {
        val unaVez = pancho.copy(vecesPorDia = 1)
        repositorio.rutinas.value = listOf(unaVez)
        repositorio.registros.value = listOf(marcar(unaVez, TOMAS, juevesALas8), marcar(unaVez, ANA, juevesALas8))

        val rutina = ObservarRutinasDelDiaUseCase(repositorio)(jueves).first().single()

        assertEquals(2, rutina.registros.size)
        assertEquals(1, rutina.hechas)
        assertTrue(rutina.completa)
    }

    @Test
    fun `marcar registra quien y a que hora`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val marcarRutina = MarcarRutinaUseCase(repositorio, relojEn(juevesALas8), IdsSecuenciales())

        assertTrue(marcarRutina("pancho", TOMAS).exito())

        assertEquals(listOf(RegistroRutina("id-1", "pancho", TOMAS, juevesALas8)), repositorio.registros.value)
    }

    @Test
    fun `marcar una rutina ya completa no agrega otra marca`() = runTest {
        repositorio.rutinas.value = listOf(pancho)
        val marcarRutina = MarcarRutinaUseCase(repositorio, relojEn(juevesALas8), IdsSecuenciales())

        assertTrue(marcarRutina("pancho", TOMAS).exito())
        assertTrue(marcarRutina("pancho", ANA).exito())
        assertFalse(marcarRutina("pancho", TOMAS).exito())

        assertEquals(2, repositorio.registros.value.size)
    }

    @Test
    fun `una rutina de 4 veces por dia nunca se guarda`() = runTest {
        val registrar = RegistrarRutinaUseCase(FakeHogarRepository(), repositorio, IdsSecuenciales())

        val resultado = registrar("Regar", Frecuencia.DIARIA, emptySet(), vecesPorDia = 4)

        assertTrue(resultado is Resource.Error)
        assertEquals(emptyList<Rutina>(), repositorio.rutinas.value)
    }

    @Test
    fun `marcar una rutina que ya no existe devuelve error`() = runTest {
        val marcarRutina = MarcarRutinaUseCase(repositorio, relojEn(juevesALas8), IdsSecuenciales())

        assertTrue(marcarRutina("no-existe", TOMAS) is Resource.Error)
    }

    @Test
    fun `deshacer saca la ultima marca propia de hoy y no toca las del otro`() = runTest {
        val repositorio = FakeRutinaRepository()
        repositorio.registros.value = listOf(
            RegistroRutina("ana-8", "pancho", ANA, juevesALas8.minusHours(1)),
            RegistroRutina("tomas-8", "pancho", TOMAS, juevesALas8),
            RegistroRutina("ana-ayer", "pancho", ANA, juevesALas8.minusDays(1)),
        )
        val deshacer = DeshacerMarcaUseCase(repositorio, relojEn(juevesALas8))

        assertEquals(Resource.Success(true), deshacer("pancho", ANA))
        assertEquals(listOf("tomas-8", "ana-ayer"), repositorio.registros.value.map { it.id })
        // Ya no le queda ninguna marca propia de hoy.
        assertEquals(Resource.Success(false), deshacer("pancho", ANA))
    }
}
