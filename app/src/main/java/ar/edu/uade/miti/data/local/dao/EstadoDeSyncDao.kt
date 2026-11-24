package ar.edu.uade.miti.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoDeSyncDao {
    /**
     * Ids de los ítems con cambios sin enviar (#19). Una rutina figura también si tiene una marca sin
     * enviar, porque en pantalla la marca se ve en la tarjeta de la rutina. Room vuelve a emitir cuando
     * cambia cualquiera de estas tablas, por ejemplo al terminar una sincronización.
     */
    @Query(
        """
        SELECT id FROM rutinas WHERE syncState = 'PENDIENTE'
        UNION SELECT rutinaId FROM registros_rutina WHERE syncState = 'PENDIENTE'
        UNION SELECT id FROM pendientes WHERE syncState = 'PENDIENTE'
        UNION SELECT id FROM gastos WHERE syncState = 'PENDIENTE'
        UNION SELECT id FROM pagos WHERE syncState = 'PENDIENTE'
        """,
    )
    fun observarIdsSinEnviar(): Flow<List<String>>
}
