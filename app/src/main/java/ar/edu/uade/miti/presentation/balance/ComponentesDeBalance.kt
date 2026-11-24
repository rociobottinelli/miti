package ar.edu.uade.miti.presentation.balance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.domain.model.CampoDeGasto
import ar.edu.uade.miti.domain.model.ConflictoDeGasto
import ar.edu.uade.miti.domain.model.Gasto
import ar.edu.uade.miti.domain.model.Reparto
import ar.edu.uade.miti.presentation.components.formatearPesos
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** La tarjeta de arriba (Figma, "BalanceCard"): quién le debe a quién, Registrar pago y cuánto puso cada uno. */
@Composable
internal fun TarjetaDeBalance(estado: BalanceUiState.Contenido, alRegistrarPago: () -> Unit, modifier: Modifier = Modifier) {
    val es = Locale.forLanguageTag("es-AR")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(
                stringResource(R.string.balance_titulo_tarjeta, estado.mes.format(DateTimeFormatter.ofPattern("MMMM", es))).uppercase(es),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
                ) {
                    Icon(
                        if (estado.resultado == null) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    textoDelResultado(estado.resultado),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            FilledTonalButton(onClick = alRegistrarPago, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.pago_registrar), modifier = Modifier.padding(start = 6.dp))
            }
            if (estado.pagadoPor.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                Row(Modifier.height(IntrinsicSize.Min).padding(top = 12.dp)) {
                    estado.pagadoPor.forEachIndexed { i, total ->
                        if (i > 0) VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                        Column(Modifier.weight(1f).padding(start = if (i > 0) 14.dp else 0.dp)) {
                            Text(
                                stringResource(R.string.balance_pago, total.nombre),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                formatearPesos(total.montoCentavos),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Ana le debe $ 18.750 a Tomás" con el monto destacado, o "Están a mano". */
@Composable
private fun textoDelResultado(deuda: DeudaEnPantalla?) = buildAnnotatedString {
    if (deuda == null) {
        append(stringResource(R.string.balance_a_mano))
        return@buildAnnotatedString
    }
    append(stringResource(R.string.balance_le_debe_antes, deuda.deudor))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatearPesos(deuda.montoCentavos)) }
    append(stringResource(R.string.balance_le_debe_despues, deuda.acreedor))
}

/** Lo que no se saldó en meses anteriores y se suma a este (preentrega, RF04). */
@Composable
internal fun SaldoAnterior(saldo: DeudaEnPantalla, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.balance_saldo_anterior, saldo.deudor, formatearPesos(saldo.montoCentavos), saldo.acreedor),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** Un gasto del mes (Figma, "GastoItem"). Al tocarlo se edita. */
@Composable
internal fun ItemDeGasto(gasto: Movimiento.CuotaDeGasto, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = alTocar,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Sin categorías (fuera de alcance en la preentrega): un ícono genérico para todos.
            IconoDeMovimiento(Icons.AutoMirrored.Outlined.ReceiptLong)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Row {
                    Text(gasto.descripcion, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(formatearPesos(gasto.montoCentavos), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    stringResource(R.string.balance_pago_quien, gasto.pagador),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 7.dp)) {
                    Etiqueta(
                        Icons.AutoMirrored.Outlined.CallSplit,
                        if (gasto.soloDe == null) stringResource(R.string.gasto_a_medias) else stringResource(R.string.gasto_solo, gasto.soloDe),
                        destacada = gasto.soloDe == null,
                    )
                    if (gasto.enCuotas) {
                        Etiqueta(Icons.Outlined.CreditCard, stringResource(R.string.balance_cuota, gasto.numeroCuota, gasto.cuotasTotales), destacada = false)
                    }
                }
            }
        }
    }
}

/** Un pago registrado: se descuenta del balance del mes de su fecha. */
@Composable
internal fun ItemDePago(pago: Movimiento.PagoHecho, modifier: Modifier = Modifier) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            IconoDeMovimiento(Icons.Outlined.SwapHoriz)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(stringResource(R.string.balance_pago_de_a, pago.de, pago.a), style = MaterialTheme.typography.titleSmall)
                Text(
                    pago.fecha.format(DateTimeFormatter.ofPattern("d/M")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatearPesos(pago.montoCentavos), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

/**
 * C2: una edición propia que no entró porque la otra persona cambió el gasto antes. Muestra cada campo
 * que cambió con los dos valores (#51) y deja elegir; no se pierde en silencio.
 */
@Composable
internal fun TarjetaDeConflicto(
    conflicto: ConflictoEnPantalla,
    alRehacer: () -> Unit,
    alDescartar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SyncProblem, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    stringResource(R.string.conflicto_titulo, conflicto.descripcion),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            val detalle = when {
                conflicto.conflicto.eraBorrado -> stringResource(R.string.conflicto_era_borrado)
                conflicto.montoActualCentavos == null -> stringResource(R.string.conflicto_ya_no_esta, formatearPesos(conflicto.miMontoCentavos))
                else -> stringResource(R.string.conflicto_cambio_antes)
            }
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!conflicto.conflicto.eraBorrado) {
                conflicto.diferencias.forEach { diferencia ->
                    Text(
                        stringResource(
                            R.string.conflicto_diferencia,
                            stringResource(nombreDelCampo(diferencia.campo)),
                            textoDelValor(diferencia.mio),
                            textoDelValor(diferencia.quedo),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TextButton(onClick = alDescartar) { Text(stringResource(R.string.conflicto_descartar)) }
                TextButton(onClick = alRehacer) { Text(stringResource(R.string.conflicto_rehacer)) }
            }
        }
    }
}

private fun nombreDelCampo(campo: CampoDeGasto): Int = when (campo) {
    CampoDeGasto.DESCRIPCION -> R.string.conflicto_campo_descripcion
    CampoDeGasto.MONTO -> R.string.conflicto_campo_monto
    CampoDeGasto.PAGADOR -> R.string.conflicto_campo_pagador
    CampoDeGasto.REPARTO -> R.string.conflicto_campo_reparto
    CampoDeGasto.CUOTAS -> R.string.conflicto_campo_cuotas
}

/** Los textos de etiqueta ("A medias", "Cuota 1 de 3") van en minúscula porque quedan en medio de la oración. */
@Composable
private fun textoDelValor(valor: ValorDeCampo): String = when (valor) {
    is ValorDeCampo.Texto -> stringResource(R.string.conflicto_valor_texto, valor.texto)
    is ValorDeCampo.Pesos -> formatearPesos(valor.centavos)
    is ValorDeCampo.Persona -> valor.nombre
    ValorDeCampo.AMedias -> stringResource(R.string.gasto_a_medias).enMinuscula()
    is ValorDeCampo.SoloDe -> stringResource(R.string.gasto_solo, valor.nombre).enMinuscula()
    is ValorDeCampo.Cuotas ->
        if (valor.total == 1) stringResource(R.string.conflicto_sin_cuotas) else stringResource(R.string.balance_cuota, valor.actual, valor.total).enMinuscula()
}

private fun String.enMinuscula() = replaceFirstChar { it.lowercaseChar() }

@Composable
private fun IconoDeMovimiento(icono: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

/** Chip chico de solo lectura: "A medias", "Solo Ana", "Cuota 2 de 6". El texto dice todo, no solo el color. */
@Composable
private fun Etiqueta(icono: ImageVector, texto: String, destacada: Boolean) {
    Surface(
        color = if (destacada) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
        modifier = Modifier.clearAndSetSemantics { contentDescription = texto },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(texto, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaDeConflictoPreview() {
    val gasto = Gasto("g1", "h1", "Heladera", 3_100_000, "ana", Reparto.A_MEDIAS, mesInicio = YearMonth.of(2026, 11))
    MitiTheme {
        Column(Modifier.padding(16.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaDeConflicto(
                ConflictoEnPantalla(
                    ConflictoDeGasto("c1", gasto, gasto.copy(montoTotalCentavos = 2_900_000), false), "Heladera", 2_900_000, 3_100_000,
                    listOf(DiferenciaDeGasto(CampoDeGasto.MONTO, ValorDeCampo.Pesos(2_900_000), ValorDeCampo.Pesos(3_100_000))),
                ),
                alRehacer = {}, alDescartar = {},
            )
            // #51: mismo monto, cambió quién pagó y el reparto.
            TarjetaDeConflicto(
                ConflictoEnPantalla(
                    ConflictoDeGasto("c2", gasto, gasto, false), "Heladera", 3_100_000, 3_100_000,
                    listOf(
                        DiferenciaDeGasto(CampoDeGasto.PAGADOR, ValorDeCampo.Persona("Tomás"), ValorDeCampo.Persona("Ana")),
                        DiferenciaDeGasto(CampoDeGasto.REPARTO, ValorDeCampo.SoloDe("Tomás"), ValorDeCampo.AMedias),
                    ),
                ),
                alRehacer = {}, alDescartar = {},
            )
            SaldoAnterior(DeudaEnPantalla("ana", "Ana", "tomas", "Tomás", 500_000))
        }
    }
}
