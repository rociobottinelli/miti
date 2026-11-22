package ar.edu.uade.miti.domain.repository

import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RutinaRepository {
    fun observarRutinas(): Flow<List<Rutina>>
    fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>>
    suspend fun guardarRutina(rutina: Rutina)
    suspend fun eliminarRutina(id: String)
    suspend fun agregarRegistro(registro: RegistroRutina)
}
