package ar.edu.uade.miti.data.sync

import androidx.room.withTransaction
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.avisos.AvisoDeTareasNuevas
import ar.edu.uade.miti.data.local.AppDatabase
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import ar.edu.uade.miti.data.local.datastore.HogarActivo
import ar.edu.uade.miti.data.local.datastore.SesionLocal
import ar.edu.uade.miti.data.mapper.toDto
import ar.edu.uade.miti.data.mapper.toEntity
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.SESION_VENCIDA
import ar.edu.uade.miti.data.remote.SyncApiService
import ar.edu.uade.miti.domain.model.TipoPendiente
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import java.time.Clock
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

/** Cómo terminó una sincronización. Le dice al [SyncWorker] si tiene que reintentar. */
sealed interface ResultadoDeSync {
    data class Completa(val subidas: Int, val descargadas: Int, val rechazadas: Int) : ResultadoDeSync
    /** No hay sesión (o venció): no hay nada que hacer hasta que la persona vuelva a ingresar. */
    data object SinSesion : ResultadoDeSync
    /** La persona todavía no creó ni se unió a un hogar. */
    data object SinHogar : ResultadoDeSync
    /** Falló la red o el servidor: lo local queda PENDIENTE y se reintenta con espera creciente (RNF09). */
    data class Reintentar(val motivo: String) : ResultadoDeSync
}

/**
 * Sincroniza Room con Supabase (D06): primero sube lo PENDIENTE y después descarga lo que cambió en el
 * servidor desde la última vez. Room sigue siendo la única fuente de verdad (D04): la UI nunca ve la red,
 * ve las filas que esto escribe en Room.
 *
 * Conflictos (D08): gana la fila con updated_at más nuevo. El servidor ignora subidas más viejas que lo
 * que tiene (migración 3) y, al descargar, una fila remota solo pisa a la local si es igual o más nueva.
 * Las marcas de rutina no tienen conflictos: solo se agregan.
 */
