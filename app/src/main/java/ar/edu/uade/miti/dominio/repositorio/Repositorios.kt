package ar.edu.uade.miti.dominio.repositorio

import ar.edu.uade.miti.dominio.modelo.Gasto
import ar.edu.uade.miti.dominio.modelo.Hogar
import ar.edu.uade.miti.dominio.modelo.Integrante
import ar.edu.uade.miti.dominio.modelo.Pago
import ar.edu.uade.miti.dominio.modelo.Pendiente
import ar.edu.uade.miti.dominio.modelo.RegistroRutina
import ar.edu.uade.miti.dominio.modelo.Rutina
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Contratos del dominio (D05). La capa de datos los implementa con Room + Supabase;
// los tests, con fakes en memoria. Las lecturas son Flow porque Room es la fuente de verdad (D04).
// Todas las operaciones trabajan sobre el hogar activo; borrar es un borrado lógico (D07).

interface HogarRepositorio {
    /** Id del hogar con el que se está trabajando, o null si todavía no hay uno. */
    suspend fun hogarActivoId(): String?
    fun observarHogar(): Flow<Hogar?>
    fun observarIntegrantes(): Flow<List<Integrante>>
}

interface RutinaRepositorio {
    fun observarRutinas(): Flow<List<Rutina>>
    fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>>
    suspend fun guardarRutina(rutina: Rutina)
    suspend fun eliminarRutina(id: String)
    suspend fun agregarRegistro(registro: RegistroRutina)
}

interface PendienteRepositorio {
    fun observarPendientes(): Flow<List<Pendiente>>
    suspend fun obtenerPendiente(id: String): Pendiente?
    suspend fun guardarPendientes(pendientes: List<Pendiente>)
    suspend fun eliminarPendiente(id: String)
}

interface GastoRepositorio {
    fun observarGastos(): Flow<List<Gasto>>
    fun observarPagos(): Flow<List<Pago>>
    suspend fun guardarGasto(gasto: Gasto)
    suspend fun eliminarGasto(id: String)
    suspend fun guardarPago(pago: Pago)
    suspend fun eliminarPago(id: String)
}

/** Genera los ids de las entidades nuevas. En la app son UUID (D07); en los tests, predecibles. */
fun interface GeneradorDeIds {
    fun nuevoId(): String
}

/** Id del hogar activo, o un error si se intenta registrar algo antes de tener hogar. */
suspend fun HogarRepositorio.requerirHogarActivo(): String =
    checkNotNull(hogarActivoId()) { "No hay un hogar activo" }
