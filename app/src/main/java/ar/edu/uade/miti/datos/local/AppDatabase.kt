package ar.edu.uade.miti.datos.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
    ],
    version = 1,
)
@TypeConverters(Convertidores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hogarDao(): HogarDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun gastoDao(): GastoDao
}
