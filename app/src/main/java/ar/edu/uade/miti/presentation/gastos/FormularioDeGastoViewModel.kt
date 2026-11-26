package ar.edu.uade.miti.presentation.gastos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.R
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.DatosDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.domain.usecase.gastos.EditarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.EliminarGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarGastoUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.domain.usecase.pendientes.TerminarCompraUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import ar.edu.uade.miti.presentation.components.leerMontoEnCentavos
import ar.edu.uade.miti.presentation.components.montoEditable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registrar o editar un gasto (#14, RF04). Si la ruta trae un gastoId, edita ese gasto; si trae deCompra,
 * es el gasto de la compra que se está terminando y guardarlo también la cierra (flujo 3). Las validaciones
 * de los campos se hacen acá para mostrarlas al lado de cada campo; las reglas de negocio las vuelve a
 * aplicar el dominio al guardar.
 */
@HiltViewModel
class FormularioDeGastoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val registrarGasto: RegistrarGastoUseCase,
    private val editarGasto: EditarGastoUseCase,
    private val obtenerGasto: ObtenerGastoUseCase,
    private val eliminarGasto: EliminarGastoUseCase,
    private val terminarCompra: TerminarCompraUseCase,
    private val observarIntegrantes: ObservarIntegrantesUseCase,
    private val observarUsuario: ObservarUsuarioUseCase,
) : ViewModel() {

    /** Mismos nombres que las propiedades de Ruta.FormularioDeGasto. */
    private val gastoId: String? = estadoGuardado.get<String>("gastoId")
    private val deCompra: Boolean = estadoGuardado.get<Boolean>("deCompra") ?: false

    private val _uiState = MutableStateFlow(FormularioDeGastoUiState(editando = gastoId != null, deCompra = deCompra))
    val uiState: StateFlow<FormularioDeGastoUiState> = _uiState.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDeFormulario>()
    val eventos: SharedFlow<EventoDeFormulario> = _eventos.asSharedFlow()

    init {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val personas = observarIntegrantes().first().map { Persona(it.id, it.nombre) }
        val yo = observarUsuario().first()?.id
        val editado = gastoId?.let { id ->
            when (val r = obtenerGasto(id)) {
                is Resource.Success -> r.data
                is Resource.Error -> {
                    _uiState.update { it.copy(cargando = false, integrantes = personas, errorDeCarga = R.string.gasto_error_ya_no_existe) }
                    return
                }
            }
        }
        _uiState.update { estado ->
            if (editado == null) {
                estado.copy(cargando = false, integrantes = personas, pagadorId = yo ?: personas.firstOrNull()?.id)
            } else {
                estado.conGasto(editado, personas)
            }
        }
    }

    private fun FormularioDeGastoUiState.conGasto(gasto: Gasto, personas: List<Persona>) = copy(
        cargando = false,
        integrantes = personas,
        monto = montoEditable(gasto.montoTotalCentavos),
        descripcion = gasto.descripcion,
        pagadorId = gasto.pagadorId,
        beneficiarioId = gasto.beneficiarioId,
        enCuotas = gasto.cuotasTotales > 1,
        cuotasTotales = gasto.cuotasTotales.toString(),
        cuotaActual = gasto.cuotaInicial.toString(),
    )

    // --- Lo que escribe o elige la persona. Al cambiar un campo se borra su error. ---

    fun onMontoChange(valor: String) = _uiState.update { it.copy(monto = valor, errores = it.errores.copy(monto = null)) }
    fun onDescripcionChange(valor: String) =
        _uiState.update { it.copy(descripcion = valor, errores = it.errores.copy(descripcion = null)) }
    fun onPagadorChange(id: String) = _uiState.update { it.copy(pagadorId = id) }
    fun onRepartoChange(beneficiarioId: String?) = _uiState.update { it.copy(beneficiarioId = beneficiarioId) }
    fun onEnCuotasChange(valor: Boolean) = _uiState.update { it.copy(enCuotas = valor, errores = it.errores.copy(cuotas = null)) }
    fun onCuotasTotalesChange(valor: String) =
        _uiState.update { it.copy(cuotasTotales = valor.filter(Char::isDigit), errores = it.errores.copy(cuotas = null)) }
    fun onCuotaActualChange(valor: String) =
        _uiState.update { it.copy(cuotaActual = valor.filter(Char::isDigit), errores = it.errores.copy(cuotas = null)) }

    fun onGuardarClick() {
        val estado = _uiState.value
        if (estado.guardando || estado.cargando) return   // evita el doble envío
        val errores = validar(estado)
        if (errores.hayErrores) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        val datos = DatosDeGasto(
            descripcion = estado.descripcion,
            montoTotalCentavos = leerMontoEnCentavos(estado.monto) ?: return,
            pagadorId = estado.pagadorId ?: return,
            reparto = if (estado.beneficiarioId == null) Reparto.A_MEDIAS else Reparto.UNO_SOLO,
            beneficiarioId = estado.beneficiarioId,
            cuotasTotales = if (estado.enCuotas) estado.cuotasTotales.toInt() else 1,
            cuotaInicial = if (estado.enCuotas) estado.cuotaActual.toInt() else 1,
        )
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val resultado = when {
                gastoId != null -> editarGasto(gastoId, datos)
                // El caso de uso crea el gasto y cierra la compra; si falla, la compra queda abierta.
                deCompra -> terminarCompra(datos)
                else -> registrarGasto(datos)
            }
            _uiState.update { it.copy(guardando = false) }
            // "Guardado" recién después de escribir (AGENTS.md §5).
            when (resultado) {
                is Resource.Success -> _eventos.emit(EventoDeFormulario.Guardado)
                is Resource.Error -> _eventos.emit(EventoDeFormulario.NoSePudo(resultado.message))
            }
        }
    }

    fun onBorrarClick() {
        val id = gastoId ?: return
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val resultado = eliminarGasto(id)
            _uiState.update { it.copy(guardando = false) }
            when (resultado) {
                is Resource.Success -> _eventos.emit(EventoDeFormulario.Borrado)
                is Resource.Error -> _eventos.emit(EventoDeFormulario.NoSePudo(resultado.message))
            }
        }
    }

    private fun validar(estado: FormularioDeGastoUiState): ErroresDeGasto {
        val monto = leerMontoEnCentavos(estado.monto)
        val total = estado.cuotasTotales.toIntOrNull()
        val actual = estado.cuotaActual.toIntOrNull()
        return ErroresDeGasto(
            monto = if (monto == null || monto <= 0) R.string.gasto_error_monto else null,
            descripcion = if (estado.descripcion.isBlank()) R.string.gasto_error_descripcion else null,
            cuotas = when {
                !estado.enCuotas -> null
                total == null || total < 2 -> R.string.gasto_error_cuotas_total
                actual == null || actual !in 1..total -> R.string.gasto_error_cuota_actual
                else -> null
            },
        )
    }
}
