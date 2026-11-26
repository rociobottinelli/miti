package ar.edu.uade.miti.presentation.hoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.usecase.sincronizacion.SincronizarAhoraUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.model.EstadoDeSincronizacion
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.model.RutinaDelDia
import ar.edu.uade.miti.domain.usecase.ObservarSiHayItemsUseCase
import ar.edu.uade.miti.domain.usecase.avisos.ConsultarPermisosDeAvisosUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.AlternarTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarProximasTareasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarTareasHechasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ReabrirTareaUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.EliminarPendienteUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.DeshacerMarcaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.EliminarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.AlternarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.rutinas.ToqueDeRutina
import ar.edu.uade.miti.domain.usecase.rutinas.ObservarRutinasDelDiaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
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
    private val alternarRutina: AlternarRutinaUseCase,
    private val observarEstadoDeSincronizacion: ObservarEstadoDeSincronizacionUseCase,
    private val observarItemsSinEnviar: ObservarItemsSinEnviarUseCase,
    private val sincronizarAhora: SincronizarAhoraUseCase,
    private val consultarPermisos: ConsultarPermisosDeAvisosUseCase,
    private val observarTareasHechas: ObservarTareasHechasUseCase,
    private val alternarTarea: AlternarTareaUseCase,
    private val reabrirTarea: ReabrirTareaUseCase,
    private val deshacerMarca: DeshacerMarcaUseCase,
    private val eliminarRutina: EliminarRutinaUseCase,
    private val eliminarPendiente: EliminarPendienteUseCase,
    private val reloj: Clock,
) : ViewModel() {

    /**
     * Los permisos no avisan cuando cambian: se vuelven a mirar cada vez que Hoy vuelve a pantalla
     * ([revisarPermisos]), por ejemplo al volver de la configuración.
     */
    private val permisos = MutableStateFlow(consultarPermisos())

    private val filtro = MutableStateFlow(FiltroDeTareas.TODAS)

    /** Cada valor nuevo vuelve a suscribirse a los datos; lo usa [reintentar]. */
    private val intentos = MutableStateFlow(0)

    /** La rutina que la persona tocó con datos desactualizados, esperando que confirme (C1). */
    private val porConfirmar = MutableStateFlow<String?>(null)

    /** Rutinas que se están marcando ahora: un doble toque no agrega dos marcas. */
    private val marcandoAhora = mutableSetOf<String>()

    /** Lo mismo para las tareas que se están tocando. */
    private val tocandoTareas = mutableSetOf<String>()

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

    /**
     * Tocar una rutina: la tilda si le faltan marcas y la destilda si ya está completa (ver [AlternarRutinaUseCase]).
     * Si los datos están desactualizados (C1: "si la información llega tarde, pueden repetir tareas"), antes de
     * tildar se pregunta: la otra persona pudo haberla hecho y este celular todavía no lo sabe. Para destildar no.
     */
    fun marcar(rutinaId: String) {
        viewModelScope.launch {
            val dia = observarRutinasDelDia(LocalDate.now(reloj)).first().firstOrNull { it.rutina.id == rutinaId }
            val vaATildar = dia == null || !dia.completa
            if (vaATildar && observarEstadoDeSincronizacion().first().desactualizado) {
                porConfirmar.value = rutinaId
            } else {
                alternarYa(rutinaId)
            }
        }
    }

    fun confirmarMarca() {
        val id = porConfirmar.value ?: return
        porConfirmar.value = null
        alternarYa(id)
    }

    fun cancelarMarca() {
        porConfirmar.value = null
    }

    fun revisarPermisos() {
        permisos.value = consultarPermisos()
    }

    /**
     * Tocar una tarea (RF03): si está pendiente la marca como hecha; si ya estaba hecha, la destilda. Una tarea hecha
     * sigue en la lista, tildada, hasta que pasa su fecha, así el mismo toque la deshace. Si estaba atrasada,
     * desaparece al marcarla (ya venció): solo en ese caso el snackbar ofrece "Deshacer".
     */
    fun completar(tareaId: String) {
        if (!tocandoTareas.add(tareaId)) return
        viewModelScope.launch {
            try {
                when (val resultado = alternarTarea(tareaId)) {
                    is Resource.Success -> {
                        val tarea = resultado.data
                        val desaparece = tarea.resuelto && tarea.fechaHora?.toLocalDate()?.isBefore(LocalDate.now(reloj)) == true
                        if (desaparece) _eventos.emit(EventoDeHoy.TareaHecha(tareaId, tarea.titulo))
                    }
                    is Resource.Error -> _eventos.emit(EventoDeHoy.NoSePudo(resultado.message))
                }
            } finally {
                tocandoTareas.remove(tareaId)
            }
        }
    }

    /** El "Deshacer" del snackbar: la tarea vuelve a quedar pendiente. */
    fun reabrir(tareaId: String) {
        viewModelScope.launch {
            val resultado = reabrirTarea(tareaId)
            if (resultado is Resource.Error) _eventos.emit(EventoDeHoy.NoSePudo(resultado.message))
        }
    }

    /** Deshace la última marca propia de hoy (el menú de una rutina a medias, por ejemplo 1 de 2). */
    fun deshacerMarca(rutinaId: String) {
        viewModelScope.launch {
            val yo = observarUsuario().first() ?: return@launch
            val resultado = deshacerMarca(rutinaId, yo.id)
            if (resultado is Resource.Error) _eventos.emit(EventoDeHoy.NoSePudo(resultado.message))
        }
    }

    /** Borra una rutina (ya confirmado en el diálogo). */
    fun borrarRutina(rutinaId: String) {
        viewModelScope.launch {
            val titulo = tituloDeRutina(rutinaId)
            when (val resultado = eliminarRutina(rutinaId)) {
                is Resource.Success -> _eventos.emit(EventoDeHoy.Borrado(titulo))
                is Resource.Error -> _eventos.emit(EventoDeHoy.NoSePudo(resultado.message))
            }
        }
    }

    /** Borra una tarea (ya confirmado en el diálogo). Si tenía aviso, la alarma se cancela sola. */
    fun borrarTarea(tareaId: String) {
        viewModelScope.launch {
            when (val resultado = eliminarPendiente(tareaId)) {
                is Resource.Success -> _eventos.emit(EventoDeHoy.Borrado(resultado.data.titulo))
                is Resource.Error -> _eventos.emit(EventoDeHoy.NoSePudo(resultado.message))
            }
        }
    }

    private suspend fun tituloDeRutina(rutinaId: String): String =
        observarRutinasDelDia(LocalDate.now(reloj)).first().firstOrNull { it.rutina.id == rutinaId }?.rutina?.titulo.orEmpty()

    /** Pide sincronizar ya (botones Reintentar y Actualizar de los avisos). */
    fun sincronizar() {
        viewModelScope.launch { sincronizarAhora() }
    }

    /** Tildar o destildar: el cambio se ve en la tarjeta al instante, así que no hace falta otro aviso. */
    private fun alternarYa(rutinaId: String) {
        if (!marcandoAhora.add(rutinaId)) return
        viewModelScope.launch {
            try {
                val yo = observarUsuario().first()
                val resultado = if (yo == null) null else alternarRutina(rutinaId, yo.id)
                when {
                    resultado !is Resource.Success -> _eventos.emit(EventoDeHoy.NoSePudoMarcar)
                    resultado.data is ToqueDeRutina.MarcadaPorOtro -> {
                        val quien = (resultado.data as ToqueDeRutina.MarcadaPorOtro).integranteId
                        _eventos.emit(EventoDeHoy.MarcadaPorOtro(observarIntegrantes().first().firstOrNull { it.id == quien }?.nombre))
                    }
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
        // Primero las pendientes por fecha y al final las hechas (tildadas), como las rutinas.
        val tareasFiltradas = combine(filtro, personas) { f, p -> f to p }
            .flatMapLatest { (f, p) ->
                val responsable = p.responsableSegun(f)
                combine(observarProximasTareas(responsable), observarTareasHechas(hoy, responsable)) { pendientes, hechas ->
                    pendientes + hechas
                }
            }
        val todasLasTareas = combine(observarProximasTareas(), observarTareasHechas(hoy), ::Pair)

        val contexto = combine(
            combine(personas, filtro, ::Pair),
            observarEstadoDeSincronizacion(), observarItemsSinEnviar(), porConfirmar, permisos,
        ) { (p, f), sync, sinEnviar, confirmar, perm -> Contexto(p, f, sync, sinEnviar, confirmar, AvisoDePermisos.segun(perm)) }

        return combine(
            observarRutinasDelDia(hoy),
            todasLasTareas,
            tareasFiltradas,
            observarSiHayItems(),
            contexto,
        ) { rutinas, (pendientes, hechas), tareas, hayItems, c ->
            val p = c.personas
            val f = c.filtro
            val encabezado = Encabezado(hoy, p.yo, p.otro, AvisoDeSync.segun(c.sync, reloj.zone), c.permisos)
            when {
                !hayItems -> HoyUiState.Vacio(encabezado)
                // Se decide con todas las tareas, no las filtradas: si no, "Mías" sin tareas propias
                // escondería los filtros y no habría forma de volver a "Todo".
                rutinas.isEmpty() && pendientes.isEmpty() && hechas.isEmpty() -> HoyUiState.TodoAlDia(encabezado)
                else -> HoyUiState.Contenido(
                    // Con todo hecho se festeja arriba, pero la lista queda para poder destildar.
                    todoAlDia = rutinas.all { it.completa } && pendientes.isEmpty(),
                    encabezado = encabezado,
                    filtro = f,
                    rutinas = rutinas.map { p.rutinaEnPantalla(it).copy(pendienteDeSync = it.rutina.id in c.sinEnviar) }
                        .sortedBy { it.completa },
                    tareas = tareas.map { p.tareaEnPantalla(it, hoy).copy(pendienteDeSync = it.id in c.sinEnviar) },
                    confirmarMarca = c.porConfirmar?.let { id ->
                        rutinas.firstOrNull { it.rutina.id == id }?.let { r ->
                            ConfirmacionDeMarca(id, r.rutina.titulo, p.otro?.nombre, aHoraLocal(c.sync.ultimaSincronizacion))
                        }
                    },
                )
            }
        }
    }

    private class Contexto(
        val personas: Personas,
        val filtro: FiltroDeTareas,
        val sync: EstadoDeSincronizacion,
        val sinEnviar: Set<String>,
        val porConfirmar: String?,
        val permisos: AvisoDePermisos?,
    )

    private fun aHoraLocal(instante: Instant?): LocalDateTime? = instante?.let { LocalDateTime.ofInstant(it, reloj.zone) }

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
            puedoDeshacer = yo != null && rutina.registros.any { it.integranteId == yo.id },
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
            hecha = tarea.resuelto,
        )
    }
}
