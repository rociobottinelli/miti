package ar.edu.uade.miti.presentation.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.presentation.components.EsqueletoDeCarga
import ar.edu.uade.miti.presentation.components.EstadoVacio
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Conecta Balance con su ViewModel y con la navegación (#15). */
@Composable
fun BalanceScreen(
    alRegistrarGasto: () -> Unit,
    alEditarGasto: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoDeBalance.Mensaje -> snackbar.showSnackbar(recursos.getString(evento.texto))
                is EventoDeBalance.NoSePudo -> snackbar.showSnackbar(evento.mensaje)
            }
        }
    }

    BalanceContenido(
        estado = estado,
        acciones = AccionesDeBalance(
            mesAnterior = viewModel::mesAnterior,
            mesSiguiente = viewModel::mesSiguiente,
            registrarGasto = alRegistrarGasto,
            editarGasto = alEditarGasto,
            abrirPago = viewModel::abrirPago,
            cambiarDireccionDelPago = viewModel::cambiarDireccionDelPago,
            cambiarMontoDelPago = viewModel::onMontoDelPagoChange,
            cambiarFechaDelPago = viewModel::onFechaDelPagoChange,
            registrarPago = viewModel::registrarElPago,
            cerrarPago = viewModel::cerrarPago,
            borrarPago = { viewModel.borrarPago(it) },
            rehacer = { viewModel.rehacer(it) },
            descartar = { viewModel.descartar(it) },
            reintentar = viewModel::reintentar,
        ),
        snackbar = snackbar,
        modifier = modifier,
    )
}

/** Todo lo que puede hacer la persona en Balance; las acciones suben, el estado baja. */
data class AccionesDeBalance(
    val mesAnterior: () -> Unit = {},
    val mesSiguiente: () -> Unit = {},
    val registrarGasto: () -> Unit = {},
    val editarGasto: (String) -> Unit = {},
    val abrirPago: () -> Unit = {},
    val cambiarDireccionDelPago: () -> Unit = {},
    val cambiarMontoDelPago: (String) -> Unit = {},
    val cambiarFechaDelPago: (LocalDate) -> Unit = {},
    val registrarPago: () -> Unit = {},
    val cerrarPago: () -> Unit = {},
    val borrarPago: (String) -> Unit = {},
    val rehacer: (ConflictoDeGasto) -> Unit = {},
    val descartar: (ConflictoDeGasto) -> Unit = {},
    val reintentar: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceContenido(
    estado: BalanceUiState,
    acciones: AccionesDeBalance,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.destino_balance)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (estado is BalanceUiState.Contenido) {
                FloatingActionButton(onClick = acciones.registrarGasto) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.gasto_titulo_registrar))
                }
            }
        },
    ) { innerPadding ->
        val relleno = Modifier.padding(innerPadding)
        when (estado) {
            BalanceUiState.Cargando -> EsqueletoDeCarga(relleno)
            BalanceUiState.Error -> EstadoVacio(
                titulo = stringResource(R.string.balance_error_titulo),
                descripcion = stringResource(R.string.balance_error_texto),
                icono = Icons.Outlined.ErrorOutline,
                accion = stringResource(R.string.accion_reintentar),
                alTocarAccion = acciones.reintentar,
                modifier = relleno,
            )
            BalanceUiState.SinOtraPersona -> EstadoVacio(
                titulo = stringResource(R.string.balance_sin_otra_persona_titulo),
                descripcion = stringResource(R.string.balance_sin_otra_persona_texto),
                icono = Icons.Outlined.GroupAdd,
                modifier = relleno,
            )
            is BalanceUiState.Contenido -> {
                ListaDeBalance(estado, acciones, innerPadding)
                estado.pago?.let { HojaDePago(it, acciones) }
            }
        }
    }
}

