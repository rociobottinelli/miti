package ar.edu.uade.miti.presentation.compras

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvisoDeSincronizacion
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.presentation.components.EsqueletoDeCarga
import ar.edu.uade.miti.presentation.components.EstadoVacio
import ar.edu.uade.miti.presentation.components.IndicadorPendienteDeSync
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalDate

@Composable
fun ComprasScreen(modifier: Modifier = Modifier, viewModel: ComprasViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            val mensaje = when (evento) {
                EventoDeCompras.CompraTerminada -> recursos.getString(R.string.compras_terminada)
                is EventoDeCompras.NoSePudo -> evento.mensaje
            }
            snackbar.showSnackbar(mensaje)
        }
    }

    ComprasContenido(
        estado = estado,
        alEscribir = viewModel::escribir,
        alAgregar = viewModel::agregar,
        alTachar = viewModel::tachar,
        alTerminar = viewModel::terminar,
        alReintentar = viewModel::reintentar,
        alSincronizar = viewModel::sincronizar,
        snackbar = snackbar,
        modifier = modifier,
    )
}

/** Figma 37:1098 (lista), 37:1272 (con carrito) y los estados del wireframe. Sin estado propio. */
@Composable
fun ComprasContenido(
    estado: ComprasUiState,
    alEscribir: (String) -> Unit,
    alAgregar: () -> Unit,
    alTachar: (id: String, enCarrito: Boolean) -> Unit,
    alTerminar: () -> Unit,
    alReintentar: () -> Unit,
    alSincronizar: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        // El Scaffold de la barra inferior ya aplicó los márgenes del sistema.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        // El campo queda fijo abajo, al alcance del pulgar, y sube con el teclado (RNF07).
        bottomBar = {
            if (estado is ComprasUiState.Contenido) CampoParaAgregar(estado.nuevoItem, estado.sePuedeAgregar, alEscribir, alAgregar)
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Encabezado(estado as? ComprasUiState.Contenido)
            when (estado) {
                ComprasUiState.Cargando -> EsqueletoDeCarga(Modifier.padding(Espaciado.margenPantalla), cantidad = 5)
                ComprasUiState.Error -> EstadoVacio(
                    titulo = stringResource(R.string.compras_error_titulo),
                    descripcion = stringResource(R.string.compras_error_descripcion),
                    icono = Icons.Outlined.ErrorOutline,
                    accion = stringResource(R.string.accion_reintentar),
                    alTocarAccion = alReintentar,
                )
                is ComprasUiState.Contenido -> {
                    estado.aviso?.let {
                        AvisoDeSincronizacion(
                            aviso = it,
                            hoy = estado.hoy,
                            otro = null,
                            alSincronizar = alSincronizar,
                            modifier = Modifier.padding(horizontal = Espaciado.margenPantalla).padding(bottom = Espaciado.entreCards),
                        )
                    }
                    if (estado.vacia) {
                        EstadoVacio(
                            titulo = stringResource(R.string.compras_vacio_titulo),
                            descripcion = stringResource(R.string.compras_vacio_descripcion),
                            icono = Icons.Outlined.ShoppingCart,
                        )
                    } else {
                        Lista(estado, alTachar, alTerminar)
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(contenido: ComprasUiState.Contenido?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = Espaciado.margenPantalla),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.destino_compras),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (contenido != null && !contenido.vacia) {
            val descripcion = stringResource(R.string.compras_progreso_descripcion, contenido.enCarrito.size, contenido.total)
            Text(
                "${contenido.enCarrito.size}/${contenido.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clearAndSetSemantics { contentDescription = descripcion },
            )
        }
    }
}

@Composable
private fun Lista(estado: ComprasUiState.Contenido, alTachar: (String, Boolean) -> Unit, alTerminar: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = Espaciado.margenPantalla, end = Espaciado.margenPantalla, top = 4.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        grupo("por-comprar", estado.porComprar, enCarrito = false, alTachar)
        if (estado.enCarrito.isNotEmpty()) {
            item(key = "titulo-carrito") { TituloDelCarrito(estado.enCarrito.size) }
            grupo("carrito", estado.enCarrito, enCarrito = true, alTachar)
            item(key = "terminar") {
                Button(
                    onClick = alTerminar,
                    enabled = !estado.terminando,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Outlined.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.compras_terminar), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Una tarjeta con borde que agrupa las filas, separadas por una línea, como en el Figma. */
private fun LazyListScope.grupo(clave: String, items: List<ItemDeCompra>, enCarrito: Boolean, alTachar: (String, Boolean) -> Unit) {
    if (items.isEmpty()) return
    item(key = clave) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                items.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FilaDeCompra(item, enCarrito, alTachar)
                }
            }
        }
    }
}

@Composable
private fun FilaDeCompra(item: ItemDeCompra, enCarrito: Boolean, alTachar: (String, Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            // Toda la fila se toca, no solo la casilla (RNF07: una mano en el supermercado).
            .toggleable(value = enCarrito, role = Role.Checkbox, onValueChange = { alTachar(item.id, it) })
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = enCarrito, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(
            item.titulo,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enCarrito) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (enCarrito) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        if (item.pendienteDeSync) IndicadorPendienteDeSync(Modifier.padding(end = 12.dp))
    }
}

@Composable
private fun TituloDelCarrito(cantidad: Int) {
    Row(
        Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            pluralStringResource(R.plurals.compras_en_el_carrito, cantidad, cantidad).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun CampoParaAgregar(texto: String, habilitado: Boolean, alEscribir: (String) -> Unit, alAgregar: () -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface).imePadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.padding(horizontal = Espaciado.margenPantalla, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = alEscribir,
                placeholder = { Text(stringResource(R.string.compras_agregar)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                // Con "Listo" del teclado se agrega y el teclado queda abierto para el siguiente ítem.
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { alAgregar() }),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = alAgregar, enabled = habilitado, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.compras_agregar_boton))
            }
        }
    }
}

