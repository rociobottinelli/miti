package ar.edu.uade.miti.data.avisos

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** #24: la sincronización bajó tareas nuevas asignadas a quien usa el celular. */
fun interface AvisoDeTareasNuevas {
    fun avisar(titulos: List<String>)
}

class AvisoDeTareasNuevasAndroid @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : AvisoDeTareasNuevas {
    override fun avisar(titulos: List<String>) = Notificaciones.mostrarTareasNuevas(contexto, titulos)
}
