package com.autokm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autokm.app.data.dao.AbrechnungDao
import com.autokm.app.data.dao.FahrtDao
import com.autokm.app.data.dao.OrtDao
import com.autokm.app.data.dao.OrtNutzungDao
import com.autokm.app.data.model.Abrechnung
import com.autokm.app.data.model.Fahrt
import com.autokm.app.data.model.Ort
import com.autokm.app.data.model.OrtNutzung

@Database(
    entities = [Fahrt::class, Ort::class, Abrechnung::class, OrtNutzung::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fahrtDao(): FahrtDao
    abstract fun ortDao(): OrtDao
    abstract fun abrechnungDao(): AbrechnungDao
    abstract fun ortNutzungDao(): OrtNutzungDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fahrtenfuchs.db",
                )
                    // Solange sich die App noch in aktiver Entwicklung befindet und keine echten
                    // Nutzdaten existieren, ist eine harte Neuanlage bei Schemaänderungen simpler
                    // als Migrationsskripte für jeden Zwischenstand. Vor dem ersten echten Release
                    // durch richtige Migrationen ersetzen.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
