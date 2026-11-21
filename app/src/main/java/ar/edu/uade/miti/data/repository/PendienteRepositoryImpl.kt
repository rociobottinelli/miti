package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.dao.PendienteDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.data.mapper.toDomain
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.repository.PendienteRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendienteRepositoryImpl @Inject constructor(
    private val dao: PendienteDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
    private val avisoDeCambios: AvisoDeCambios,
) : PendienteRepository {
    override fun observarPendientes(): Flow<List<Pendiente>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPendientes(id).map { lista -> lista.map { it.toDomain() } } }

    override suspend fun obtenerPendiente(id: String): Resource<Pendiente?> =
        enBase(NO_SE_LEYO) { dao.obtenerPendiente(id)?.toDomain() }

    override suspend fun guardarPendientes(pendientes: List<Pendiente>) = avisoDeCambios.escribir {
        val sync = reloj.cambioPendiente()
        dao.guardarPendientes(pendientes.map { it.toEntity(sync) })
    }

    override suspend fun eliminarPendiente(id: String) = avisoDeCambios.escribir { dao.marcarBorrado(id, reloj.millis()) }
}
