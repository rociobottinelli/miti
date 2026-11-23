package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.data.local.dao.EstadoDeSyncDao
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import ar.edu.uade.miti.data.sync.MonitorDeConexion
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import ar.edu.uade.miti.domain.repository.SincronizacionRepository
import ar.edu.uade.miti.domain.repository.SituacionDeSincronizacion
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Junta las cuatro fuentes del estado de sincronización: DataStore, la red, WorkManager y Room. */
class SincronizacionRepositoryImpl @Inject constructor(
    private val estado: EstadoDeSyncLocal,
    private val conexion: MonitorDeConexion,
    private val programador: ProgramadorDeSync,
    private val dao: EstadoDeSyncDao,
) : SincronizacionRepository {

    override fun observar(): Flow<SituacionDeSincronizacion> =
        combine(
            estado.ultimaSincronizacion,
            conexion.observar(),
            programador.observarTrabajo(),
            dao.observarIdsSinEnviar(),
        ) { ultima, conConexion, trabajo, ids ->
            SituacionDeSincronizacion(
                ultimaSincronizacion = ultima?.let(Instant::ofEpochMilli),
                conConexion = conConexion,
                sincronizando = trabajo.sincronizando,
                reintentando = trabajo.reintentando,
                idsSinEnviar = ids.toSet(),
            )
        }

    override suspend fun sincronizarAhora() = programador.sincronizarAhora()
}
