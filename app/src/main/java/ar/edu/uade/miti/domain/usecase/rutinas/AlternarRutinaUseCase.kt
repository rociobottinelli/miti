package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Qué pasó al tocar una rutina. */
sealed interface ToqueDeRutina {
    data object Marcada : ToqueDeRutina
    data object Desmarcada : ToqueDeRutina

    /** Estaba hecha, pero solo por la otra persona: su marca dice "quién y cuándo" y no se toca. */
    data class MarcadaPorOtro(val integranteId: String) : ToqueDeRutina
}

/**
 * Tocar una rutina, desde la app o el widget (D03: la misma regla en los dos lados). Si le faltan marcas, suma la
 * de [integranteId]; si ya está completa, la destilda deshaciendo la última marca propia de hoy. Así un toque
 * sin querer se arregla con otro toque.
 */
class AlternarRutinaUseCase @Inject constructor(
    private val rutinas: RutinaRepository,
    private val marcar: MarcarRutinaUseCase,
    private val deshacer: DeshacerMarcaUseCase,
    private val reloj: Clock,
) {
    suspend operator fun invoke(rutinaId: String, integranteId: String): Resource<ToqueDeRutina> {
        val rutina = rutinas.observarRutinas().first().find { it.id == rutinaId }
            ?: return Resource.Error("La rutina ya no existe")
        val marcasDeHoy = rutinas.observarRegistrosDelDia(LocalDate.now(reloj)).first().filter { it.rutinaId == rutinaId }
        if (marcasDeHoy.size < rutina.vecesPorDia) {
            return marcar(rutinaId, integranteId).map { ToqueDeRutina.Marcada }
        }
        if (marcasDeHoy.none { it.integranteId == integranteId }) {
            return Resource.Success(ToqueDeRutina.MarcadaPorOtro(marcasDeHoy.maxBy { it.fechaHora }.integranteId))
        }
        return deshacer(rutinaId, integranteId).map { ToqueDeRutina.Desmarcada }
    }
}
