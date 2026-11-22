package ar.edu.uade.miti.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Espejo exacto de las tablas de Supabase (supabase/migrations/…0001_tablas.sql), en snake_case.
// server_updated_at solo llega al descargar: al subir no se manda (no está en las columnas de la subida).

@JsonClass(generateAdapter = true)
data class HogarDto(
    val id: String,
    val nombre: String,
    @Json(name = "codigo_union") val codigoUnion: String,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class IntegranteDto(
    val id: String,
    @Json(name = "hogar_id") val hogarId: String,
    val nombre: String,
    val email: String,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class RutinaDto(
    val id: String,
    @Json(name = "hogar_id") val hogarId: String,
    val titulo: String,
    val frecuencia: String,
    /** "MONDAY,THURSDAY", igual que en Room. */
    val dias: String,
    @Json(name = "veces_por_dia") val vecesPorDia: Int,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class RegistroRutinaDto(
    val id: String,
    @Json(name = "rutina_id") val rutinaId: String,
    @Json(name = "integrante_id") val integranteId: String,
    /** Hora local del celular que marcó: "2026-11-21T08:15:00". */
    @Json(name = "fecha_hora") val fechaHora: String,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class PendienteDto(
    val id: String,
    @Json(name = "hogar_id") val hogarId: String,
    val tipo: String,
    val titulo: String,
    @Json(name = "responsable_id") val responsableId: String?,
    @Json(name = "fecha_hora") val fechaHora: String?,
    val avisar: Boolean,
    @Json(name = "en_carrito") val enCarrito: Boolean,
    val resuelto: Boolean,
    @Json(name = "gasto_id") val gastoId: String?,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class GastoDto(
    val id: String,
    @Json(name = "hogar_id") val hogarId: String,
    val descripcion: String,
    @Json(name = "monto_total_centavos") val montoTotalCentavos: Long,
    @Json(name = "pagador_id") val pagadorId: String,
    val reparto: String,
    @Json(name = "beneficiario_id") val beneficiarioId: String?,
    @Json(name = "cuotas_totales") val cuotasTotales: Int,
    @Json(name = "cuota_inicial") val cuotaInicial: Int,
    /** "2026-11". */
    @Json(name = "mes_inicio") val mesInicio: String,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class PagoDto(
    val id: String,
    @Json(name = "hogar_id") val hogarId: String,
    @Json(name = "de_integrante_id") val deIntegranteId: String,
    @Json(name = "a_integrante_id") val aIntegranteId: String,
    @Json(name = "monto_centavos") val montoCentavos: Long,
    /** "2026-11-20". */
    val fecha: String,
    @Json(name = "updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @Json(name = "server_updated_at") val serverUpdatedAt: String? = null,
)
