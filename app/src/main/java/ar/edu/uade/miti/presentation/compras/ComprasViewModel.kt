package ar.edu.uade.miti.presentation.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.Pendiente
import ar.edu.uade.miti.domain.usecase.pendientes.MarcarEnCarritoUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.ObservarListaDeComprasUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.RegistrarCompraUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.TerminarCompraUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarEstadoDeSincronizacionUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.ObservarItemsSinEnviarUseCase
import ar.edu.uade.miti.domain.usecase.sincronizacion.SincronizarAhoraUseCase
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de Compras (D01). La lista sale de Room (D04): funciona igual sin conexión (RNF01) y lo que
 * agrega o tacha la otra persona aparece solo cuando llega por la sincronización.
 *
 * "Terminé la compra" cierra la compra sin gasto. El pop up "¿Registrar el gasto?" se suma cuando esté
 * el formulario de gasto (#44); el caso de uso ya lo admite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComprasViewModel @Inject constructor(
    private val observarLista: ObservarListaDeComprasUseCase,
    private val registrarCompra: RegistrarCompraUseCase,
    private val marcarEnCarrito: MarcarEnCarritoUseCase,
    private val terminarCompra: TerminarCompraUseCase,
    private val observarEstadoDeSincronizacion: ObservarEstadoDeSincronizacionUseCase,
    private val observarItemsSinEnviar: ObservarItemsSinEnviarUseCase,
    private val sincronizarAhora: SincronizarAhoraUseCase,
    private val reloj: Clock,
) : ViewModel() {

    /** Cada valor nuevo vuelve a suscribirse a los datos; lo usa [reintentar]. */
    private val intentos = MutableStateFlow(0)
    private val nuevoItem = MutableStateFlow("")
    private val terminando = MutableStateFlow(false)

    /** Ítems que se están tachando ahora: un doble toque no manda dos cambios. */
    private val tachandoAhora = mutableSetOf<String>()

    private val _eventos = MutableSharedFlow<EventoDeCompras>(extraBufferCapacity = 1)
    val eventos: SharedFlow<EventoDeCompras> = _eventos.asSharedFlow()

    val uiState: StateFlow<ComprasUiState> = intentos
        .flatMapLatest {
            combine(
                observarLista(), observarEstadoDeSincronizacion(), observarItemsSinEnviar(), nuevoItem, terminando,
            ) { lista, sync, sinEnviar, texto, cerrando ->
                fun enPantalla(p: Pendiente) = ItemDeCompra(p.id, p.titulo, pendienteDeSync = p.id in sinEnviar)
                ComprasUiState.Contenido(
                    porComprar = lista.porComprar.map(::enPantalla),
                    enCarrito = lista.enCarrito.map(::enPantalla),
                    nuevoItem = texto,
                    aviso = AvisoDeSync.segun(sync, reloj.zone),
                    hoy = LocalDate.now(reloj),
                    terminando = cerrando,
                ) as ComprasUiState
            }.catch { emit(ComprasUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComprasUiState.Cargando)

    fun reintentar() {
        intentos.value++
    }

    fun escribir(texto: String) {
        nuevoItem.value = texto
    }

    /** Agrega lo escrito. Si se pudo, el campo queda vacío para el siguiente; si no, el texto se conserva. */
    fun agregar() {
        val texto = nuevoItem.value
        if (texto.isBlank()) return
        viewModelScope.launch {
            when (val resultado = registrarCompra(texto)) {
                is Resource.Success -> if (nuevoItem.value == texto) nuevoItem.value = ""
                is Resource.Error -> _eventos.emit(EventoDeCompras.NoSePudo(resultado.message))
            }
        }
    }

    /** Tachar pasa el ítem al carrito; destildarlo lo devuelve a la lista. */
    fun tachar(id: String, enCarrito: Boolean) {
        if (!tachandoAhora.add(id)) return
        viewModelScope.launch {
            try {
                val resultado = marcarEnCarrito(id, enCarrito)
                if (resultado is Resource.Error) _eventos.emit(EventoDeCompras.NoSePudo(resultado.message))
            } finally {
                tachandoAhora.remove(id)
            }
        }
    }

    fun terminar() {
        if (terminando.value) return
        terminando.value = true
        viewModelScope.launch {
            try {
                when (val resultado = terminarCompra(gasto = null)) {
                    is Resource.Success -> _eventos.emit(EventoDeCompras.CompraTerminada)
                    is Resource.Error -> _eventos.emit(EventoDeCompras.NoSePudo(resultado.message))
                }
            } finally {
                terminando.value = false
            }
        }
    }

    /** Botones Reintentar y Actualizar de los avisos de sincronización. */
    fun sincronizar() {
        viewModelScope.launch { sincronizarAhora() }
    }
}
