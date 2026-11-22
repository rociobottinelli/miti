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
        HogarEntidad::class,
        IntegranteEntidad::class,
        RutinaEntidad::class,
        RegistroRutinaEntidad::class,
        PendienteEntidad::class,
        GastoEntidad::class,
        PagoEntidad::class,
    ],
    version = 1,
)
@TypeConverters(Convertidores::class)
abstract class MitiDatabase : RoomDatabase() {
    abstract fun hogarDao(): HogarDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun gastoDao(): GastoDao
}
