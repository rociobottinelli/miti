package ar.edu.uade.miti.data.local

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Borra todo lo del hogar guardado en el celular (al cerrar sesión, #16). */
fun interface BorradorDeDatosLocales {
    suspend fun borrarTodo()
}

class BorradorDeDatosLocalesRoom @Inject constructor(private val base: AppDatabase) : BorradorDeDatosLocales {
    // clearAllTables bloquea el hilo: se corre fuera del principal.
    override suspend fun borrarTodo() = withContext(Dispatchers.IO) { base.clearAllTables() }
}
