package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.EsqueletoDeCarga
import ar.edu.uade.miti.presentation.components.EstadoVacio
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * La pantalla Hoy sin estado propio: recibe el [HoyUiState] y avisa cada acción hacia arriba
 * (state hoisting). Por eso se puede ver en las previews con cualquier estado.
 */
@Composable
fun HoyContenido(
    estado: HoyUiState,
    alMarcar: (String) -> Unit,
    alElegirFiltro: (FiltroDeTareas) -> Unit,
    alAgregar: () -> Unit,
    alAbrirAjustes: () -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    alSincronizar: () -> Unit = {},
    alConfirmarMarca: () -> Unit = {},
    alCancelarMarca: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        // El Scaffold de la barra inferior ya aplicó los márgenes del sistema.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Abajo a la derecha, al alcance del pulgar (RNF07).
            if (estado !is HoyUiState.Cargando && estado !is HoyUiState.Error) {
                FloatingActionButton(onClick = alAgregar) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hoy_agregar))
                }
            }
        },
    ) { relleno ->
        Column(modifier = Modifier.padding(relleno).fillMaxSize()) {
            when (estado) {
                HoyUiState.Cargando -> EsqueletoDeCarga(
                    modifier = Modifier.padding(Espaciado.margenPantalla),
                    cantidad = 4,
                )
                HoyUiState.Error -> EstadoVacio(
                    titulo = stringResource(R.string.hoy_error_titulo),
                    descripcion = stringResource(R.string.hoy_error_descripcion),
                    icono = Icons.Outlined.ErrorOutline,
                    accion = stringResource(R.string.accion_reintentar),
                    alTocarAccion = alReintentar,
                )
                is HoyUiState.Vacio -> {
                    EncabezadoConAviso(estado.encabezado, alAbrirAjustes, alSincronizar)
                    EstadoVacio(
                        titulo = stringResource(R.string.hoy_vacio_titulo),
                        descripcion = stringResource(R.string.hoy_vacio_descripcion),
                        icono = Icons.Outlined.AddHome,
                        accion = stringResource(R.string.hoy_vacio_accion),
                        alTocarAccion = alAgregar,
                        modifier = Modifier.weight(1f),
                    )
                }
                is HoyUiState.TodoAlDia -> {
                    EncabezadoConAviso(estado.encabezado, alAbrirAjustes, alSincronizar)
                    EstadoVacio(
                        titulo = stringResource(R.string.hoy_al_dia_titulo),
                        descripcion = stringResource(R.string.hoy_al_dia_descripcion),
                        icono = Icons.Outlined.CheckCircle,
                        modifier = Modifier.weight(1f),
                    )
                }
                is HoyUiState.Contenido -> {
                    ListaDeHoy(estado, alMarcar, alElegirFiltro, alAbrirAjustes, alSincronizar)
                    estado.confirmarMarca?.let {
                        DialogoConfirmarMarca(it, estado.encabezado.fecha, alConfirmarMarca, alCancelarMarca)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaDeHoy(
    estado: HoyUiState.Contenido,
    alMarcar: (String) -> Unit,
    alElegirFiltro: (FiltroDeTareas) -> Unit,
    alAbrirAjustes: () -> Unit,
    alSincronizar: () -> Unit,
) {
    EncabezadoConAviso(estado.encabezado, alAbrirAjustes, alSincronizar)
    FiltrosDeTareas(estado.filtro, estado.encabezado.otro, alElegirFiltro)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Abajo deja lugar para que el botón + no tape la última tarjeta.
        contentPadding = PaddingValues(start = Espaciado.margenPantalla, end = Espaciado.margenPantalla, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Espaciado.entreCards),
    ) {
        if (estado.rutinas.isNotEmpty()) {
            item(key = "titulo-rutinas") {
                TituloDeSeccion(
                    texto = stringResource(R.string.hoy_seccion_rutinas),
                    progreso = estado.rutinasCompletas to estado.rutinas.size,
                )
            }
            items(estado.rutinas, key = { "rutina-${it.id}" }) { rutina ->
                TarjetaDeRutina(rutina, alMarcar = { alMarcar(rutina.id) }, modifier = Modifier.animateItem())
            }
        }
        item(key = "titulo-tareas") { TituloDeSeccion(stringResource(R.string.hoy_seccion_tareas)) }
        if (estado.tareas.isEmpty()) {
            item(key = "sin-tareas") {
                Text(
                    text = when (estado.filtro) {
                        FiltroDeTareas.MIAS -> stringResource(R.string.hoy_sin_tareas_mias)
                        FiltroDeTareas.DEL_OTRO ->
                            stringResource(R.string.hoy_sin_tareas_del_otro, estado.encabezado.otro?.nombre.orEmpty())
                        FiltroDeTareas.TODAS -> stringResource(R.string.hoy_sin_tareas)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(estado.tareas, key = { "tarea-${it.id}" }) { tarea ->
                TarjetaDeTarea(tarea, modifier = Modifier.animateItem())
            }
        }
    }
}

/** "RUTINAS DE HOY" con la píldora "1/3" a la derecha. */
@Composable
private fun TituloDeSeccion(texto: String, modifier: Modifier = Modifier, progreso: Pair<Int, Int>? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Espaciado.entreCards),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .semantics { heading(); contentDescription = texto },
        )
        progreso?.let { (hechas, total) ->
            val descripcion = stringResource(R.string.hoy_rutinas_hechas, hechas, total)
            Text(
                text = "$hechas/$total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .clearAndSetSemantics { contentDescription = descripcion },
            )
        }
    }
}

// Previews de cada estado, con los datos del Figma.

private val encabezadoDeEjemplo = Encabezado(
    fecha = LocalDate.of(2026, 9, 30),
    yo = IntegranteEnPantalla("a", "Ana", 0),
    otro = IntegranteEnPantalla("t", "Tomás", 1),
)

private val contenidoDeEjemplo = HoyUiState.Contenido(
    encabezado = encabezadoDeEjemplo,
    filtro = FiltroDeTareas.TODAS,
    rutinas = listOf(
        RutinaDeHoy("1", "Darle de comer a Pancho", 1, 2, Marca("Tomás", LocalTime.of(8, 15))),
        RutinaDeHoy("2", "Regar las plantas", 0, 1, null),
        RutinaDeHoy("3", "Sacar la basura", 1, 1, Marca("Ana", LocalTime.of(21, 3))),
    ),
    tareas = listOf(
        TareaDeHoy("4", "Pagar ABL", CuandoVence.EstaSemana(DayOfWeek.THURSDAY, LocalTime.of(18, 0)), encabezadoDeEjemplo.otro, true),
        TareaDeHoy("5", "Llevar a Pancho al veterinario", CuandoVence.EstaSemana(DayOfWeek.SATURDAY, LocalTime.of(10, 0)), encabezadoDeEjemplo.yo, true),
    ),
)

@Composable
private fun VistaPrevia(estado: HoyUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) {
        HoyContenido(estado, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Contenido", showSystemUi = true)
@Composable
private fun HoyContenidoPreview() = VistaPrevia(contenidoDeEjemplo)

@Preview(name = "Contenido oscuro", showSystemUi = true)
@Composable
private fun HoyContenidoOscuroPreview() = VistaPrevia(contenidoDeEjemplo, oscuro = true)

@Preview(name = "Sin conexión", showSystemUi = true)
@Composable
private fun HoySinConexionPreview() = VistaPrevia(
    contenidoDeEjemplo.copy(encabezado = encabezadoDeEjemplo.copy(aviso = AvisoDeSync.SinConexion(LocalDate.of(2026, 9, 30).atTime(14, 32)))),
)

@Preview(name = "Datos desactualizados al marcar", showSystemUi = true)
@Composable
private fun HoyConfirmarMarcaPreview() = VistaPrevia(
    contenidoDeEjemplo.copy(
        encabezado = encabezadoDeEjemplo.copy(aviso = AvisoDeSync.Desactualizado(LocalDate.of(2026, 9, 30).atTime(8, 15))),
        confirmarMarca = ConfirmacionDeMarca("2", "Regar las plantas", "Tomás", LocalDate.of(2026, 9, 30).atTime(8, 15)),
    ),
)

@Preview(name = "Filtro sin tareas", showSystemUi = true)
@Composable
private fun HoyFiltroSinTareasPreview() =
    VistaPrevia(contenidoDeEjemplo.copy(filtro = FiltroDeTareas.DEL_OTRO, tareas = emptyList()))

@Preview(name = "Vacío", showSystemUi = true)
@Composable
private fun HoyVacioPreview() = VistaPrevia(HoyUiState.Vacio(encabezadoDeEjemplo))

@Preview(name = "Todo al día", showSystemUi = true)
@Composable
private fun HoyTodoAlDiaPreview() = VistaPrevia(HoyUiState.TodoAlDia(encabezadoDeEjemplo))

@Preview(name = "Cargando", showSystemUi = true)
@Composable
private fun HoyCargandoPreview() = VistaPrevia(HoyUiState.Cargando)

@Preview(name = "Error", showSystemUi = true)
@Composable
private fun HoyErrorPreview() = VistaPrevia(HoyUiState.Error)
