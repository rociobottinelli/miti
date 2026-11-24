package ar.edu.uade.miti.data.avisos

import ar.edu.uade.miti.data.sync.AvisoDeCambios
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import javax.inject.Inject

/**
 * Lo que pasa después de cada cambio local: se pide subirlo (#18) y se ponen las alarmas al día (#23),
 * porque el cambio pudo ser crear, resolver o borrar una tarea con aviso.
 */
class AvisoDeCambiosLocales @Inject constructor(
    private val sincronizacion: ProgramadorDeSync,
    private val avisos: ProgramadorDeAvisos,
) : AvisoDeCambios {
    override suspend fun huboCambios() {
        sincronizacion.sincronizarAhora()
        avisos.pedir()
    }
}
