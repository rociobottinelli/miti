package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.IndicadorPendienteDeSync
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalTime

/**
 * Una rutina del día (Figma "RoutineCard"). Toda la tarjeta se toca para marcar (RNF07: mucho más que
 * 48 dp). Se distingue de una tarea por la forma, no solo por el color (RNF08): a la izquierda tiene la
 * casilla con el progreso, separada por una línea, y la tarea en cambio tiene un ícono y un avatar.
 */
@Composable
fun TarjetaDeRutina(
    rutina: RutinaDeHoy,
    alMarcar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detalle = detalleDeRutina(rutina)
    val estado = when {
        rutina.completa -> stringResource(R.string.hoy_rutina_hecha)
        else -> stringResource(R.string.hoy_progreso, rutina.hechas, rutina.vecesPorDia)
    }
    Surface(
        onClick = alMarcar,
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = estado },
        shape = MaterialTheme.shapes.large,
        color = if (rutina.completa) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .height(IntrinsicSize.Min)
                .alpha(if (rutina.completa) 0.8f else 1f),
        ) {
            Box(
                modifier = Modifier.width(72.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                CasillaDeProgreso(hechas = rutina.hechas, completa = rutina.completa)
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = Espaciado.rellenoInterno),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = rutina.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (rutina.completa) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (rutina.completa) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        // onSurfaceVariant y no outline como en el Figma: llega a 4,5:1 de contraste (RNF08, #9).
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // La marca está en este celular pero todavía no llegó al otro (RNF09).
                    if (rutina.pendienteDeSync) IndicadorPendienteDeSync()
                }
            }
        }
    }
}

/** "Pendiente", "1 de 2 · Tomás 8:15" o "Hecha · Ana 21:03". */
@Composable
private fun detalleDeRutina(rutina: RutinaDeHoy): String {
    val marca = rutina.ultimaMarca ?: return stringResource(R.string.hoy_rutina_pendiente)
    val quienYCuando = marca.quien?.let { "$it ${marca.hora.comoHora()}" } ?: marca.hora.comoHora()
    return if (rutina.completa) {
        stringResource(R.string.hoy_rutina_hecha_por, quienYCuando)
    } else {
        stringResource(R.string.hoy_rutina_parcial, rutina.hechas, rutina.vecesPorDia, quienYCuando)
    }
}

/**
 * Vacía, con la cantidad de marcas (rutinas de 2 o 3 veces) o tildada. El estado lo lee TalkBack desde
 * la tarjeta, por eso la casilla no se anuncia sola.
 */
@Composable
private fun CasillaDeProgreso(hechas: Int, completa: Boolean, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(8.dp)
    val (fondo, borde) = when {
        completa -> colores.primary to colores.primary
        hechas > 0 -> colores.primaryContainer to colores.primary
        else -> colores.surfaceContainerLow to colores.outline
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .background(fondo, forma)
            .border(2.dp, borde, forma)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        when {
            completa -> Icon(Icons.Filled.Check, contentDescription = null, tint = colores.onPrimary, modifier = Modifier.size(16.dp))
            hechas > 0 -> Text(
                text = hechas.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colores.onPrimaryContainer,
            )
        }
    }
}

private val ejemplos = listOf(
    RutinaDeHoy("1", "Darle de comer a Pancho", hechas = 1, vecesPorDia = 2, Marca("Tomás", LocalTime.of(8, 15)), pendienteDeSync = true),
    RutinaDeHoy("2", "Regar las plantas", hechas = 0, vecesPorDia = 1, ultimaMarca = null),
    RutinaDeHoy("3", "Sacar la basura", hechas = 1, vecesPorDia = 1, Marca("Ana", LocalTime.of(21, 3))),
)

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun TarjetaDeRutinaPreview() {
    MitiTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ejemplos.forEach { TarjetaDeRutina(it, alMarcar = {}) }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1415)
@Composable
private fun TarjetaDeRutinaOscuroPreview() {
    MitiTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ejemplos.forEach { TarjetaDeRutina(it, alMarcar = {}) }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, fontScale = 2f)
@Composable
private fun TarjetaDeRutinaFuenteGrandePreview() {
    MitiTheme {
        TarjetaDeRutina(ejemplos.first(), alMarcar = {}, modifier = Modifier.padding(16.dp))
    }
}
