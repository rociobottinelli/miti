package ar.edu.uade.miti.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** El estado de la sincronización tal como lo ve la capa de datos, sin reglas de presentación. */
data class SituacionDeSincronizacion(
    val ultimaSincronizacion: Instant?,
    val conConexion: Boolean,
    val sincronizando: Boolean,
    val reintentando: Boolean,
    /** Ids de ítems con cambios sin enviar. Una rutina figura si tiene una marca sin enviar. */
    val idsSinEnviar: Set<String>,
)

interface SincronizacionRepository {
    fun observar(): Flow<SituacionDeSincronizacion>

    /** Pide sincronizar ya (por ejemplo, desde el botón Reintentar). Si no hay red, espera a que vuelva. */
    suspend fun sincronizarAhora()
}
