package ar.edu.uade.miti.presentation.agregar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.domain.model.Frecuencia
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val espaniol = Locale.forLanguageTag("es-AR")

/** Formulario de la hoja "Nueva rutina" (Figma 37:2544). Sin estado propio. */
@Composable
fun FormularioDeRutina(
    estado: NuevaRutinaUiState,
    alCambiarNombre: (String) -> Unit,
    alElegirFrecuencia: (Frecuencia) -> Unit,
    alAlternarDia: (DayOfWeek) -> Unit,
    alElegirVeces: (Int) -> Unit,
    alGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
        TituloDeHoja(stringResource(R.string.rutina_nueva))
        OutlinedTextField(
            value = estado.nombre,
            onValueChange = alCambiarNombre,
            label = { Text(stringResource(R.string.formulario_nombre)) },
            isError = estado.errorNombre != null,
            supportingText = estado.errorNombre?.let { { Text(textoDe(it)) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )
        Etiqueta(stringResource(R.string.rutina_cada_cuanto))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(Frecuencia.DIARIA to R.string.rutina_todos_los_dias, Frecuencia.DIAS_DE_LA_SEMANA to R.string.rutina_algunos_dias)
                .forEachIndexed { i, (frecuencia, texto) ->
                    SegmentedButton(
                        selected = estado.frecuencia == frecuencia,
                        onClick = { alElegirFrecuencia(frecuencia) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(texto)) }
                }
        }
        if (estado.frecuencia == Frecuencia.DIAS_DE_LA_SEMANA) {
            SelectorDeDias(estado.dias, alAlternarDia)
            estado.errorDias?.let {
                Text(
                    textoDe(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
        }
        Etiqueta(stringResource(R.string.rutina_veces_por_dia))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            (1..3).forEach { veces ->
                SegmentedButton(
                    selected = estado.vecesPorDia == veces,
                    onClick = { alElegirVeces(veces) },
                    shape = SegmentedButtonDefaults.itemShape(veces - 1, 3),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(veces.toString()) }
            }
        }
        NotaDeFormulario(stringResource(R.string.rutina_nota), Modifier.padding(top = 16.dp))
        estado.errorAlGuardar?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
        Button(
            onClick = alGuardar,
            enabled = estado.puedeGuardar,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.rutina_guardar)) }
    }
}

/** Lunes a domingo, como en el calendario argentino. */
@Composable
private fun SelectorDeDias(elegidos: Set<DayOfWeek>, alAlternar: (DayOfWeek) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayOfWeek.entries.forEach { dia ->
            FilterChip(
                selected = dia in elegidos,
                onClick = { alAlternar(dia) },
                label = { Text(dia.getDisplayName(TextStyle.SHORT, espaniol).replaceFirstChar { it.titlecase(espaniol) }.trimEnd('.')) },
                // En pantalla "lun", "mar"…; TalkBack lee el nombre completo ("lunes").
                modifier = Modifier.semantics { contentDescription = dia.getDisplayName(TextStyle.FULL, espaniol) },
            )
        }
    }
}

@Composable
internal fun TituloDeHoja(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun Etiqueta(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/** Recuadro gris con un ícono de información (Figma: "Cualquiera de los dos la puede marcar."). */
@Composable
internal fun NotaDeFormulario(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun textoDe(error: ErrorDeFormulario): String = stringResource(
    when (error) {
        ErrorDeFormulario.NOMBRE_VACIO -> R.string.formulario_error_nombre
        ErrorDeFormulario.SIN_DIAS -> R.string.rutina_error_dias
        ErrorDeFormulario.FECHA_PASADA -> R.string.tarea_error_fecha_pasada
    },
)

@Composable
private fun VistaPrevia(estado: NuevaRutinaUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) { FormularioDeRutina(estado, {}, {}, {}, {}, {}) }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeRutinaPreview() = VistaPrevia(NuevaRutinaUiState())

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeRutinaAlgunosDiasPreview() = VistaPrevia(
    NuevaRutinaUiState(nombre = "Sacar la basura", frecuencia = Frecuencia.DIAS_DE_LA_SEMANA, dias = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
    oscuro = true,
)

@Preview(name = "Errores", showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeRutinaErroresPreview() = VistaPrevia(
    NuevaRutinaUiState(
        frecuencia = Frecuencia.DIAS_DE_LA_SEMANA,
        errorNombre = ErrorDeFormulario.NOMBRE_VACIO,
        errorDias = ErrorDeFormulario.SIN_DIAS,
    ),
)
