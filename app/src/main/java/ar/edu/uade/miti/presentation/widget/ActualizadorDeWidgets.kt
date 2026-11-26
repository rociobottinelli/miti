package ar.edu.uade.miti.presentation.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Mantiene los widgets al día mientras el proceso de Miti está vivo (RNF05: lo marcado en la app se ve en
 * el widget en menos de 5 segundos). Escucha el mismo estado que los widgets, que sale de Room, así que
 * reacciona igual a lo que se marca en la app, en el widget o lo que trae la sincronización.
 *
 * Con el proceso muerto no hay nada que cambie Room: lo único que pasa es la medianoche, que cubre
 * [AlarmaDeMedianoche].
 */
@Singleton
class ActualizadorDeWidgets @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
    private val observarEstado: ObservarEstadoDelWidget,
) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun empezar() {
        alcance.launch {
            observarEstado().collectLatest { actualizarWidgets(contexto) }
        }
    }
}
