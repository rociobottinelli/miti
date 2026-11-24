package ar.edu.uade.miti.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Qué tan al día está este celular con el otro (C1, C4). Lo muestran Hoy, Compras y Balance:
 * "Sin conexión · última sincronización hh:mm", el aviso de error con Reintentar y, en cada ítem,
 * el ícono de pendiente de sincronizar (RNF09).
 */
data class EstadoDeSincronizacion(
    /** Última sincronización completa, o null si este celular nunca sincronizó. */
    val ultimaSincronizacion: Instant?,
    val conConexion: Boolean,
    val sincronizando: Boolean,
    /** El último intento falló y se va a reintentar solo (los cambios siguen guardados en el celular). */
    val reintentando: Boolean,
    /** Cambios hechos en este celular que todavía no llegaron al servidor. */
    val cambiosSinEnviar: Int,
    /** Ver [DatosDesactualizados]. */
    val desactualizado: Boolean,
)

/**
 * Regla de "datos desactualizados" (C4): lo que se ve puede no incluir lo que hizo la otra persona.
 *
 * Están desactualizados si nunca se sincronizó o si la última sincronización completa fue hace más de
 * [UMBRAL]. Con la app abierta y conexión, se sincroniza al abrirla, después de cada cambio y cada
 * 15 minutos, así que pasar el umbral indica un problema (sin conexión o un error que se repite).
 *
 * Qué se muestra: el banner con la hora de la última sincronización y, en las acciones que se pueden
 * duplicar (marcar una rutina), un aviso de que la otra persona pudo haberla hecho ya.
 */
object DatosDesactualizados {
    val UMBRAL: Duration = Duration.ofMinutes(30)

    fun segun(ultimaSincronizacion: Instant?, ahora: Instant): Boolean =
        ultimaSincronizacion == null || Duration.between(ultimaSincronizacion, ahora) > UMBRAL
}