@Singleton
class Sincronizador @Inject constructor(
    private val api: SyncApiService,
    private val configuracion: ConfiguracionSupabase,
    private val proveedorDeToken: ProveedorDeToken,
    private val sesionLocal: SesionLocal,
    private val hogarActivo: HogarActivo,
    private val estado: EstadoDeSyncLocal,
    private val base: AppDatabase,
    private val reloj: Clock,
    private val avisoDeTareasNuevas: AvisoDeTareasNuevas,
) {
    /** Filas por pedido. Un hogar tiene pocas; con esto casi siempre alcanza un pedido por tabla. */
    internal var limite = 500

    private val mutex = Mutex()

    /** Quién usa el celular en esta sincronización (para saber qué tareas nuevas son suyas, #24). */
    private var usuarioId: String? = null

    /** Orden de subida: primero lo que otras filas referencian (un pendiente apunta a su gasto). */
    private val tablas: List<TablaSincronizable<*, *>> by lazy { tablas() }

    suspend fun sincronizar(): ResultadoDeSync = mutex.withLock {
        if (!configuracion.configurado) return@withLock ResultadoDeSync.Reintentar("Servidor sin configurar")
        val usuario = sesionLocal.sesion.first() ?: return@withLock ResultadoDeSync.SinSesion
        val token = when (val t = proveedorDeToken.tokenVigente()) {
            is Resource.Success -> t.data
            is Resource.Error ->
                return@withLock if (t.message == SESION_VENCIDA) ResultadoDeSync.SinSesion else ResultadoDeSync.Reintentar(t.message)
        }
        val autorizacion = "Bearer $token"
        usuarioId = usuario.usuarioId

        try {
            if (hogarActivo.id.first() == null) {
                val hogar = buscarMiHogar(autorizacion, usuario.usuarioId) ?: return@withLock ResultadoDeSync.SinHogar
                hogarActivo.establecer(hogar)
            }

            var subidas = 0
            var rechazadas = 0
            for (tabla in tablas) {
                val (ok, rechazo) = subir(tabla, autorizacion)
                subidas += ok
                rechazadas += rechazo
            }
            var descargadas = 0
            for (tabla in tablas) descargadas += descargar(tabla, autorizacion)

            estado.registrarSincronizacion(reloj.millis())
            ResultadoDeSync.Completa(subidas, descargadas, rechazadas)
        } catch (e: FalloDeSync) {
            ResultadoDeSync.Reintentar(e.message ?: "Error de sincronización")
        } catch (e: JsonEncodingException) {
            ResultadoDeSync.Reintentar("Respuesta inesperada del servidor")
        } catch (e: IOException) {
            ResultadoDeSync.Reintentar("Sin conexión")
        } catch (e: JsonDataException) {
            ResultadoDeSync.Reintentar("Respuesta inesperada del servidor")
        }
    }

    private suspend fun buscarMiHogar(autorizacion: String, usuarioId: String): String? {
        val respuesta = api.buscarIntegrante(autorizacion, "eq.$usuarioId")
        verificar(respuesta, "buscar el hogar")
        return respuesta.body()?.firstOrNull()?.hogarId
    }

    /** Sube las filas PENDIENTE de [tabla]. Devuelve (subidas, rechazadas). */
    private suspend fun <E, D> subir(tabla: TablaSincronizable<E, D>, autorizacion: String): Pair<Int, Int> {
        val subida = tabla.subida ?: return 0 to 0
        val pendientes = subida.porSubir()
        if (pendientes.isEmpty()) return 0 to 0

        val respuesta = subida.subir(autorizacion, pendientes.map(tabla.aDto))
        if (respuesta.isSuccessful) {
            pendientes.forEach { subida.marcarSincronizada(tabla.id(it), tabla.metadatos(it).updatedAt) }
            return pendientes.size to 0
        }
        if (!esRechazoDeDatos(respuesta.code())) verificar(respuesta, "subir ${tabla.nombre}")

        // El servidor rechazó el lote por los datos (por ejemplo, un CHECK). Se sube de a una para que una
        // fila inválida no frene a las demás; la rechazada queda PENDIENTE y se informa en el resultado.
        var subidas = 0
        var rechazadas = 0
        for (fila in pendientes) {
            val individual = subida.subir(autorizacion, listOf(tabla.aDto(fila)))
            when {
                individual.isSuccessful -> {
                    subida.marcarSincronizada(tabla.id(fila), tabla.metadatos(fila).updatedAt)
                    subidas++
                }
                esRechazoDeDatos(individual.code()) -> rechazadas++
                else -> verificar(individual, "subir ${tabla.nombre}")
            }
        }
        return subidas to rechazadas
    }

    /** Descarga lo que cambió en [tabla] desde el último cursor. Devuelve cuántas filas se aplicaron. */
    private suspend fun <E, D> descargar(tabla: TablaSincronizable<E, D>, autorizacion: String): Int {
        val cursor = estado.cursor(tabla.nombre)
        val desde = desdeConMargen(cursor)
        var nuevoCursor = cursor
        var offset = 0
        var aplicadas = 0
        while (true) {
            val respuesta = tabla.descargar(autorizacion, "gte.$desde", limite, offset)
            verificar(respuesta, "descargar ${tabla.nombre}")
            val filas = respuesta.body() ?: throw FalloDeSync("Respuesta vacía al descargar ${tabla.nombre}")

            val aGuardar = mutableListOf<E>()
            val nuevas = mutableListOf<E>()
            base.withTransaction {
                for (dto in filas) {
                    val remota = tabla.aEntidad(dto)
                    val local = tabla.obtenerLocal(tabla.id(remota))
                    if (local == null) nuevas += remota
                    // D08: la remota solo pisa a la local si es igual o más nueva. Si la local es más nueva,
                    // es un cambio PENDIENTE que todavía no subió (o que el servidor va a aceptar).
                    if (local == null || tabla.metadatos(remota).updatedAt >= tabla.metadatos(local).updatedAt) {
                        aGuardar += remota
                    }
                }
                if (aGuardar.isNotEmpty()) tabla.guardarDescargadas(aGuardar)
            }
            aplicadas += aGuardar.size
            if (nuevas.isNotEmpty()) tabla.alBajarNuevas?.invoke(nuevas, cursor == null)
            filas.mapNotNull(tabla.horaDelServidor).maxOrNull()?.let { nuevoCursor = maximo(nuevoCursor, it) }

            if (filas.size < limite) break
            offset += limite
        }
        val final = nuevoCursor
        if (final != null && final != cursor) estado.guardarCursor(tabla.nombre, final)
        return aplicadas
    }

    /**
     * Se vuelve a pedir el último minuto antes del cursor. La hora del servidor es la del inicio de cada
     * transacción, así que un cambio que tardó en confirmarse puede quedar "antes" de uno ya descargado.
     * Repetir filas no hace daño: aplicar una fila igual no cambia nada.
     */
    private fun desdeConMargen(cursor: String?): String =
        cursor?.let { OffsetDateTime.parse(it).minusSeconds(MARGEN_S).toString() } ?: DESDE_SIEMPRE

    private fun maximo(a: String?, b: String): String =
        if (a == null || OffsetDateTime.parse(b).isAfter(OffsetDateTime.parse(a))) b else a

    /** 400, 409, 422: el servidor no acepta esos datos. Reintentar no va a cambiar nada. */
    private fun esRechazoDeDatos(codigo: Int) = codigo == 400 || codigo == 409 || codigo == 422

    private fun verificar(respuesta: Response<*>, que: String) {
        if (!respuesta.isSuccessful) throw FalloDeSync("No se pudo $que (${respuesta.code()})")
    }

    private class FalloDeSync(mensaje: String) : Exception(mensaje)

    private fun tablas(): List<TablaSincronizable<*, *>> {
        val hogares = base.hogarDao()
        val rutinas = base.rutinaDao()
        val gastos = base.gastoDao()
        val pendientes = base.pendienteDao()
        val conflictos = base.conflictoGastoDao()
        return listOf(
            TablaSincronizable(
                nombre = "hogares", aDto = { error("Los hogares no se suben") }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = hogares::hogaresParaSync, guardarDescargadas = hogares::guardarHogaresDescargados,
                descargar = api::descargarHogares,
            ),
            TablaSincronizable(
                nombre = "integrantes", aDto = { error("Los integrantes no se suben") }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = hogares::integrantesParaSync, guardarDescargadas = hogares::guardarIntegrantesDescargados,
                descargar = api::descargarIntegrantes,
            ),
            TablaSincronizable(
                nombre = "rutinas", aDto = { it.toDto() }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = rutinas::rutinasParaSync, guardarDescargadas = rutinas::guardarRutinasDescargados,
                descargar = api::descargarRutinas,
                subida = TablaSincronizable.Subida(rutinas::rutinasPorSubir, rutinas::marcarRutinaSincronizado, api::subirRutinas),
            ),
            TablaSincronizable(
                nombre = "gastos", aDto = { it.toDto() }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = gastos::gastosParaSync, guardarDescargadas = gastos::guardarGastosDescargados,
                descargar = api::descargarGastos,
                subida = TablaSincronizable.Subida(gastos::gastosPorSubir, gastos::marcarGastoSincronizado, api::subirGastos),
            ),
            TablaSincronizable(
                nombre = "conflictos_gasto", aDto = { error("Los conflictos los crea el servidor") },
                aEntidad = { it.toEntity() }, id = { it.id }, metadatos = { it.sync },
                horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = conflictos::conflictosParaSync, guardarDescargadas = conflictos::guardarConflictosDescargados,
                descargar = api::descargarConflictos,
            ),
            TablaSincronizable(
                nombre = "pagos", aDto = { it.toDto() }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = gastos::pagosParaSync, guardarDescargadas = gastos::guardarPagosDescargados,
                descargar = api::descargarPagos,
                subida = TablaSincronizable.Subida(gastos::pagosPorSubir, gastos::marcarPagoSincronizado, api::subirPagos),
            ),
            TablaSincronizable(
                nombre = "pendientes", aDto = { it.toDto() }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = pendientes::pendientesParaSync, guardarDescargadas = pendientes::guardarPendientesDescargados,
                descargar = api::descargarPendientes,
                subida = TablaSincronizable.Subida(
                    pendientes::pendientesPorSubir, pendientes::marcarPendienteSincronizado, api::subirPendientes,
                ),
                alBajarNuevas = { nuevas, primeraVez ->
                    // #24: solo tareas nuevas para esta persona. En la primera descarga (recién ingresó) se baja
                    // todo el hogar de una vez y no se avisa nada: no son "nuevas" para ella.
                    val mias = nuevas.filter {
                        it.tipo == TipoPendiente.TAREA && it.responsableId == usuarioId && !it.resuelto && !it.sync.deleted
                    }
                    if (!primeraVez && mias.isNotEmpty()) avisoDeTareasNuevas.avisar(mias.map { it.titulo })
                },
            ),
            TablaSincronizable(
                nombre = "registros_rutina", aDto = { it.toDto() }, aEntidad = { it.toEntity() },
                id = { it.id }, metadatos = { it.sync }, horaDelServidor = { it.serverUpdatedAt },
                obtenerLocal = rutinas::registrosParaSync, guardarDescargadas = rutinas::guardarRegistrosDescargados,
                descargar = api::descargarRegistros,
                subida = TablaSincronizable.Subida(rutinas::registrosPorSubir, rutinas::marcarRegistroSincronizado, api::subirRegistros),
            ),
        )
    }

    private companion object {
        const val MARGEN_S = 60L
        const val DESDE_SIEMPRE = "1970-01-01T00:00:00Z"
    }
}