@Composable
private fun ListaDeBalance(estado: BalanceUiState.Contenido, acciones: AccionesDeBalance, innerPadding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Espaciado.margenPantalla,
            end = Espaciado.margenPantalla,
            top = innerPadding.calculateTopPadding(),
            // Lugar para el botón flotante, así no tapa el último movimiento.
            bottom = innerPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Espaciado.entreCards),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "mes") { SelectorDeMes(estado.mes, estado.puedeAvanzar, acciones) }
        items(estado.conflictos, key = { "conflicto-${it.conflicto.id}" }) { conflicto ->
            TarjetaDeConflicto(conflicto, alRehacer = { acciones.rehacer(conflicto.conflicto) }, alDescartar = { acciones.descartar(conflicto.conflicto) })
        }
        item(key = "resultado") { TarjetaDeBalance(estado, acciones.abrirPago) }
        estado.saldoAnterior?.let { saldo -> item(key = "saldo") { SaldoAnterior(saldo) } }
        if (estado.movimientos.isEmpty()) {
            item(key = "vacio") {
                EstadoVacio(
                    titulo = stringResource(R.string.balance_vacio_titulo),
                    descripcion = stringResource(R.string.balance_vacio_texto),
                    icono = Icons.AutoMirrored.Outlined.ReceiptLong,
                    accion = stringResource(R.string.gasto_titulo_registrar),
                    alTocarAccion = acciones.registrarGasto,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            item(key = "titulo-movimientos") {
                Text(
                    stringResource(R.string.balance_movimientos).uppercase(Locale.forLanguageTag("es-AR")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(estado.movimientos, key = { it.id }) { movimiento ->
                when (movimiento) {
                    is Movimiento.CuotaDeGasto -> ItemDeGasto(movimiento, alTocar = { acciones.editarGasto(movimiento.gastoId) })
                    is Movimiento.PagoHecho -> ItemDePago(movimiento, alBorrar = { acciones.borrarPago(movimiento.pagoId) })
                }
            }
        }
    }
}

@Composable
private fun SelectorDeMes(mes: YearMonth, puedeAvanzar: Boolean, acciones: AccionesDeBalance) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = acciones.mesAnterior) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.balance_mes_anterior))
        }
        Text(mes.comoNombre(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = acciones.mesSiguiente, enabled = puedeAvanzar) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.balance_mes_siguiente))
        }
    }
}

private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-AR"))

/** "Octubre 2026". */
internal fun YearMonth.comoNombre(): String =
    format(formatoMes).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-AR")) }

// --- Previews de cada estado (AGENTS.md §14: render de carga, vacío, error y contenido) ---

internal val contenidoDeEjemplo = BalanceUiState.Contenido(
    mes = YearMonth.of(2026, 10),
    puedeAvanzar = false,
    resultado = DeudaEnPantalla("ana", "Ana", "tomas", "Tomás", 1_875_000),
    pagadoPor = listOf(TotalDePersona("ana", "Ana", 4_190_000), TotalDePersona("tomas", "Tomás", 6_520_000)),
    saldoAnterior = null,
    movimientos = listOf(
        Movimiento.CuotaDeGasto("g1", "Supermercado", 2_840_000, "Ana", null, 1, 1),
        Movimiento.CuotaDeGasto("g2", "Seguro del auto", 1_260_000, "Tomás", null, 2, 6),
        Movimiento.CuotaDeGasto("g3", "Delivery", 870_000, "Ana", "Ana", 1, 1),
        Movimiento.PagoHecho("p1", "Ana", "Tomás", 500_000, LocalDate.of(2026, 10, 20)),
    ),
    conflictos = emptyList(),
)

@Preview(showBackground = true)
@Composable
private fun BalanceContenidoPreview() {
    MitiTheme { BalanceContenido(contenidoDeEjemplo, AccionesDeBalance()) }
}

@Preview(showBackground = true)
@Composable
private fun BalanceAManoSinGastosPreview() {
    MitiTheme {
        BalanceContenido(
            contenidoDeEjemplo.copy(resultado = null, movimientos = emptyList(), pagadoPor = contenidoDeEjemplo.pagadoPor.map { it.copy(montoCentavos = 0) }),
            AccionesDeBalance(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCargandoPreview() {
    MitiTheme { BalanceContenido(BalanceUiState.Cargando, AccionesDeBalance()) }
}

@Preview(showBackground = true)
@Composable
private fun BalanceSinOtraPersonaPreview() {
    MitiTheme { BalanceContenido(BalanceUiState.SinOtraPersona, AccionesDeBalance()) }
}

