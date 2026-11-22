package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.data.local.dao.GastoDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.data.mapper.toDomain
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Pago
import ar.edu.uade.miti.domain.repository.GastoRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GastoRepositoryImpl @Inject constructor(
    private val dao: GastoDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
    private val avisoDeCambios: AvisoDeCambios,
) : GastoRepository {
    override fun observarGastos(): Flow<List<Gasto>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarGastos(id).map { lista -> lista.map { it.toDomain() } } }

    override fun observarPagos(): Flow<List<Pago>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPagos(id).map { lista -> lista.map { it.toDomain() } } }

    override suspend fun guardarGasto(gasto: Gasto) = avisoDeCambios.escribir { dao.guardarGasto(gasto.toEntity(reloj.cambioPendiente())) }

    override suspend fun eliminarGasto(id: String) = avisoDeCambios.escribir { dao.marcarGastoBorrado(id, reloj.millis()) }

    override suspend fun guardarPago(pago: Pago) = avisoDeCambios.escribir { dao.guardarPago(pago.toEntity(reloj.cambioPendiente())) }

    override suspend fun eliminarPago(id: String) = avisoDeCambios.escribir { dao.marcarPagoBorrado(id, reloj.millis()) }
}
