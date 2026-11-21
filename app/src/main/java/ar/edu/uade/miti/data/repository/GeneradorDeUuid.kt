package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import java.util.UUID
import javax.inject.Inject

/** Ids UUID generados en el celular: se pueden crear sin conexión sin chocar con los del otro (D07). */
class GeneradorDeUuid @Inject constructor() : GeneradorDeIds {
    override fun nuevoId(): String = UUID.randomUUID().toString()
}
