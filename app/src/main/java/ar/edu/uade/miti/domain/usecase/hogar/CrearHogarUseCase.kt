package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import javax.inject.Inject

/** Crea el hogar de quien usa el celular. Devuelve el hogar con su código para compartir. */
class CrearHogarUseCase @Inject constructor(private val ingreso: IngresoAHogarRepository) {
    suspend operator fun invoke(nombreHogar: String): Resource<Hogar> {
        if (nombreHogar.isBlank()) return Resource.Error("Ponele un nombre al hogar")
        return ingreso.crearHogar(nombreHogar.trim())
    }
}
