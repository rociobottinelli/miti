package ar.edu.uade.miti.presentation.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** "Registrar pago" (Figma, "PaymentSheet"): quién le pagó a quién, cuánto y cuándo (RF04). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaDePago(pago: PagoEnCurso, acciones: AccionesDeBalance) {
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = acciones.cerrarPago, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding()) {
            Text(stringResource(R.string.pago_registrar), style = MaterialTheme.typography.headlineSmall)

            Text(
                stringResource(R.string.pago_direccion),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = true, onClick = {}, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.pago_le_pago, pago.de, pago.a), maxLines = 1)
                }
                SegmentedButton(selected = false, onClick = acciones.cambiarDireccionDelPago, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(stringResource(R.string.pago_le_pago, pago.a, pago.de), maxLines = 1)
                }
            }

            OutlinedTextField(
                value = pago.monto,
                onValueChange = acciones.cambiarMontoDelPago,
                label = { Text(stringResource(R.string.pago_monto)) },
                prefix = { Text("$ ") },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = pago.error != null,
                supportingText = pago.error?.let { { Text(stringResource(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                Text(
                    stringResource(R.string.pago_fecha, pago.fecha.format(DateTimeFormatter.ofPattern("d/M/yyyy"))),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                TextButton(onClick = acciones.cerrarPago) { Text(stringResource(R.string.accion_cancelar)) }
                Button(onClick = acciones.registrarPago, enabled = !pago.guardando) { Text(stringResource(R.string.pago_confirmar)) }
            }
        }
    }

    if (eligiendoFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = pago.fecha.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let {
                        acciones.cambiarFechaDelPago(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    eligiendoFecha = false
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        ) { DatePicker(estadoFecha) }
    }
}