private val hoyDeEjemplo = LocalDate.of(2026, 9, 30)
private val listaDeEjemplo = ComprasUiState.Contenido(
    porComprar = listOf(ItemDeCompra("1", "Papa"), ItemDeCompra("2", "Tomate")),
    enCarrito = listOf(ItemDeCompra("3", "Detergente"), ItemDeCompra("4", "Papel higiénico"), ItemDeCompra("5", "Café molido")),
    hoy = hoyDeEjemplo,
)

@Composable
private fun VistaPrevia(estado: ComprasUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) { ComprasContenido(estado, {}, {}, { _, _ -> }, {}, {}, {}) }
}

@Preview(name = "Con carrito (37:1272)", showSystemUi = true)
@Composable
private fun ComprasConCarritoPreview() = VistaPrevia(listaDeEjemplo)

@Preview(name = "Lista (37:1098)", showSystemUi = true)
@Composable
private fun ComprasListaPreview() = VistaPrevia(
    listaDeEjemplo.copy(porComprar = listaDeEjemplo.porComprar + listaDeEjemplo.enCarrito, enCarrito = emptyList(), nuevoItem = "Leche"),
)

@Preview(name = "Oscuro", showSystemUi = true)
@Composable
private fun ComprasOscuroPreview() = VistaPrevia(listaDeEjemplo, oscuro = true)

@Preview(name = "Vacío", showSystemUi = true)
@Composable
private fun ComprasVaciaPreview() = VistaPrevia(listaDeEjemplo.copy(porComprar = emptyList(), enCarrito = emptyList()))

@Preview(name = "Sin conexión", showSystemUi = true)
@Composable
private fun ComprasSinConexionPreview() = VistaPrevia(
    listaDeEjemplo.copy(
        aviso = AvisoDeSync.SinConexion(hoyDeEjemplo.atTime(14, 32)),
        porComprar = listaDeEjemplo.porComprar.map { it.copy(pendienteDeSync = true) },
    ),
)

@Preview(name = "Carga", showSystemUi = true)
@Composable
private fun ComprasCargandoPreview() = VistaPrevia(ComprasUiState.Cargando)

@Preview(name = "Error", showSystemUi = true)
@Composable
private fun ComprasErrorPreview() = VistaPrevia(ComprasUiState.Error)
