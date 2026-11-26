package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.core.map
import ar.edu.uade.miti.data.local.dao.ConflictoGastoDao
import ar.edu.uade.miti.data.local.dao.GastoDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.entity.EstadoSync
import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.mapper.miVersion
import ar.edu.uade.miti.data.mapper.toDomain
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
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
    private val conflictos: ConflictoGastoDao,
) : GastoRepository {
    override fun observarGastos(): Flow<List<Gasto>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarGastos(id).map { lista -> lista.map { it.toDomain() } } }

    override fun observarPagos(): Flow<List<Pago>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPagos(id).map { lista -> lista.map { it.toDomain() } } }

    override suspend fun guardarGasto(gasto: Gasto) = avisoDeCambios.escribir {
        val actual = dao.gastosParaSync(gasto.id)
        dao.guardarGasto(gasto.toEntity(reloj.cambioPendiente()).copy(baseUpdatedAt = versionBase(actual)))
    }

    /** Borrar también es editar: lleva la versión base para que un borrado no pise una edición del otro. */
    override suspend fun eliminarGasto(id: String) = avisoDeCambios.escribir {
        val actual = dao.gastosParaSync(id) ?: return@escribir
        dao.guardarGasto(
            actual.copy(
                sync = reloj.cambioPendiente().copy(deleted = true),
                baseUpdatedAt = versionBase(actual),
            ),
        )
    }

    override suspend fun guardarPago(pago: Pago) = avisoDeCambios.escribir { dao.guardarPago(pago.toEntity(reloj.cambioPendiente())) }

    override suspend fun eliminarPago(id: String) = avisoDeCambios.escribir { dao.marcarPagoBorrado(id, reloj.millis()) }

    override suspend fun obtenerGasto(id: String): Resource<Gasto?> =
        enBase(NO_SE_LEYO) { dao.gastosParaSync(id)?.takeUnless { it.sync.deleted }?.toDomain() }

    override fun observarConflictos(): Flow<List<ConflictoDeGasto>> =
        hogarActivo.observar(emptyList()) { hogarId ->
            conflictos.observarAbiertos().map { lista ->
                lista.map { c ->
                    ConflictoDeGasto(
                        id = c.id,
                        actual = dao.gastosParaSync(c.gastoId)?.takeUnless { it.sync.deleted }?.toDomain(),
                        miVersion = c.miVersion(hogarId),
                        eraBorrado = c.eraBorrado,
                    )
                }
            }
        }

    override suspend fun resolverConflicto(id: String): Resource<Unit> =
        enBase(NO_SE_GUARDO) { conflictos.marcarResuelto(id, reloj.millis()) }.map { }

    /**
     * Sobre qué versión del servidor se está editando (C2). Si el gasto estaba sincronizado, es esa versión.
     * Si ya tenía una edición sin subir, se mantiene la base de la primera: esa es la que vio esta persona.
     * Un gasto nuevo no tiene base.
     */
    private fun versionBase(actual: GastoEntity?): Long? = when {
        actual == null -> null
        actual.sync.syncState == EstadoSync.SINCRONIZADO -> actual.sync.updatedAt
        else -> actual.baseUpdatedAt
    }
}
