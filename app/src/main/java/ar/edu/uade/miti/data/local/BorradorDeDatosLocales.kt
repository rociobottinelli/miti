package ar.edu.uade.miti.data.local

import ar.edu.uade.miti.data.avisos.ProgramadorDeAvisos
import ar.edu.uade.miti.data.local.datastore.EstadoDeSyncLocal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Borra todo lo del hogar guardado en el celular (al cerrar sesión, #16). */
fun interface BorradorDeDatosLocales {
    suspend fun borrarTodo()
}

class BorradorDeDatosLocalesRoom @Inject constructor(
    private val base: AppDatabase,
    private val estadoDeSync: EstadoDeSyncLocal,
    private val avisos: ProgramadorDeAvisos,
) : BorradorDeDatosLocales {
    override suspend fun borrarTodo() {
        // clearAllTables bloquea el hilo: se corre fuera del principal.
        withContext(Dispatchers.IO) { base.clearAllTables() }
        // Sin esto, otra cuenta que ingrese en este celular no descargaría lo anterior al último cursor.
        estadoDeSync.borrar()
        // Sin sesión no hay tareas propias: se cancelan las alarmas que quedaban.
        avisos.pedir()
    }
}
