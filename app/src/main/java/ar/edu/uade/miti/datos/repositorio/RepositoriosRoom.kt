package ar.edu.uade.miti.datos.repositorio

import ar.edu.uade.miti.datos.local.GastoDao
import ar.edu.uade.miti.datos.local.HogarDao
import ar.edu.uade.miti.datos.local.MetadatosSync
import ar.edu.uade.miti.datos.local.PendienteDao
import ar.edu.uade.miti.datos.local.RutinaDao
import ar.edu.uade.miti.datos.local.aDominio
import ar.edu.uade.miti.datos.local.aEntidad
import ar.edu.uade.miti.datos.sesion.HogarActivo
import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Hogar
import ar.edu.uade.miti.dominio.modelo.Integrante
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina
import ar.edu.uade.miti.dominio.repositorio.GastoRepositorio
import ar.edu.uade.miti.dominio.repositorio.GeneradorDeIds
import ar.edu.uade.miti.dominio.repositorio.HogarRepositorio
import ar.edu.uade.miti.dominio.repositorio.PendienteRepositorio
import ar.edu.uade.miti.dominio.repositorio.RutinaRepositorio
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

class HogarRepositorioRoom @Inject constructor(
    private val dao: HogarDao,
    private val hogarActivo: HogarActivo,
) : HogarRepositorio {
    override suspend fun hogarActivoId(): String? = hogarActivo.id.first()

    override fun observarHogar(): Flow<Hogar?> =
        hogarActivo.observar(null) { id -> dao.observarHogar(id).map { it?.aDominio() } }

    override fun observarIntegrantes(): Flow<List<Integrante>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarIntegrantes(id).map { lista -> lista.map { it.aDominio() } } }
}

class RutinaRepositorioRoom @Inject constructor(
    private val dao: RutinaDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : RutinaRepositorio {
    override fun observarRutinas(): Flow<List<Rutina>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarRutinas(id).map { lista -> lista.map { it.aDominio() } } }

    override fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>> =
        hogarActivo.observar(emptyList()) { id ->
            dao.observarRegistros(id, desde = fecha.atStartOfDay(), hasta = fecha.plusDays(1).atStartOfDay())
                .map { lista -> lista.map { it.aDominio() } }
        }

    override suspend fun guardarRutina(rutina: Rutina) = dao.guardarRutina(rutina.aEntidad(reloj.cambioPendiente()))

    override suspend fun eliminarRutina(id: String) = dao.marcarBorrada(id, reloj.millis())

    override suspend fun agregarRegistro(registro: RegistroRutina) =
        dao.guardarRegistro(registro.aEntidad(reloj.cambioPendiente()))
}

class PendienteRepositorioRoom @Inject constructor(
    private val dao: PendienteDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : PendienteRepositorio {
    override fun observarPendientes(): Flow<List<Pendiente>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPendientes(id).map { lista -> lista.map { it.aDominio() } } }

    override suspend fun obtenerPendiente(id: String): Pendiente? = dao.obtenerPendiente(id)?.aDominio()

    override suspend fun guardarPendientes(pendientes: List<Pendiente>) {
        val sync = reloj.cambioPendiente()
        dao.guardarPendientes(pendientes.map { it.aEntidad(sync) })
    }

    override suspend fun eliminarPendiente(id: String) = dao.marcarBorrado(id, reloj.millis())
}

class GastoRepositorioRoom @Inject constructor(
    private val dao: GastoDao,
    private val hogarActivo: HogarActivo,
    private val reloj: Clock,
) : GastoRepositorio {
    override fun observarGastos(): Flow<List<Gasto>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarGastos(id).map { lista -> lista.map { it.aDominio() } } }

    override fun observarPagos(): Flow<List<Pago>> =
        hogarActivo.observar(emptyList()) { id -> dao.observarPagos(id).map { lista -> lista.map { it.aDominio() } } }

    override suspend fun guardarGasto(gasto: Gasto) = dao.guardarGasto(gasto.aEntidad(reloj.cambioPendiente()))

    override suspend fun eliminarGasto(id: String) = dao.marcarGastoBorrado(id, reloj.millis())

    override suspend fun guardarPago(pago: Pago) = dao.guardarPago(pago.aEntidad(reloj.cambioPendiente()))

    override suspend fun eliminarPago(id: String) = dao.marcarPagoBorrado(id, reloj.millis())
}

/** Ids UUID generados en el celular: se pueden crear sin conexión sin chocar con los del otro (D07). */
class GeneradorDeUuid @Inject constructor() : GeneradorDeIds {
    override fun nuevoId(): String = UUID.randomUUID().toString()
}
