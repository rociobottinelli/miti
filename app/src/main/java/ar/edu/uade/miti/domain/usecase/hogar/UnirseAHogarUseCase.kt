package ar.edu.uade.miti.domain.usecase.hogar

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.repository.IngresoAHogarRepository
import javax.inject.Inject

/**
 * Se une al hogar del código. Acepta el código como se dicta o se pega ("k7p 2qx"): lo pasa a mayúsculas
 * y le saca los espacios. Si no tiene la forma de un código, avisa sin llamar al servidor.
 */
class UnirseAHogarUseCase @Inject constructor(private val ingreso: IngresoAHogarRepository) {
    suspend operator fun invoke(codigo: String): Resource<Hogar> {
        val limpio = normalizar(codigo)
        if (limpio.isEmpty()) return Resource.Error("Ingresá el código del hogar")
        if (!formato.matches(limpio)) return Resource.Error("El código tiene 6 letras o números, como K7P2QX")
        return ingreso.unirseAHogar(limpio)
    }

    companion object {
        /** Mismo formato que exige la tabla hogares en Supabase. */
        private val formato = Regex("^[A-Z0-9]{6}$")

        fun normalizar(codigo: String): String = codigo.filterNot { it.isWhitespace() }.uppercase()
    }
}
