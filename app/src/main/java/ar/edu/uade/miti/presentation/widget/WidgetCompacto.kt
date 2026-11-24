package ar.edu.uade.miti.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ar.edu.uade.miti.R
import dagger.hilt.android.EntryPointAccessors

/** Widget compacto 2x2 (Figma "Widget compacto"): la próxima rutina que falta y el botón Hecho. */
class WidgetCompacto : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val observar = EntryPointAccessors.fromApplication(context, DependenciasDelWidget::class.java).observarEstado()
        provideContent {
            val estado by remember { observar() }.collectAsState(initial = null)
            GlanceTheme(colors = coloresDelWidget) {
                FondoDeWidget { estado?.let { ContenidoCompacto(it) } }
            }
        }
    }
}

class ReceptorWidgetCompacto : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetCompacto()

}

@Composable
private fun ContenidoCompacto(estado: EstadoDelWidget) {
    val colores = GlanceTheme.colors
    val conRutinas = estado as? EstadoDelWidget.ConRutinas
    val proxima = conRutinas?.proxima
    Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
        // Todo lo que no es el botón abre la app.
        Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(abrirLaApp)) {
            Text(
                texto(R.string.widget_titulo),
                style = TextStyle(color = colores.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            val titulo = when {
                estado == EstadoDelWidget.SinHogar -> texto(R.string.widget_sin_hogar)
                conRutinas == null -> texto(R.string.widget_sin_rutinas)
                proxima == null -> texto(R.string.widget_todo_al_dia)
                else -> proxima.titulo
            }
            Text(titulo, style = TextStyle(color = colores.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 2)
            if (conRutinas != null) {
                Text(
                    texto(R.string.widget_progreso, conRutinas.hechas, conRutinas.total),
                    style = TextStyle(color = colores.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        if (proxima != null) {
            FilledButton(
                text = texto(R.string.widget_hecho),
                onClick = actionRunCallback<MarcarRutinaDesdeWidget>(actionParametersOf(MarcarRutinaDesdeWidget.RUTINA to proxima.id)),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp).describir(texto(R.string.widget_marcar, proxima.titulo)),
                colors = ButtonDefaults.buttonColors(backgroundColor = colores.primary, contentColor = colores.onPrimary),
            )
        }
    }
}
