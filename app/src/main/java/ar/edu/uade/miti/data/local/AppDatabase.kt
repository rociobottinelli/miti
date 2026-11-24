package ar.edu.uade.miti.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ar.edu.uade.miti.data.local.converter.Convertidores
import ar.edu.uade.miti.data.local.dao.EstadoDeSyncDao
import ar.edu.uade.miti.data.local.dao.ConflictoGastoDao
import ar.edu.uade.miti.data.local.dao.GastoDao
import ar.edu.uade.miti.data.local.dao.HogarDao
import ar.edu.uade.miti.data.local.dao.PendienteDao
import ar.edu.uade.miti.data.local.dao.RutinaDao
import ar.edu.uade.miti.data.local.entity.ConflictoGastoEntity
import ar.edu.uade.miti.data.local.entity.GastoEntity
import ar.edu.uade.miti.data.local.entity.HogarEntity
import ar.edu.uade.miti.data.local.entity.IntegranteEntity
import ar.edu.uade.miti.data.local.entity.PagoEntity
import ar.edu.uade.miti.data.local.entity.PendienteEntity
import ar.edu.uade.miti.data.local.entity.RegistroRutinaEntity
import ar.edu.uade.miti.data.local.entity.RutinaEntity

/**
 * La base local: única fuente de verdad de la app (D04). Si se cambia una tabla hay que subir
 * [version] y escribir una migración; el esquema de cada versión queda en `app/schemas/`.
 */
@Database(
    entities = [
        HogarEntity::class,
        IntegranteEntity::class,
        RutinaEntity::class,
        RegistroRutinaEntity::class,
        PendienteEntity::class,
        GastoEntity::class,
        PagoEntity::class,
        ConflictoGastoEntity::class,
    ],
    version = 2,
)
@TypeConverters(Convertidores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hogarDao(): HogarDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun gastoDao(): GastoDao
    abstract fun estadoDeSyncDao(): EstadoDeSyncDao
    abstract fun conflictoGastoDao(): ConflictoGastoDao
}
