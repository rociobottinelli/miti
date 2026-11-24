package ar.edu.uade.miti.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.hoy.RutinaDeHoy
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Widget 4x2 (Figma "Widget"): las rutinas de hoy, cada una se marca con un toque (RNF03). */
class WidgetDeRutinas : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val observar = EntryPointAccessors.fromApplication(context, DependenciasDelWidget::class.java).observarEstado()
        provideContent {
            val estado by remember { observar() }.collectAsState(initial = null)
            GlanceTheme(colors = coloresDelWidget) {
                FondoDeWidget { estado?.let { ContenidoDelWidget(it) } }
            }
        }
    }
}

class ReceptorWidgetDeRutinas : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetDeRutinas()

    /** Android también lo llama después de reiniciar el celular: ahí se vuelve a programar la alarma (RNF01). */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        AlarmaDeMedianoche.programar(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmaDeMedianoche.ACCION) return super.onReceive(context, intent)
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                actualizarWidgets(context)
                AlarmaDeMedianoche.programar(context)
            } finally {
                pendiente.finish()
            }
        }
    }
}

@Composable
private fun ContenidoDelWidget(estado: EstadoDelWidget) {
    Column(GlanceModifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Encabezado(estado)
        when (estado) {
            EstadoDelWidget.SinHogar -> MensajeCentrado(texto(R.string.widget_sin_hogar), null, null)
            EstadoDelWidget.SinRutinas -> MensajeCentrado(texto(R.string.widget_sin_rutinas), texto(R.string.widget_sin_rutinas_detalle), null)
            is EstadoDelWidget.ConRutinas -> if (estado.todoAlDia) {
                MensajeCentrado(
                    texto(R.string.widget_todo_al_dia),
                    texto(R.string.widget_todo_al_dia_detalle, estado.hechas, estado.total),
                    R.drawable.ic_widget_todo_al_dia,
                )
            } else {
                LazyColumn {
                    items(estado.rutinas, itemId = { it.id.hashCode().toLong() }) { FilaDeRutina(it) }
                }
            }
        }
    }
}

/** "Miti · Hoy" con la flecha; tocarlo abre la app. Debajo, el aviso de sincronización si hay (C1). */
@Composable
private fun Encabezado(estado: EstadoDelWidget) {
    val aviso = (estado as? EstadoDelWidget.ConRutinas)?.let { e -> e.aviso?.let { textoDelAviso(it, e.hoy) } }
    Row(
        GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).clickable(abrirLaApp).describir(texto(R.string.widget_abrir)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(26.dp).background(GlanceTheme.colors.primary).cornerRadius(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(R.drawable.ic_hoy), contentDescription = null, colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary), modifier = GlanceModifier.size(16.dp))
        }
        Column(GlanceModifier.defaultWeight().padding(start = 8.dp)) {
            Text(
                texto(R.string.widget_titulo),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (aviso != null) {
                Text(aviso, style = TextStyle(color = GlanceTheme.colors.error, fontSize = 11.sp), maxLines = 1)
            }
        }
        Image(ImageProvider(R.drawable.ic_widget_abrir), contentDescription = null, colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant), modifier = GlanceModifier.size(22.dp))
    }
}

/**
 * Toda la fila es el botón (48 dp de alto, RNF07). Una rutina completa no hace nada al tocarla: las marcas
 * no se borran (D08) y el caso de uso ya no agrega otra.
 */
@Composable
private fun FilaDeRutina(rutina: RutinaDeHoy) {
    val colores = GlanceTheme.colors
    val accion = if (rutina.completa) {
        GlanceModifier
    } else {
        GlanceModifier
            .clickable(actionRunCallback<MarcarRutinaDesdeWidget>(actionParametersOf(MarcarRutinaDesdeWidget.RUTINA to rutina.id)))
            .describir(texto(R.string.widget_marcar, rutina.titulo))
    }
    Row(accion.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(if (rutina.completa) R.drawable.ic_widget_casilla_marcada else R.drawable.ic_widget_casilla),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (rutina.completa) colores.primary else colores.outline),
            modifier = GlanceModifier.size(24.dp),
        )
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                rutina.titulo,
                style = TextStyle(
                    color = if (rutina.completa) colores.onSurfaceVariant else colores.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (rutina.completa) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
            )
            Text(detalleDeRutina(rutina), style = TextStyle(color = colores.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
        }
        if (rutina.pendienteDeSync) {
            Image(
                ImageProvider(R.drawable.ic_widget_pendiente),
                contentDescription = texto(R.string.pendiente_de_sincronizar),
                colorFilter = ColorFilter.tint(colores.onSurfaceVariant),
                modifier = GlanceModifier.size(16.dp),
            )
        }
    }
}
