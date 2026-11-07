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

interface HogarRepositorio {
    fun observarHogar(): Flow<Hogar?>
    fun observarIntegrantes(): Flow<List<Integrante>>
}

interface RutinaRepositorio {
    fun observarRutinas(): Flow<List<Rutina>>
    fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>>
    suspend fun guardarRutina(rutina: Rutina)
    suspend fun agregarRegistro(registro: RegistroRutina)
}

interface PendienteRepositorio {
    fun observarPendientes(): Flow<List<Pendiente>>
    suspend fun guardarPendiente(pendiente: Pendiente)
}

interface GastoRepositorio {
    fun observarGastos(): Flow<List<Gasto>>
    fun observarPagos(): Flow<List<Pago>>
    suspend fun guardarGasto(gasto: Gasto)
    suspend fun guardarPago(pago: Pago)
}
