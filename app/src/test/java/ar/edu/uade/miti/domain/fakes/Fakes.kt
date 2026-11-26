package ar.edu.uade.miti.domain.fakes

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Hogar
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.Pago
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.RegistroRutina
import ar.edu.uade.miti.domain.model.Rutina
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.domain.repository.GastoRepository
import ar.edu.uade.miti.domain.repository.GeneradorDeIds
import ar.edu.uade.miti.domain.repository.HogarRepository
import ar.edu.uade.miti.domain.repository.PendienteRepository
import ar.edu.uade.miti.domain.repository.RutinaRepository
import ar.edu.uade.miti.domain.repository.SesionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.fail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Repositorios en memoria para probar los casos de uso sin Room ni red (D05).
// Cada uno guarda sus datos en un MutableStateFlow, así las lecturas se actualizan solas como con Room.

const val HOGAR = "h1"

const val ANA = "ana"

const val TOMAS = "tomas"

class FakeHogarRepository(
    integrantes: List<Integrante> = listOf(
        Integrante(ANA, HOGAR, "Ana", "ana@mail.com"),
        Integrante(TOMAS, HOGAR, "Tomás", "tomas@mail.com"),
    ),
    private val hogarActivo: String? = HOGAR,
) : HogarRepository {
    val integrantes = MutableStateFlow(integrantes)
    override suspend fun hogarActivoId() = hogarActivo
    override fun observarHogar(): Flow<Hogar?> =
        MutableStateFlow(hogarActivo?.let { Hogar(it, "Casa", "ABC123") })
    override fun observarIntegrantes(): Flow<List<Integrante>> = integrantes
}

open class FakeRutinaRepository : FallaConfigurable(), RutinaRepository {
    val rutinas = MutableStateFlow<List<Rutina>>(emptyList())
    val registros = MutableStateFlow<List<RegistroRutina>>(emptyList())

    override fun observarRutinas(): Flow<List<Rutina>> = rutinas
    override fun observarRegistrosDelDia(fecha: LocalDate): Flow<List<RegistroRutina>> =
        registros.map { todos -> todos.filter { it.fechaHora.toLocalDate() == fecha } }
    override suspend fun guardarRutina(rutina: Rutina) = escribir { rutinas.update { reemplazar(it, rutina) { r -> r.id } } }
    override suspend fun eliminarRutina(id: String) = escribir { rutinas.update { lista -> lista.filterNot { it.id == id } } }
    override suspend fun agregarRegistro(registro: RegistroRutina) = escribir { registros.update { it + registro } }
    override suspend fun quitarRegistro(id: String) = escribir { registros.update { lista -> lista.filterNot { it.id == id } } }
}

class FakePendienteRepository : FallaConfigurable(), PendienteRepository {
    val pendientes = MutableStateFlow<List<Pendiente>>(emptyList())

    override fun observarPendientes(): Flow<List<Pendiente>> = pendientes
    override suspend fun obtenerPendiente(id: String): Resource<Pendiente?> =
        falla ?: Resource.Success(pendientes.value.find { it.id == id })
    override suspend fun guardarPendientes(pendientes: List<Pendiente>) = escribir { this.pendientes.update { actuales -> pendientes.fold(actuales) { lista, p -> reemplazar(lista, p) { it.id } } } }
    override suspend fun eliminarPendiente(id: String) = escribir { pendientes.update { lista -> lista.filterNot { it.id == id } } }
}

class FakeGastoRepository : FallaConfigurable(), GastoRepository {
    val gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val pagos = MutableStateFlow<List<Pago>>(emptyList())

    override fun observarGastos(): Flow<List<Gasto>> = gastos
    override fun observarPagos(): Flow<List<Pago>> = pagos
    override suspend fun guardarGasto(gasto: Gasto) = escribir { gastos.update { reemplazar(it, gasto) { g -> g.id } } }
    override suspend fun eliminarGasto(id: String) = escribir { gastos.update { lista -> lista.filterNot { it.id == id } } }
    override suspend fun guardarPago(pago: Pago) = escribir { pagos.update { reemplazar(it, pago) { p -> p.id } } }
    override suspend fun eliminarPago(id: String) = escribir { pagos.update { lista -> lista.filterNot { it.id == id } } }

    val conflictos = MutableStateFlow<List<ConflictoDeGasto>>(emptyList())
    override suspend fun obtenerGasto(id: String): Resource<Gasto?> = falla ?: Resource.Success(gastos.value.find { it.id == id })
    override fun observarConflictos(): Flow<List<ConflictoDeGasto>> = conflictos
    override suspend fun resolverConflicto(id: String) = escribir { conflictos.update { lista -> lista.filterNot { it.id == id } } }
}

/** Ids predecibles: "id-1", "id-2", ... */
class IdsSecuenciales : GeneradorDeIds {
    private var siguiente = 1
    override fun nuevoId() = "id-${siguiente++}"
}

/** Reloj fijo en [momento], para que los tests no dependan de la hora real. */
fun relojEn(momento: LocalDateTime): Clock = Clock.fixed(momento.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

private fun <T> reemplazar(lista: List<T>, nuevo: T, id: (T) -> String): List<T> =
    lista.filterNot { id(it) == id(nuevo) } + nuevo

/**
 * Base de los fakes que escriben: si [falla] no es null, todas las escrituras la devuelven sin guardar
 * nada. Sirve para probar qué pasa cuando la base del celular falla.
 */
abstract class FallaConfigurable {
    var falla: Resource.Error? = null

    protected inline fun escribir(escritura: () -> Unit): Resource<Unit> =
        falla ?: Resource.Success(escritura())
}

/** El dato de un [Resource.Success]; si llegó un error, el test falla mostrando cuál. */
fun <T> Resource<T>.exito(): T = when (this) {
    is Resource.Success -> data
    is Resource.Error -> fail("Se esperaba Success y llegó $this") as Nothing
}

/** Sesión en memoria: por defecto, Ana logueada. */
class FakeSesionRepository(usuario: Usuario? = Usuario(ANA, "ana@mail.com")) : SesionRepository {
    val usuario = MutableStateFlow(usuario)
    override fun observarUsuario(): Flow<Usuario?> = usuario
    override suspend fun crearCuenta(nombre: String, email: String, contrasenia: String): Resource<Usuario> = Resource.Error("No disponible")
    override suspend fun iniciarSesion(email: String, contrasenia: String): Resource<Usuario> = Resource.Error("No disponible")
    override suspend fun cerrarSesion(): Resource<Unit> = Resource.Success(Unit).also { usuario.value = null }
}
