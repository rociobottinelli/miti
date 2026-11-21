package ar.edu.uade.miti.datos.repositorio

import ar.edu.uade.miti.datos.local.GastoDao
import ar.edu.uade.miti.datos.local.HogarDao
import ar.edu.uade.miti.datos.local.MetadatosSync
import ar.edu.uade.miti.datos.local.PendienteDao
import ar.edu.uade.miti.datos.local.RutinaDao
import ar.edu.uade.miti.datos.local.toDomain
import ar.edu.uade.miti.datos.local.toEntity
import ar.edu.uade.miti.datos.sesion.HogarActivo
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Hogar
import ar.edu.uade.miti.dominio.modelo.Integrante
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina
import ar.edu.uade.miti.dominio.repositorio.GastoRepository
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepository
import ar.edu.uade.miti.dominio.repositorio.PendienteRepository
import ar.edu.uade.miti.dominio.repositorio.RutinaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// Implementaciones de los contratos del dominio con Room (D05). Cada escritura marca la fila
// como PENDIENTE de sincronizar, con la hora del cambio; el SyncWorker (#18) las va a enviar.

/**
 * Consulta [consulta] sobre el hogar activo y, si cambia el hogar, vuelve a consultar sobre el nuevo.
 * Sin hogar activo devuelve [vacio].
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> HogarActivo.observar(vacio: T, consulta: (hogarId: String) -> Flow<T>): Flow<T> =
    id.flatMapLatest { hogarId -> if (hogarId == null) flowOf(vacio) else consulta(hogarId) }

private fun Clock.cambioPendiente() = MetadatosSync(updatedAt = millis())

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

class PendienteRepositoryImpl @Inject constructor(
    private val dao: PendienteDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : PendienteRepository {
    override fun observarPendientes(): Flow<List<Pendiente>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPendientes(id).map { lista -> lista.map { it.toDomain() } } }

    override suspend fun obtenerPendiente(id: String): Pendiente? = dao.obtenerPendiente(id)?.toDomain()

    override suspend fun guardarPendientes(pendientes: List<Pendiente>) {
        val sync = reloj.cambioPendiente()
        dao.guardarPendientes(pendientes.map { it.toEntity(sync) })
    }

    override suspend fun eliminarPendiente(id: String) = dao.marcarBorrado(id, reloj.millis())
}

class GastoRepositoryImpl @Inject constructor(
    private val dao: GastoDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : GastoRepository {
    override fun observarGastos(): Flow<List<Gasto>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarGastos(id).map { lista -> lista.map { it.toDomain() } } }

    override fun observarPagos(): Flow<List<Pago>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPagos(id).map { lista -> lista.map { it.toDomain() } } }

    override suspend fun guardarGasto(gasto: Gasto) = dao.guardarGasto(gasto.toEntity(reloj.cambioPendiente()))

    override suspend fun eliminarGasto(id: String) = dao.marcarGastoBorrado(id, reloj.millis())

    override suspend fun guardarPago(pago: Pago) = dao.guardarPago(pago.toEntity(reloj.cambioPendiente()))

    override suspend fun eliminarPago(id: String) = dao.marcarPagoBorrado(id, reloj.millis())
}

/** Ids UUID generados en el celular: se pueden crear sin conexión sin chocar con los del otro (D07). */
class GeneradorDeUuid @Inject constructor() : GeneradorDeIds {
    override fun nuevoId(): String = UUID.randomUUID().toString()
}
