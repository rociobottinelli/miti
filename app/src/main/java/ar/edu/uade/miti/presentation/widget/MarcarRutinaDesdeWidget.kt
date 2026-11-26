package ar.edu.uade.miti.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import ar.edu.uade.miti.domain.usecase.rutinas.AlternarRutinaUseCase
import kotlinx.coroutines.flow.first

/**
 * Tocar una rutina desde el widget: un toque y sin abrir la app (RNF03). Usa [AlternarRutinaUseCase] como Hoy
 * (D03): la tilda si le faltan marcas y la destilda si estaba completa con una marca propia. Funciona sin
 * conexión y el cambio se envía en la próxima sincronización (RNF01).
 *
 * No pide confirmación con datos desactualizados (como sí hace Hoy, C1) porque el widget no puede mostrar
 * un diálogo; en su lugar, el encabezado avisa que puede no estar al día.
 */
class MarcarRutinaDesdeWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val rutinaId = parameters[RUTINA] ?: return
        val dependencias = EntryPointAccessors.fromApplication(context, DependenciasDelWidget::class.java)
        val yo = dependencias.observarUsuario()().first() ?: return
        dependencias.alternarRutina()(rutinaId, yo.id)
        // Los dos widgets muestran lo mismo: se actualizan juntos, sin esperar a que lo detecte la app.
        actualizarWidgets(context)
    }

    companion object {
        val RUTINA = ActionParameters.Key<String>("rutinaId")
    }
}
