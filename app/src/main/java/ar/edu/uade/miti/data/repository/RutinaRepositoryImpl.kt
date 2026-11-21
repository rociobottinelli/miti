package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.data.local.dao.RutinaDao
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.mapper.toDomain
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.repository.RutinaRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RutinaRepositoryImpl @Inject constructor(
    private val dao: RutinaDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : RutinaRepository {
    override fun observarRutinas(): Flow<List<Rutina>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarRutinas(id).map { lista -> lista.map { it.toDomain() } } }

    override fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>> =
        hogarActivo.observar(emptyList()) { id ->
            dao.observarRegistros(id, desde = fecha.atStartOfDay(), hasta = fecha.plusDays(1).atStartOfDay())
                .map { lista -> lista.map { it.toDomain() } }
        }

    override suspend fun guardarRutina(rutina: Rutina) = dao.guardarRutina(rutina.toEntity(reloj.cambioPendiente()))

    override suspend fun eliminarRutina(id: String) = dao.marcarBorrada(id, reloj.millis())

    override suspend fun agregarRegistro(registro: RegistroRutina) =
        dao.guardarRegistro(registro.toEntity(reloj.cambioPendiente()))
}
