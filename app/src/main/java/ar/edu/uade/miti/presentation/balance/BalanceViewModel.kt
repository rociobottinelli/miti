package ar.edu.uade.miti.presentation.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.miti.R
import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.domain.model.BalanceDelMes
import ar.edu.uade.miti.domain.model.CampoDeGasto
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.Deuda
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.usecase.gastos.DescartarConflictoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObservarConflictosDeGastoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.ObtenerBalanceDelMesUseCase
import ar.edu.uade.miti.domain.usecase.gastos.EliminarPagoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RegistrarPagoUseCase
import ar.edu.uade.miti.domain.usecase.gastos.RehacerMiEdicionUseCase
import ar.edu.uade.miti.domain.usecase.hogar.ObservarIntegrantesUseCase
import ar.edu.uade.miti.presentation.components.leerMontoEnCentavos
import ar.edu.uade.miti.presentation.components.montoEditable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Balance del mes (#15, RF04). Todo sale de Room: al registrar o editar un gasto o un pago (acá o desde
 * el otro celular, vía sincronización) el balance se recalcula solo (D09).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val obtenerBalance: ObtenerBalanceDelMesUseCase,
    private val observarIntegrantes: ObservarIntegrantesUseCase,
    private val observarConflictos: ObservarConflictosDeGastoUseCase,
    private val registrarPago: RegistrarPagoUseCase,
    private val eliminarPago: EliminarPagoUseCase,
    private val rehacerMiEdicion: RehacerMiEdicionUseCase,
    private val descartarConflicto: DescartarConflictoUseCase,
    private val reloj: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(reloj))
    private val pago = MutableStateFlow<PagoEnCurso?>(null)

    /** Cada valor nuevo vuelve a suscribirse a los datos (botón Reintentar). */
    private val intentos = MutableStateFlow(0)

    private val _eventos = MutableSharedFlow<EventoDeBalance>()
    val eventos: SharedFlow<EventoDeBalance> = _eventos.asSharedFlow()

    val uiState: StateFlow<BalanceUiState> = intentos
        .flatMapLatest { datos() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalanceUiState.Cargando)

    private fun datos(): Flow<BalanceUiState> =
        combine(
            mes.flatMapLatest { m -> obtenerBalance(m).map { m to it } },
            observarIntegrantes(),
            observarConflictos(),
            pago,
        ) { (m, balance), integrantes, conflictos, pagoEnCurso ->
            if (balance == null) BalanceUiState.SinOtraPersona
            else armar(m, balance, integrantes, conflictos, pagoEnCurso)
        }.catch { emit(BalanceUiState.Error) }

    private fun armar(
        m: YearMonth,
        balance: BalanceDelMes,
        integrantes: List<Integrante>,
        conflictos: List<ConflictoDeGasto>,
        pagoEnCurso: PagoEnCurso?,
    ): BalanceUiState.Contenido {
        val nombres = integrantes.associate { it.id to it.nombre }
        fun nombre(id: String) = nombres[id] ?: "?"
        fun Deuda.enPantalla() = DeudaEnPantalla(deudorId, nombre(deudorId), acreedorId, nombre(acreedorId), montoCentavos)

        val movimientos = balance.cuotas.map { c ->
            Movimiento.CuotaDeGasto(
                gastoId = c.gasto.id,
                descripcion = c.gasto.descripcion,
                montoCentavos = c.montoCentavos,
                pagador = nombre(c.gasto.pagadorId),
                soloDe = c.gasto.beneficiarioId?.let(::nombre),
                numeroCuota = c.numero,
                cuotasTotales = c.gasto.cuotasTotales,
            )
        } + balance.pagos.sortedBy { it.fecha }.map { p ->
            Movimiento.PagoHecho(p.id, nombre(p.deIntegranteId), nombre(p.aIntegranteId), p.montoCentavos, p.fecha)
        }

        return BalanceUiState.Contenido(
            mes = m,
            puedeAvanzar = m < YearMonth.now(reloj),
            resultado = balance.resultado?.enPantalla(),
            pagadoPor = integrantes.map { persona ->
                TotalDePersona(persona.id, persona.nombre, balance.cuotas.filter { it.gasto.pagadorId == persona.id }.sumOf { it.montoCentavos })
            },
            saldoAnterior = balance.saldoAnterior?.enPantalla(),
            movimientos = movimientos,
            conflictos = conflictos.map { c ->
                fun Gasto.valor(campo: CampoDeGasto): ValorDeCampo = when (campo) {
                    CampoDeGasto.DESCRIPCION -> ValorDeCampo.Texto(descripcion)
                    CampoDeGasto.MONTO -> ValorDeCampo.Pesos(montoTotalCentavos)
                    CampoDeGasto.PAGADOR -> ValorDeCampo.Persona(nombre(pagadorId))
                    CampoDeGasto.REPARTO -> beneficiarioId?.let { ValorDeCampo.SoloDe(nombre(it)) } ?: ValorDeCampo.AMedias
                    CampoDeGasto.CUOTAS -> ValorDeCampo.Cuotas(cuotaInicial, cuotasTotales)
                }
                val diferencias = c.actual?.let { quedo ->
                    c.camposDistintos.map { DiferenciaDeGasto(it, c.miVersion.valor(it), quedo.valor(it)) }
                }.orEmpty()
                ConflictoEnPantalla(c, c.miVersion.descripcion, c.miVersion.montoTotalCentavos, c.actual?.montoTotalCentavos, diferencias)
            },
            pago = pagoEnCurso,
        )
    }

    fun mesAnterior() = mes.update { it.minusMonths(1) }

    fun mesSiguiente() = mes.update { if (it < YearMonth.now(reloj)) it.plusMonths(1) else it }

    fun reintentar() = intentos.update { it + 1 }

    // --- Registrar pago ---

    /** Abre la hoja con la deuda del mes ya cargada: el caso más común es saldarla entera. */
    fun abrirPago() {
        val estado = uiState.value as? BalanceUiState.Contenido ?: return
        val hoy = LocalDate.now(reloj)
        val deuda = estado.resultado
        pago.value = if (deuda != null) {
            PagoEnCurso(deuda.deudorId, deuda.deudor, deuda.acreedorId, deuda.acreedor, montoEditable(deuda.montoCentavos), hoy)
        } else {
            // "Están a mano": se abre igual (por ejemplo, para adelantar), sin monto sugerido.
            val (uno, otro) = estado.pagadoPor.takeIf { it.size >= 2 } ?: return
            PagoEnCurso(uno.id, uno.nombre, otro.id, otro.nombre, "", hoy)
        }
    }

    fun cambiarDireccionDelPago() = pago.update { p -> p?.copy(deId = p.aId, de = p.a, aId = p.deId, a = p.de) }
    fun onMontoDelPagoChange(valor: String) = pago.update { it?.copy(monto = valor, error = null) }
    fun onFechaDelPagoChange(fecha: LocalDate) = pago.update { it?.copy(fecha = fecha) }
    fun cerrarPago() { pago.value = null }

    fun registrarElPago() {
        val actual = pago.value ?: return
        if (actual.guardando) return
        val centavos = leerMontoEnCentavos(actual.monto)
        if (centavos == null || centavos <= 0) {
            pago.update { it?.copy(error = R.string.pago_error_monto) }
            return
        }
        pago.update { it?.copy(guardando = true) }
        viewModelScope.launch {
            when (val r = registrarPago(actual.deId, actual.aId, centavos, actual.fecha)) {
                is Resource.Success -> {
                    pago.value = null
                    _eventos.emit(EventoDeBalance.Mensaje(R.string.pago_registrado))
                }
                is Resource.Error -> {
                    pago.update { it?.copy(guardando = false) }
                    _eventos.emit(EventoDeBalance.NoSePudo(r.message))
                }
            }
        }
    }

    /** Borra un pago cargado por error (ya confirmado en el diálogo). */
    fun borrarPago(pagoId: String) = viewModelScope.launch {
        when (val r = eliminarPago(pagoId)) {
            is Resource.Success -> _eventos.emit(EventoDeBalance.Mensaje(R.string.pago_borrado))
            is Resource.Error -> _eventos.emit(EventoDeBalance.NoSePudo(r.message))
        }
    }

    // --- Conflictos (C2) ---

    fun rehacer(conflicto: ConflictoDeGasto) = viewModelScope.launch {
        when (val r = rehacerMiEdicion(conflicto)) {
            is Resource.Success -> _eventos.emit(EventoDeBalance.Mensaje(R.string.conflicto_rehecho))
            is Resource.Error -> _eventos.emit(EventoDeBalance.NoSePudo(r.message))
        }
    }

    fun descartar(conflicto: ConflictoDeGasto) = viewModelScope.launch {
        if (descartarConflicto(conflicto) is Resource.Error) {
            _eventos.emit(EventoDeBalance.NoSePudo("No se pudo cerrar el aviso. Probá de nuevo."))
        }
    }
}
