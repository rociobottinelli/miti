package ar.edu.uade.miti.data.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Cada cuánto se sincroniza mientras la app está en pantalla (C1). Supabase no le avisa a un celular que el
 * otro cambió algo (push desde el servidor queda fuera de alcance), así que, con la app visible, se pregunta
 * seguido: los cambios del otro aparecen en 15 s como mucho. En segundo plano queda la periódica de 15 min,
 * para no gastar batería ni datos. Cada consulta baja solo lo nuevo (descarga incremental), así que es liviana.
 */
val SYNC_EN_PANTALLA: Duration = 15.seconds

/**
 * Corre [accion] ya y después cada [intervalo], hasta que se cancele la corrutina. Quien la llama decide
 * cuándo corre: MainActivity la ata a que la app esté en pantalla (repeatOnLifecycle STARTED), así se corta
 * sola al pasar a segundo plano y vuelve a arrancar, con una sincronización inmediata, al volver.
 */
suspend fun repetirCada(intervalo: Duration, accion: suspend () -> Unit): Nothing {
    while (true) {
        accion()
        delay(intervalo)
    }
}
