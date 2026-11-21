package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.data.local.dao.HogarDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.mapper.toDomain
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.repository.HogarRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HogarRepositoryImpl @Inject constructor(
    private val dao: HogarDao,
    private val hogarActivo: HogarActivo,
) : HogarRepository {
    override suspend fun hogarActivoId(): String? = hogarActivo.id.first()

    override fun observarHogar(): Flow<Hogar?> =
        hogarActivo.observar(null) { id -> dao.observarHogar(id).map { it?.toDomain() } }

    override fun observarIntegrantes(): Flow<List<Integrante>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarIntegrantes(id).map { lista -> lista.map { it.toDomain() } } }
}
