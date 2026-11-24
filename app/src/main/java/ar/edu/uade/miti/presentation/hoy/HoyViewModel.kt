package ar.edu.uade.miti.presentation.hoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.RutinaDelDia
import ar.edu.uade.miti.domain.usecase.ObservarSiHayItemsUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarProximasTareasUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.MarcarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de Hoy (D01): junta las rutinas del día, las próximas tareas y los integrantes en un único
 * [uiState]. Todo sale de Room a través de los casos de uso (D04), así que cualquier cambio, venga de
 * esta pantalla, del widget o de la sincronización, se refleja solo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HoyViewModel @Inject constructor(
    private val observarRutinasDelDia: ObservarRutinasDelDiaUseCase,
    private val observarProximasTareas: ObservarProximasTareasUseCase,
    private val observarSiHayItems: ObservarSiHayItemsUseCase,
    private val observarIntegrantes: ObservarIntegrantesUseCase,
    private val observarUsuario: ObservarUsuarioUseCase,
    private val marcarRutina: MarcarRutinaUseCase,
    private val reloj: Clock,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroDeTareas.TODAS)

    /** Cada valor nuevo vuelve a suscribirse a los datos; lo usa [reintentar]. */
    private val intentos = MutableStateFlow(0)

    /** Rutinas que se están marcando ahora: un doble toque no agrega dos marcas. */
    private val marcandoAhora = mutableSetOf<String>()

    private val _eventos = MutableSharedFlow<EventoDeHoy>(extraBufferCapacity = 1)
    val eventos: SharedFlow<EventoDeHoy> = _eventos.asSharedFlow()

    val uiState: StateFlow<HoyUiState> = intentos
        .flatMapLatest {
            fechaDeHoy().flatMapLatest { hoy -> estadoPara(hoy) }
                .catch { emit(HoyUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoyUiState.Cargando)

    fun elegirFiltro(nuevo: FiltroDeTareas) {
        filtro.value = nuevo
    }

    fun reintentar() {
        intentos.value++
    }

    fun marcar(rutinaId: String) {
        if (!marcandoAhora.add(rutinaId)) return
        viewModelScope.launch {
            try {
                val yo = observarUsuario().first()
                val resultado = if (yo == null) null else marcarRutina(rutinaId, yo.id)
                when {
                    resultado is Resource.Success && !resultado.data -> _eventos.emit(EventoDeHoy.RUTINA_YA_COMPLETA)
                    resultado !is Resource.Success -> _eventos.emit(EventoDeHoy.NO_SE_PUDO_MARCAR)
                }
            } finally {
                marcandoAhora.remove(rutinaId)
            }
        }
    }

    /**
     * La fecha de hoy, y la nueva a la medianoche. Así las rutinas se reinician aunque la app quede
     * abierta toda la noche, sin ningún proceso que borre nada (D09).
     */
    private fun fechaDeHoy(): Flow<LocalDate> = flow {
        while (true) {
            val ahora = LocalDateTime.now(reloj)
            emit(ahora.toLocalDate())
            val medianoche = ahora.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(ahora, medianoche).toMillis() + 1)
        }
    }.distinctUntilChanged()

    private fun estadoPara(hoy: LocalDate): Flow<HoyUiState> {
        val personas = combine(observarUsuario(), observarIntegrantes()) { usuario, integrantes ->
            Personas(usuario?.id, integrantes)
        }
        val tareasFiltradas = combine(filtro, personas) { f, p -> f to p }
            .flatMapLatest { (f, p) -> observarProximasTareas(p.responsableSegun(f)) }

        return combine(
            observarRutinasDelDia(hoy),
            observarProximasTareas(),
            tareasFiltradas,
            observarSiHayItems(),
            combine(personas, filtro) { p, f -> p to f },
        ) { rutinas, todasLasTareas, tareas, hayItems, (p, f) ->
            val encabezado = Encabezado(hoy, p.yo, p.otro)
            when {
                !hayItems -> HoyUiState.Vacio(encabezado)
                // Se decide con todas las tareas, no las filtradas: si no, "Mías" sin tareas propias
                // escondería los filtros y no habría forma de volver a "Todo".
                rutinas.all { it.completa } && todasLasTareas.isEmpty() -> HoyUiState.TodoAlDia(encabezado)
                else -> HoyUiState.Contenido(
                    encabezado = encabezado,
                    filtro = f,
                    rutinas = rutinas.map { p.rutinaEnPantalla(it) }.sortedBy { it.completa },
                    tareas = tareas.map { p.tareaEnPantalla(it, hoy) },
                )
            }
        }
    }

    /** Los integrantes con su posición (su color) y quién es quién desde este celular. */
    private class Personas(yoId: String?, integrantes: List<Integrante>) {
        private val porId = integrantes.mapIndexed { posicion, integrante ->
            integrante.id to IntegranteEnPantalla(integrante.id, integrante.nombre, posicion)
        }.toMap()
        val yo = yoId?.let { porId[it] }
        val otro = porId.values.firstOrNull { it.id != yoId }.takeIf { yo != null }

        fun responsableSegun(filtro: FiltroDeTareas): String? = when (filtro) {
            FiltroDeTareas.TODAS -> null
            FiltroDeTareas.MIAS -> yo?.id
            FiltroDeTareas.DEL_OTRO -> otro?.id
        }

        fun rutinaEnPantalla(rutina: RutinaDelDia) = RutinaDeHoy(
            id = rutina.rutina.id,
            titulo = rutina.rutina.titulo,
            hechas = rutina.hechas,
            vecesPorDia = rutina.rutina.vecesPorDia,
            ultimaMarca = rutina.registros.maxByOrNull { it.fechaHora }?.let {
                Marca(porId[it.integranteId]?.nombre, it.fechaHora.toLocalTime())
            },
        )

        fun tareaEnPantalla(tarea: Pendiente, hoy: LocalDate) = TareaDeHoy(
            id = tarea.id,
            titulo = tarea.titulo,
            // Una tarea siempre tiene fecha (lo valida Pendiente).
            cuando = cuandoVence(requireNotNull(tarea.fechaHora), hoy),
            responsable = tarea.responsableId?.let { porId[it] },
            conAviso = tarea.avisar,
        )
    }
}
