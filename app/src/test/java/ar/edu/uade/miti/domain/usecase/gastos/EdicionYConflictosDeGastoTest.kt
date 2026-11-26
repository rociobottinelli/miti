package ar.edu.uade.miti.domain.usecase.gastos

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.fakes.ANA
import ar.edu.uade.miti.domain.fakes.FakeGastoRepository
import ar.edu.uade.miti.domain.fakes.HOGAR
import ar.edu.uade.miti.domain.fakes.TOMAS
import ar.edu.uade.miti.domain.fakes.exito
import ar.edu.uade.miti.domain.model.CampoDeGasto
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdicionYConflictosDeGastoTest {

    private val gastos = FakeGastoRepository()
    private val heladera = Gasto("g1", HOGAR, "Heladera", 3_000_000, TOMAS, Reparto.A_MEDIAS, cuotasTotales = 3, mesInicio = YearMonth.of(2026, 11))

    @Test
    fun `editar cambia los datos y conserva id, hogar y mes`() = runTest {
        gastos.gastos.value = listOf(heladera)

        val editado = EditarGastoUseCase(gastos)(
            "g1", DatosDeGasto(" Heladera nueva ", 3_300_000, ANA, Reparto.A_MEDIAS, cuotasTotales = 3),
        ).exito()

        assertEquals(heladera.copy(descripcion = "Heladera nueva", montoTotalCentavos = 3_300_000, pagadorId = ANA), editado)
        assertEquals(listOf(editado), gastos.gastos.value)
    }

    @Test
    fun `una edicion invalida no se guarda`() = runTest {
        gastos.gastos.value = listOf(heladera)

        val resultado = EditarGastoUseCase(gastos)("g1", DatosDeGasto("Heladera", 0, TOMAS, Reparto.A_MEDIAS))

        assertTrue(resultado is Resource.Error)
        assertEquals(listOf(heladera), gastos.gastos.value)
    }

    @Test
    fun `editar un gasto que ya no existe avisa`() = runTest {
        assertEquals(
            Resource.Error("El gasto ya no existe"),
            EditarGastoUseCase(gastos)("nada", DatosDeGasto("X", 100, TOMAS, Reparto.A_MEDIAS)),
        )
    }

    @Test
    fun `rehacer mi edicion la vuelve a guardar y cierra el conflicto`() = runTest {
        val ganadora = heladera.copy(montoTotalCentavos = 3_100_000)
        val mia = heladera.copy(montoTotalCentavos = 2_900_000)
        gastos.gastos.value = listOf(ganadora)
        val conflicto = ConflictoDeGasto("c1", ganadora, mia, eraBorrado = false)
        gastos.conflictos.value = listOf(conflicto)

        assertEquals(Resource.Success(Unit), RehacerMiEdicionUseCase(gastos)(conflicto))

        assertEquals(listOf(mia), gastos.gastos.value)
        assertTrue(gastos.conflictos.value.isEmpty())
    }

    @Test
    fun `rehacer un borrado vuelve a borrar el gasto`() = runTest {
        gastos.gastos.value = listOf(heladera)
        val conflicto = ConflictoDeGasto("c1", heladera, heladera, eraBorrado = true)
        gastos.conflictos.value = listOf(conflicto)

        RehacerMiEdicionUseCase(gastos)(conflicto)

        assertTrue(gastos.gastos.value.isEmpty())
    }

    @Test
    fun `descartar deja la version que gano y cierra el conflicto`() = runTest {
        val ganadora = heladera.copy(montoTotalCentavos = 3_100_000)
        gastos.gastos.value = listOf(ganadora)
        val conflicto = ConflictoDeGasto("c1", ganadora, heladera, eraBorrado = false)
        gastos.conflictos.value = listOf(conflicto)

        DescartarConflictoUseCase(gastos)(conflicto)

        assertEquals(listOf(ganadora), gastos.gastos.value)
        assertTrue(gastos.conflictos.value.isEmpty())
    }

    @Test
    fun `si no se pudo rehacer, el conflicto sigue abierto`() = runTest {
        val conflicto = ConflictoDeGasto("c1", heladera, heladera.copy(montoTotalCentavos = 1), eraBorrado = false)
        gastos.conflictos.value = listOf(conflicto)
        gastos.falla = Resource.Error("No se pudo guardar")

        assertEquals(Resource.Error("No se pudo guardar"), RehacerMiEdicionUseCase(gastos)(conflicto))
        assertEquals(listOf(conflicto), gastos.conflictos.value)
    }

    // --- Qué campos cambiaron (#51) ---

    @Test
    fun `un conflicto por quien pago no dice que cambio el monto`() {
        val conflicto = ConflictoDeGasto("c1", heladera.copy(pagadorId = ANA), heladera, eraBorrado = false)

        assertEquals(listOf(CampoDeGasto.PAGADOR), conflicto.camposDistintos)
    }

    @Test
    fun `pasar de a medias a uno solo es un cambio de reparto`() {
        val mia = heladera.copy(reparto = Reparto.UNO_SOLO, beneficiarioId = TOMAS)

        assertEquals(listOf(CampoDeGasto.REPARTO), ConflictoDeGasto("c1", heladera, mia, eraBorrado = false).camposDistintos)
    }

    @Test
    fun `cambiar de quien es un gasto de uno solo tambien es un cambio de reparto`() {
        val deTomas = heladera.copy(reparto = Reparto.UNO_SOLO, beneficiarioId = TOMAS)
        val deAna = deTomas.copy(beneficiarioId = ANA)

        assertEquals(listOf(CampoDeGasto.REPARTO), ConflictoDeGasto("c1", deAna, deTomas, eraBorrado = false).camposDistintos)
    }

    @Test
    fun `varios campos distintos salen en el orden del formulario`() {
        val mia = heladera.copy(descripcion = "Heladera nueva", montoTotalCentavos = 2_900_000, cuotaInicial = 2)
        val conflicto = ConflictoDeGasto("c1", heladera.copy(pagadorId = ANA), mia, eraBorrado = false)

        assertEquals(
            listOf(CampoDeGasto.DESCRIPCION, CampoDeGasto.MONTO, CampoDeGasto.PAGADOR, CampoDeGasto.CUOTAS),
            conflicto.camposDistintos,
        )
    }

    @Test
    fun `si el gasto ya no esta no hay campos para comparar`() {
        assertTrue(ConflictoDeGasto("c1", actual = null, miVersion = heladera, eraBorrado = false).camposDistintos.isEmpty())
    }

    @Test
    fun `un conflicto cuyas dos versiones ya coinciden no se muestra`() = runTest {
        val gastos = FakeGastoRepository()
        val otroMonto = heladera.copy(montoTotalCentavos = heladera.montoTotalCentavos + 100)
        gastos.conflictos.value = listOf(
            ConflictoDeGasto("igual", heladera, heladera, eraBorrado = false),
            ConflictoDeGasto("distinto", heladera, otroMonto, eraBorrado = false),
            ConflictoDeGasto("borrado", heladera, heladera, eraBorrado = true),
            ConflictoDeGasto("ya-no-esta", actual = null, miVersion = heladera, eraBorrado = false),
        )

        val visibles = ObservarConflictosDeGastoUseCase(gastos)().first().map { it.id }

        assertEquals(listOf("distinto", "borrado", "ya-no-esta"), visibles)
    }
}
