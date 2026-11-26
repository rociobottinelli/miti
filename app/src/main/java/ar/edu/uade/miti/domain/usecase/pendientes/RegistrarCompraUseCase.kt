package ar.edu.uade.miti.domain.usecase.pendientes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.getOrElse
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.TipoPendiente
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.requerirHogarActivo
import ar.edu.uade.miti.domain.usecase.validar
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Agrega un ítem a la lista de compras. Si ya hay uno abierto con el mismo nombre (sin importar
 * mayúsculas ni espacios), no se repite: es el caso de los dos agregando "Leche" (corrección C1).
 */
class RegistrarCompraUseCase @Inject constructor(
    private val hogares: HogarRepository,
    private val pendientes: PendienteRepository,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(titulo: String): Resource<Pendiente> {
        val hogarId = hogares.requerirHogarActivo().getOrElse { return it }
        val abiertas = pendientes.observarPendientes().first().filter { it.tipo == TipoPendiente.COMPRA && !it.resuelto }
        if (abiertas.any { mismoNombre(it.titulo, titulo) }) return Resource.Error(YA_ESTA_EN_LA_LISTA)
        val compra = validar { Pendiente(ids.nuevoId(), hogarId, TipoPendiente.COMPRA, titulo.trim()) }
            .getOrElse { return it }
        return pendientes.guardarPendientes(listOf(compra)).map { compra }
    }

    companion object {
        const val YA_ESTA_EN_LA_LISTA = "Ya está en la lista"

        private fun normalizar(nombre: String) = nombre.trim().replace(Regex("\\s+"), " ").lowercase()

        fun mismoNombre(a: String, b: String) = normalizar(a) == normalizar(b)
    }
}
