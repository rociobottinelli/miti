package ar.edu.uade.miti.presentation.gastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.EstadoVacio
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme

/** Conecta el formulario con su ViewModel y con la navegación (#14). */
@Composable
fun FormularioDeGastoScreen(
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormularioDeGastoViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDeFormulario.Guardado, EventoDeFormulario.Borrado -> alCerrar()
                is EventoDeFormulario.NoSePudo -> snackbar.showSnackbar(evento.mensaje)
            }
        }
    }

    FormularioDeGastoContenido(
        estado = estado,
        alCerrar = alCerrar,
        onMontoChange = viewModel::onMontoChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onPagadorChange = viewModel::onPagadorChange,
        onRepartoChange = viewModel::onRepartoChange,
        onEnCuotasChange = viewModel::onEnCuotasChange,
        onCuotasTotalesChange = viewModel::onCuotasTotalesChange,
        onCuotaActualChange = viewModel::onCuotaActualChange,
        onGuardarClick = viewModel::onGuardarClick,
        onBorrarClick = viewModel::onBorrarClick,
        snackbar = snackbar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioDeGastoContenido(
    estado: FormularioDeGastoUiState,
    alCerrar: () -> Unit,
    onMontoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onPagadorChange: (String) -> Unit,
    onRepartoChange: (String?) -> Unit,
    onEnCuotasChange: (Boolean) -> Unit,
    onCuotasTotalesChange: (String) -> Unit,
    onCuotaActualChange: (String) -> Unit,
    onGuardarClick: () -> Unit,
    onBorrarClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (estado.editando) R.string.gasto_titulo_editar else R.string.gasto_titulo_registrar))
                },
                navigationIcon = {
                    IconButton(onClick = alCerrar) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.accion_cerrar))
                    }
                },
                actions = {
                    if (estado.editando && estado.errorDeCarga == null) {
                        IconButton(onClick = { confirmarBorrado = true }, enabled = !estado.guardando) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.gasto_borrar))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            estado.errorDeCarga != null -> EstadoVacio(
                titulo = stringResource(estado.errorDeCarga),
                descripcion = stringResource(R.string.gasto_error_ya_no_existe_detalle),
                icono = Icons.Outlined.CreditCard,
                accion = stringResource(R.string.accion_volver),
                alTocarAccion = alCerrar,
                modifier = Modifier.padding(innerPadding),
            )
            else -> Campos(
                estado, onMontoChange, onDescripcionChange, onPagadorChange, onRepartoChange, onEnCuotasChange,
                onCuotasTotalesChange, onCuotaActualChange, onGuardarClick,
                Modifier.padding(innerPadding),
            )
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(stringResource(R.string.gasto_borrar_titulo)) },
            text = { Text(stringResource(R.string.gasto_borrar_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; onBorrarClick() }) { Text(stringResource(R.string.gasto_borrar)) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}

@Composable
private fun Campos(
    estado: FormularioDeGastoUiState,
    onMontoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onPagadorChange: (String) -> Unit,
    onRepartoChange: (String?) -> Unit,
    onEnCuotasChange: (Boolean) -> Unit,
    onCuotasTotalesChange: (String) -> Unit,
    onCuotaActualChange: (String) -> Unit,
    onGuardarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Monto(estado.monto, estado.errores.monto, onMontoChange)

        Column(Modifier.padding(horizontal = 20.dp, vertical = Espaciado.entreSecciones)) {
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = onDescripcionChange,
                label = { Text(stringResource(R.string.gasto_descripcion)) },
                isError = estado.errores.descripcion != null,
                supportingText = estado.errores.descripcion?.let { { Text(stringResource(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Titulo(R.string.gasto_quien_pago)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                estado.integrantes.forEachIndexed { i, persona ->
                    SegmentedButton(
                        selected = estado.pagadorId == persona.id,
                        onClick = { onPagadorChange(persona.id) },
                        shape = SegmentedButtonDefaults.itemShape(i, estado.integrantes.size),
                    ) { Text(persona.nombre) }
                }
            }

            Titulo(R.string.gasto_como_se_reparte)
            val opciones = listOf<Persona?>(null) + estado.integrantes
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opciones.forEachIndexed { i, persona ->
                    SegmentedButton(
                        selected = estado.beneficiarioId == persona?.id,
                        onClick = { onRepartoChange(persona?.id) },
                        shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                    ) {
                        Text(
                            if (persona == null) stringResource(R.string.gasto_a_medias)
                            else stringResource(R.string.gasto_solo, persona.nombre),
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.size(Espaciado.entreSecciones))
            Cuotas(estado, onEnCuotasChange, onCuotasTotalesChange, onCuotaActualChange)

            Spacer(Modifier.size(20.dp))
            Button(
                onClick = onGuardarClick,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().heightIn(min = Espaciado.areaTactilMinima),
            ) {
                if (estado.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.gasto_guardar))
            }
        }
    }
}

/** El monto grande arriba, como en el Figma, con el error justo debajo. */
@Composable
private fun Monto(valor: String, error: Int?, onMontoChange: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            val descripcion = stringResource(R.string.gasto_monto)
            TextField(
                value = valor,
                onValueChange = onMontoChange,
                prefix = { Text("$", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = {
                    Text("0", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light), textAlign = TextAlign.Center)
                },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(0.7f).semantics { contentDescription = descripcion },
            )
            if (error != null) {
                Text(stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Cuotas(
    estado: FormularioDeGastoUiState,
    onEnCuotasChange: (Boolean) -> Unit,
    onCuotasTotalesChange: (String) -> Unit,
    onCuotaActualChange: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.gasto_en_cuotas), modifier = Modifier.weight(1f))
                Switch(checked = estado.enCuotas, onCheckedChange = onEnCuotasChange)
            }
            if (estado.enCuotas) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = estado.cuotasTotales,
                        onValueChange = onCuotasTotalesChange,
                        label = { Text(stringResource(R.string.gasto_cuotas_totales)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = estado.errores.cuotas != null,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = estado.cuotaActual,
                        onValueChange = onCuotaActualChange,
                        label = { Text(stringResource(R.string.gasto_cuota_actual)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = estado.errores.cuotas != null,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                estado.errores.cuotas?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.gasto_cuotas_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Titulo(texto: Int) {
    Text(
        stringResource(texto),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = Espaciado.entreSecciones, bottom = 8.dp),
    )
}

private val personas = listOf(Persona("ana", "Ana"), Persona("tomas", "Tomás"))

@Preview(showBackground = true)
@Composable
private fun FormularioNuevoPreview() {
    MitiTheme {
        FormularioDeGastoContenido(
            estado = FormularioDeGastoUiState(cargando = false, integrantes = personas, pagadorId = "ana"),
            alCerrar = {}, onMontoChange = {}, onDescripcionChange = {}, onPagadorChange = {}, onRepartoChange = {},
            onEnCuotasChange = {}, onCuotasTotalesChange = {}, onCuotaActualChange = {}, onGuardarClick = {}, onBorrarClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormularioConErroresYCuotasPreview() {
    MitiTheme {
        FormularioDeGastoContenido(
            estado = FormularioDeGastoUiState(
                cargando = false, editando = true, integrantes = personas, pagadorId = "tomas", beneficiarioId = "ana",
                enCuotas = true, cuotasTotales = "3", cuotaActual = "5",
                errores = ErroresDeGasto(monto = R.string.gasto_error_monto, cuotas = R.string.gasto_error_cuota_actual),
            ),
            alCerrar = {}, onMontoChange = {}, onDescripcionChange = {}, onPagadorChange = {}, onRepartoChange = {},
            onEnCuotasChange = {}, onCuotasTotalesChange = {}, onCuotaActualChange = {}, onGuardarClick = {}, onBorrarClick = {},
        )
    }
}
