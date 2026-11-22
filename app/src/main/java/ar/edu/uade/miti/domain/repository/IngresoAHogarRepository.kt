package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar

/**
 * Entrar a un hogar: crearlo, unirse con el código o recuperar el que ya se tenía (por ejemplo, al
 * ingresar en un celular nuevo). Es lo único del hogar que necesita conexión, porque el código y el
 * límite de dos integrantes los valida el servidor (preentrega, sección 14). Si sale bien, el hogar
 * queda como hogar activo en el celular.
 */
interface IngresoAHogarRepository {
    suspend fun crearHogar(nombreHogar: String): Resource<Hogar>
    suspend fun unirseAHogar(codigo: String): Resource<Hogar>

    /** Busca si la persona ya tiene hogar en el servidor. Devuelve si lo encontró. */
    suspend fun recuperarMiHogar(): Resource<Boolean>
}
