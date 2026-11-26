package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RutinaRepository {
    fun observarRutinas(): Flow<List<Rutina>>
    fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>>
    suspend fun guardarRutina(rutina: Rutina): Resource<Unit>
    suspend fun eliminarRutina(id: String): Resource<Unit>
    suspend fun agregarRegistro(registro: RegistroRutina): Resource<Unit>

    /** Deshace una marca (borrado lógico): deja de contar en los dos celulares. */
    suspend fun quitarRegistro(id: String): Resource<Unit>
}
