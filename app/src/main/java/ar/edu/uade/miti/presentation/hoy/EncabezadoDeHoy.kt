package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ar.edu.uade.miti.presentation.components.AvatarDeIntegrante
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalDate

/** Día de la semana, fecha y el avatar propio, que abre Ajustes (preentrega, sección 11). */
@Composable
fun EncabezadoDeHoy(
    encabezado: Encabezado,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fechaLarga = encabezado.fecha.comoFechaLarga()
    val dia = encabezado.fecha.dayOfWeek.comoNombre()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = Espaciado.margenPantalla, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    heading()
                    contentDescription = "$dia $fechaLarga"
                },
        ) {
            Text(
                text = dia.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                // onSurfaceVariant y no outline como en el Figma, por contraste (RNF08).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = fechaLarga,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        encabezado.yo?.let { yo ->
            val abrirAjustes = stringResource(R.string.hoy_abrir_ajustes)
            IconButton(
                onClick = alAbrirAjustes,
                modifier = Modifier
                    .size(Espaciado.areaTactilMinima)
                    .semantics { contentDescription = abrirAjustes },
            ) {
                // TalkBack dice "Abrir ajustes" y no el nombre: lo que importa es a dónde lleva.
                AvatarDeIntegrante(
                    nombre = yo.nombre,
                    posicion = yo.posicion,
                    tamanio = 44.dp,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/**
 * Chips "Todo", "Mías" y "De <la otra persona>". Filtran solo las tareas: las rutinas no tienen
 * responsable. Si en el hogar hay una sola persona no se muestran, porque no hay nada que filtrar.
 */
@Composable
fun FiltrosDeTareas(
    seleccionado: FiltroDeTareas,
    otro: IntegranteEnPantalla?,
    alElegir: (FiltroDeTareas) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (otro == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Espaciado.margenPantalla),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.entreCards),
    ) {
        listOf(
            FiltroDeTareas.TODAS to stringResource(R.string.hoy_filtro_todas),
            FiltroDeTareas.MIAS to stringResource(R.string.hoy_filtro_mias),
            FiltroDeTareas.DEL_OTRO to stringResource(R.string.hoy_filtro_del_otro, otro.nombre),
        ).forEach { (filtro, etiqueta) ->
            val elegido = filtro == seleccionado
            FilterChip(
                selected = elegido,
                onClick = { alElegir(filtro) },
                label = { Text(etiqueta) },
                leadingIcon = if (elegido) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

private val ejemplo = Encabezado(
    fecha = LocalDate.of(2026, 9, 30),
    yo = IntegranteEnPantalla("a", "Ana", 0),
    otro = IntegranteEnPantalla("t", "Tomás", 1),
)

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun EncabezadoDeHoyPreview() {
    MitiTheme {
        Column {
            EncabezadoDeHoy(ejemplo, alAbrirAjustes = {})
            FiltrosDeTareas(FiltroDeTareas.TODAS, ejemplo.otro, alElegir = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1415)
@Composable
private fun EncabezadoDeHoyOscuroPreview() {
    MitiTheme(darkTheme = true) {
        Column {
            EncabezadoDeHoy(ejemplo, alAbrirAjustes = {})
            FiltrosDeTareas(FiltroDeTareas.MIAS, ejemplo.otro, alElegir = {})
        }
    }
}
