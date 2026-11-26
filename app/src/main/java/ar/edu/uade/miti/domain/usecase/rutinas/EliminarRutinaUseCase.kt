package ar.edu.uade.miti.domain.usecase.rutinas

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.repository.RutinaRepository
import javax.inject.Inject

/** Borra una rutina (borrado lógico, D07): deja de aparecer en Hoy y en el widget de los dos celulares. */
class EliminarRutinaUseCase @Inject constructor(private val rutinas: RutinaRepository) {
    suspend operator fun invoke(id: String): Resource<Unit> = rutinas.eliminarRutina(id)
}
