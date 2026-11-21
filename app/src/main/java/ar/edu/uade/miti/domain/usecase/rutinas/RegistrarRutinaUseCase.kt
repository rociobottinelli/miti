package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.RutinaRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import java.time.DayOfWeek
import javax.inject.Inject

class RegistrarRutinaUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val rutinas: RutinaRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(
        titulo: String,
        frecuencia: Frecuencia,
        dias: Set<DayOfWeek>,
        vecesPorDia: Int,
    ): Rutina {
        val rutina = Rutina(
            id = ids.nuevoId(),
            hogarId = hogares.requerirHogarActivo(),
            titulo = titulo.trim(),
            frecuencia = frecuencia,
            dias = if (frecuencia == Frecuencia.DIARIA) emptySet() else dias,
            vecesPorDia = vecesPorDia,
        )
        rutinas.guardarRutina(rutina)
        return rutina
    }
}
