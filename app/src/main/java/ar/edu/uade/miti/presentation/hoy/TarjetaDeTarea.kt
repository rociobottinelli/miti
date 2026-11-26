package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.IndicadorPendienteDeSync
import ar.edu.uade.miti.presentation.components.AvatarDeIntegrante
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Una próxima tarea (Figma "TaskCard"): ícono a la izquierda (campana si tiene aviso), título, cuándo
 * vence y el avatar de quien la tiene asignada. Una atrasada se marca con texto, no solo con color (RNF08).
 */
/**
 * Una próxima tarea. Tocarla la marca como hecha (RF03), igual que una rutina; la casilla de la izquierda lo
 * indica y el snackbar permite deshacerlo.
 */
@Composable
fun TarjetaDeTarea(tarea: TareaDeHoy, alCompletar: () -> Unit, modifier: Modifier = Modifier, alBorrar: () -> Unit = {}) {
    val atrasada = tarea.cuando is CuandoVence.Atrasada
    val marcarComoHecha = stringResource(R.string.hoy_tarea_marcar)
    Surface(
        onClick = alCompletar,
        modifier = modifier
            .fillMaxWidth()
            // TalkBack la lee como una sola cosa: "Pagar ABL, Jueves 18:00 · Tomás", y anuncia la acción.
            .semantics(mergeDescendants = true) { onClick(label = marcarComoHecha) { alCompletar(); true } },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // La casilla vacía, como en las rutinas: se toca para marcarla como hecha.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clearAndSetSemantics {},
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (atrasada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(
                        imageVector = if (tarea.conAviso) Icons.Outlined.NotificationsActive else Icons.Outlined.Schedule,
                        contentDescription = if (tarea.conAviso) stringResource(R.string.hoy_tarea_con_aviso) else null,
                        tint = color,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = detalleDeTarea(tarea),
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                    )
                    if (tarea.pendienteDeSync) IndicadorPendienteDeSync()
                }
            }
            tarea.responsable?.let { AvatarDeIntegrante(nombre = it.nombre, posicion = it.posicion) }
            MenuDeItem(
                titulo = tarea.titulo,
                opciones = listOf(
                    OpcionDeMenu(
                        texto = stringResource(R.string.hoy_borrar_tarea),
                        confirmar = Confirmacion(
                            titulo = stringResource(R.string.hoy_borrar_titulo, tarea.titulo),
                            texto = stringResource(R.string.hoy_borrar_texto),
                            boton = stringResource(R.string.hoy_borrar),
                        ),
                        alElegir = alBorrar,
                    ),
                ),
            )
        }
    }
}

/** "Jueves 18:00 · Tomás". */
@Composable
private fun detalleDeTarea(tarea: TareaDeHoy): String {
    val cuando = textoDe(tarea.cuando)
    return tarea.responsable?.let { stringResource(R.string.hoy_tarea_detalle, cuando, it.nombre) } ?: cuando
}

@Composable
private fun textoDe(cuando: CuandoVence): String {
    val hora = cuando.hora.comoHora()
    return when (cuando) {
        is CuandoVence.Atrasada -> stringResource(R.string.hoy_tarea_atrasada, cuando.fecha.comoFechaCorta(), hora)
        is CuandoVence.Hoy -> stringResource(R.string.hoy_tarea_hoy, hora)
        is CuandoVence.Maniana -> stringResource(R.string.hoy_tarea_maniana, hora)
        is CuandoVence.EstaSemana -> "${cuando.dia.comoNombre()} $hora"
        is CuandoVence.MasAdelante -> "${cuando.fecha.comoFechaCorta()} $hora"
    }
}

private val tomas = IntegranteEnPantalla("t", "Tomás", 1)
private val ana = IntegranteEnPantalla("a", "Ana", 0)
private val ejemplos = listOf(
    TareaDeHoy("1", "Pagar ABL", CuandoVence.EstaSemana(DayOfWeek.THURSDAY, LocalTime.of(18, 0)), tomas, conAviso = true),
    TareaDeHoy("2", "Llevar a Pancho al veterinario", CuandoVence.EstaSemana(DayOfWeek.SATURDAY, LocalTime.of(10, 0)), ana, conAviso = true),
    TareaDeHoy("3", "Llamar al plomero", CuandoVence.Atrasada(LocalDate.of(2026, 9, 28), LocalTime.of(9, 0)), ana, conAviso = false),
)

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun TarjetaDeTareaPreview() {
    MitiTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ejemplos.forEach { TarjetaDeTarea(it, alCompletar = {}) }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1415)
@Composable
private fun TarjetaDeTareaOscuroPreview() {
    MitiTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ejemplos.forEach { TarjetaDeTarea(it, alCompletar = {}) }
        }
    }
}
