package ar.edu.uade.miti.domain.usecase.sincronizacion

import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import javax.inject.Inject

/** Botón Reintentar del aviso de error de sincronización. */
class SincronizarAhoraUseCase @Inject constructor(private val sincronizacion: SincronizacionRepository) {
    suspend operator fun invoke() = sincronizacion.sincronizarAhora()
}
