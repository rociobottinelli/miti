package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.data.remote.dto.ConflictoGastoDto
import ar.edu.uade.miti.data.remote.dto.GastoDto
import ar.edu.uade.miti.data.remote.dto.HogarDto
import ar.edu.uade.miti.data.remote.dto.IntegranteDto
import ar.edu.uade.miti.data.remote.dto.PagoDto
import ar.edu.uade.miti.data.remote.dto.PendienteDto
import ar.edu.uade.miti.data.remote.dto.RegistroRutinaDto
import ar.edu.uade.miti.data.remote.dto.RutinaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API REST de Supabase (PostgREST) para sincronizar. Las reglas RLS hacen que cada usuario solo reciba
 * y pueda escribir filas de su hogar, así que las consultas no necesitan filtrar por hogar.
 *
 * Descargar: `desde` es un filtro de PostgREST, por ejemplo "gte.2026-11-21T20:00:00Z".
 * Subir: "upsert" (si el id existe, actualiza). `columns` fija las columnas que se escriben, así una fila
 * con un campo en null no hace que se ignore ese campo en las demás, y server_updated_at nunca se manda.
 */
interface SyncApiService {

    @GET("rest/v1/integrantes")
    suspend fun buscarIntegrante(
        @Header("Authorization") autorizacion: String,
        @Query("id") id: String,
        @Query("deleted") deleted: String = "is.false",
    ): Response<List<IntegranteDto>>

    @GET("rest/v1/hogares")
    suspend fun descargarHogares(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<HogarDto>>

    @GET("rest/v1/integrantes")
    suspend fun descargarIntegrantes(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<IntegranteDto>>

    @GET("rest/v1/rutinas")
    suspend fun descargarRutinas(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<RutinaDto>>

    @POST("rest/v1/rutinas")
    suspend fun subirRutinas(
        @Header("Authorization") autorizacion: String,
        @Body filas: List<RutinaDto>,
        @Query("columns") columnas: String = "id,hogar_id,titulo,frecuencia,dias,veces_por_dia,updated_at,deleted",
        @Header("Prefer") preferencia: String = UPSERT,
    ): Response<Unit>

    @GET("rest/v1/registros_rutina")
    suspend fun descargarRegistros(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<RegistroRutinaDto>>

    /** Las marcas nunca se editan (D08): si el id ya existe, se ignora. */
    @POST("rest/v1/registros_rutina")
    suspend fun subirRegistros(
        @Header("Authorization") autorizacion: String,
        @Body filas: List<RegistroRutinaDto>,
        @Query("columns") columnas: String = "id,rutina_id,integrante_id,fecha_hora,updated_at,deleted",
        @Header("Prefer") preferencia: String = SOLO_NUEVOS,
    ): Response<Unit>

    @GET("rest/v1/pendientes")
    suspend fun descargarPendientes(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<PendienteDto>>

    @POST("rest/v1/pendientes")
    suspend fun subirPendientes(
        @Header("Authorization") autorizacion: String,
        @Body filas: List<PendienteDto>,
        @Query("columns") columnas: String =
            "id,hogar_id,tipo,titulo,responsable_id,fecha_hora,avisar,en_carrito,resuelto,gasto_id,updated_at,deleted",
        @Header("Prefer") preferencia: String = UPSERT,
    ): Response<Unit>

    @GET("rest/v1/gastos")
    suspend fun descargarGastos(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<GastoDto>>

    @POST("rest/v1/gastos")
    suspend fun subirGastos(
        @Header("Authorization") autorizacion: String,
        @Body filas: List<GastoDto>,
        @Query("columns") columnas: String =
            "id,hogar_id,descripcion,monto_total_centavos,pagador_id,reparto,beneficiario_id,cuotas_totales,cuota_inicial,mes_inicio,updated_at,deleted,base_updated_at",
        @Header("Prefer") preferencia: String = UPSERT,
    ): Response<Unit>

    @GET("rest/v1/pagos")
    suspend fun descargarPagos(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<PagoDto>>

    @POST("rest/v1/pagos")
    suspend fun subirPagos(
        @Header("Authorization") autorizacion: String,
        @Body filas: List<PagoDto>,
        @Query("columns") columnas: String =
            "id,hogar_id,de_integrante_id,a_integrante_id,monto_centavos,fecha,updated_at,deleted",
        @Header("Prefer") preferencia: String = UPSERT,
    ): Response<Unit>

    /** Solo trae los conflictos propios: la regla RLS filtra por la persona logueada. */
    @GET("rest/v1/conflictos_gasto")
    suspend fun descargarConflictos(
        @Header("Authorization") autorizacion: String,
        @Query("server_updated_at") desde: String,
        @Query("limit") limite: Int,
        @Query("offset") offset: Int,
        @Query("order") orden: String = ORDEN,
    ): Response<List<ConflictoGastoDto>>

    companion object {
        /** Orden estable para paginar: por hora del servidor y, si empatan, por id. */
        const val ORDEN = "server_updated_at.asc,id.asc"
        const val UPSERT = "resolution=merge-duplicates,return=minimal"
        const val SOLO_NUEVOS = "resolution=ignore-duplicates,return=minimal"
    }
}
