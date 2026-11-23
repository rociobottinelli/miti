package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import javax.inject.Inject

/**
 * Después de ingresar: si la persona ya tenía hogar (por ejemplo, cambió de celular), lo activa y la app
 * va directo a Hoy. Si no, devuelve false y el primer uso sigue en "Tu hogar".
 */
class RecuperarMiHogarUseCase @Inject constructor(private val ingreso: IngresoAHogarRepository) {
    suspend operator fun invoke(): Resource<Boolean> = ingreso.recuperarMiHogar()
}
