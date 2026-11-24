package ar.edu.uade.miti.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import ar.edu.uade.miti.MainActivity
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.presentation.components.cuando
import ar.edu.uade.miti.presentation.hoy.RutinaDeHoy
import ar.edu.uade.miti.presentation.hoy.comoHora
import ar.edu.uade.miti.presentation.theme.EsquemaClaro
import ar.edu.uade.miti.presentation.theme.EsquemaOscuro
import java.time.LocalDate

/** La paleta de la app también en los widgets: claro u oscuro según el sistema, sin Material You. */
internal val coloresDelWidget = ColorProviders(light = EsquemaClaro, dark = EsquemaOscuro)

/** Actualiza los widgets. Si no hay ninguno en la pantalla de inicio, no hace nada. */
suspend fun actualizarWidgets(context: Context) {
    WidgetDeRutinas().updateAll(context)
}

/** El fondo redondeado de los dos widgets (en Android 11 o anterior las esquinas quedan rectas). */
internal fun GlanceModifier.fondoDeWidget() = fillMaxSize()
    .appWidgetBackground()
    .cornerRadius(24.dp)

@Composable
internal fun FondoDeWidget(contenido: @Composable () -> Unit) {
    Box(GlanceModifier.fondoDeWidget().background(GlanceTheme.colors.widgetBackground)) { contenido() }
}

internal val abrirLaApp = actionStartActivity<MainActivity>()

@Composable
internal fun texto(id: Int, vararg argumentos: Any): String = LocalContext.current.getString(id, *argumentos)

/** "Pendiente", "1 de 2 · Tomás 8:15" o "Ana 7:40" (en el widget, tachada ya dice que está hecha). */
@Composable
internal fun detalleDeRutina(rutina: RutinaDeHoy): String {
    val marca = rutina.ultimaMarca ?: return texto(R.string.hoy_rutina_pendiente)
    val quienYCuando = marca.quien?.let { "$it ${marca.hora.comoHora()}" } ?: marca.hora.comoHora()
    return if (rutina.completa) quienYCuando else texto(R.string.hoy_rutina_parcial, rutina.hechas, rutina.vecesPorDia, quienYCuando)
}

/** Versión corta de los avisos de Hoy, para que entre en el encabezado del widget (C1). */
@Composable
internal fun textoDelAviso(aviso: AvisoDeSync, hoy: LocalDate): String = when (aviso) {
    is AvisoDeSync.SinConexion -> aviso.ultimaSincronizacion?.let { texto(R.string.widget_sin_conexion_con_hora, cuando(it, hoy)) }
        ?: texto(R.string.widget_sin_conexion)
    AvisoDeSync.NoSePudoSincronizar -> texto(R.string.widget_sin_sincronizar)
    is AvisoDeSync.Desactualizado -> aviso.ultimaSincronizacion?.let { texto(R.string.widget_desactualizado_con_hora, cuando(it, hoy)) }
        ?: texto(R.string.widget_desactualizado)
}

/** "Todo al día", "Todavía no hay rutinas" o "Abrí Miti para empezar", centrado. Tocarlo abre la app. */
@Composable
internal fun MensajeCentrado(titulo: String, detalle: String?, icono: Int?, modifier: GlanceModifier = GlanceModifier) {
    Column(
        modifier.fillMaxSize().clickable(abrirLaApp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) {
            Image(ImageProvider(icono), contentDescription = null, colorFilter = ColorFilter.tint(GlanceTheme.colors.primary), modifier = GlanceModifier.size(32.dp))
            Spacer(GlanceModifier.height(8.dp))
        }
        Text(
            titulo,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 2,
        )
        if (detalle != null) {
            Text(
                detalle,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
                maxLines = 2,
                modifier = GlanceModifier.padding(top = 2.dp),
            )
        }
    }
}

/** Para TalkBack (RNF08): describe la acción, no el dibujo. */
internal fun GlanceModifier.describir(descripcion: String) = semantics { contentDescription = descripcion }
