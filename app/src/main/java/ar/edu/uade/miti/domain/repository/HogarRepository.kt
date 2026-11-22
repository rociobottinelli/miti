package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.model.Integrante
import kotlinx.coroutines.flow.Flow

// Contratos del dominio (D05). La capa de datos los implementa con Room + Supabase;
// los tests, con fakes en memoria. Las lecturas son Flow porque Room es la fuente de verdad (D04).
// Todas las operaciones trabajan sobre el hogar activo; borrar es un borrado lógico (D07).

interface HogarRepository {
    /** Id del hogar con el que se está trabajando, o null si todavía no hay uno. */
    suspend fun hogarActivoId(): String?
    fun observarHogar(): Flow<Hogar?>
    fun observarIntegrantes(): Flow<List<Integrante>>
}

/** Id del hogar activo, o un error si se intenta registrar algo antes de tener hogar. */
suspend fun HogarRepository.requerirHogarActivo(): Resource<String> =
    hogarActivoId()?.let { Resource.Success(it) } ?: Resource.Error("Todavía no hay un hogar activo")
