package ar.edu.uade.miti.presentation.agregar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR"))
private val formatoHora = DateTimeFormatter.ofPattern("H:mm")

/** Formulario de la hoja "Nueva tarea" (Figma 37:2226). Sin estado propio, salvo qué diálogo está abierto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioDeTarea(
    estado: NuevaTareaUiState,
    alCambiarNombre: (String) -> Unit,
    alElegirResponsable: (String) -> Unit,
    alElegirFecha: (LocalDate) -> Unit,
    alElegirHora: (LocalTime) -> Unit,
    alCambiarAviso: (Boolean) -> Unit,
    alGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var eligiendoHora by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
        TituloDeHoja(stringResource(R.string.tarea_nueva))
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
        if (estado.responsables.size > 1) {
            Etiqueta(stringResource(R.string.tarea_responsable))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                estado.responsables.forEachIndexed { i, persona ->
                    SegmentedButton(
                        selected = estado.responsableId == persona.id,
                        onClick = { alElegirResponsable(persona.id) },
                        shape = SegmentedButtonDefaults.itemShape(i, estado.responsables.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(persona.nombre) }
                }
            }
        }
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoQueAbreDialogo(
                etiqueta = stringResource(R.string.tarea_fecha),
                valor = estado.fecha.format(formatoFecha),
                icono = Icons.Outlined.CalendarMonth,
                error = estado.errorFecha != null,
                alTocar = { eligiendoFecha = true },
                modifier = Modifier.weight(1f),
            )
            CampoQueAbreDialogo(
                etiqueta = stringResource(R.string.tarea_hora),
                valor = estado.hora.format(formatoHora),
                icono = Icons.Outlined.Schedule,
                error = estado.errorFecha != null,
                alTocar = { eligiendoHora = true },
                modifier = Modifier.weight(1f),
            )
        }
        estado.errorFecha?.let {
            Text(textoDe(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
        FilaDeAviso(estado.avisar, alCambiarAviso, Modifier.padding(top = 16.dp))
        estado.errorAlGuardar?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
        Button(
            onClick = alGuardar,
            enabled = estado.puedeGuardar,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.tarea_guardar)) }
    }

    if (eligiendoFecha) {
        val hoy = LocalDate.now()
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = estado.fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            // No se ofrecen días que ya pasaron.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(hoy)
            },
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    selector.selectedDateMillis?.let { alElegirFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    eligiendoFecha = false
                }) { Text(stringResource(R.string.agregar_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text(stringResource(R.string.agregar_cancelar)) } },
        ) { DatePicker(selector) }
    }

    if (eligiendoHora) {
        val selector = rememberTimePickerState(initialHour = estado.hora.hour, initialMinute = estado.hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { eligiendoHora = false },
            confirmButton = {
                TextButton(onClick = {
                    alElegirHora(LocalTime.of(selector.hour, selector.minute))
                    eligiendoHora = false
                }) { Text(stringResource(R.string.agregar_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoHora = false }) { Text(stringResource(R.string.agregar_cancelar)) } },
            text = { TimePicker(selector) },
        )
    }
}

/** Parece un campo, pero al tocarlo abre el selector (no se escribe la fecha a mano). */
@Composable
private fun CampoQueAbreDialogo(
    etiqueta: String,
    valor: String,
    icono: ImageVector,
    error: Boolean,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(icono, contentDescription = null) },
            isError = error,
            singleLine = true,
            // TalkBack lee la capa de encima; si leyera también el campo, lo diría dos veces.
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        )
        // Una capa encima para que todo el campo responda al toque y TalkBack lo anuncie como botón, con su
        // nombre y su valor ("Fecha, mié 25/11"). Sin la descripción decía solo "botón" (RNF08).
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = etiqueta, onClick = alTocar)
                .semantics { contentDescription = "$etiqueta, $valor" },
        )
    }
}

/** "Avisar a la hora" con su interruptor; toda la fila se puede tocar (RNF07). */
@Composable
private fun FilaDeAviso(avisar: Boolean, alCambiar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .toggleable(value = avisar, role = Role.Switch, onValueChange = alCambiar)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.tarea_avisar), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // El estado lo maneja la fila entera; el interruptor solo lo muestra.
        Switch(checked = avisar, onCheckedChange = null)
    }
}

@Composable
private fun VistaPrevia(estado: NuevaTareaUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) { FormularioDeTarea(estado, {}, {}, {}, {}, {}, {}) }
    }
}

private val ejemplo = NuevaTareaUiState(
    responsables = listOf(Responsable("a", "Ana"), Responsable("t", "Tomás")),
    responsableId = "a",
    fecha = LocalDate.of(2026, 11, 26),
    hora = LocalTime.of(18, 0),
)

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeTareaPreview() = VistaPrevia(ejemplo)

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeTareaOscuroPreview() = VistaPrevia(ejemplo.copy(nombre = "Pagar ABL", avisar = false), oscuro = true)

@Preview(name = "Errores", showBackground = true, widthDp = 412)
@Composable
private fun FormularioDeTareaErroresPreview() = VistaPrevia(
    ejemplo.copy(errorNombre = ErrorDeFormulario.NOMBRE_VACIO, errorFecha = ErrorDeFormulario.FECHA_PASADA),
)
