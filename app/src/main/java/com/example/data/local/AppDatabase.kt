package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BatteryDao
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.local.entity.SavedChargerEntity

/**
 * Zentrale Room-Datenbankinstanz zur dauerhaften Speicherung von Ladekurven, Sitzungen, SOH-Historie und Netzteil-Profilen.
 * Verwendet explizite nicht-destruktive Migrationen ab v3 -> v4, um bestehende Verlaufsdaten zu bewahren.
 */
@Database(
    entities = [
        ChargingSessionEntity::class,
        BatterySampleEntity::class,
        BatteryHealthSnapshotEntity::class,
        SavedChargerEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE charging_sessions ADD COLUMN chargerProfileName TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_chargers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `ratedWatts` INTEGER NOT NULL,
                        `grade` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `peakCurrentMilliAmp` REAL NOT NULL,
                        `peakPowerWatts` REAL NOT NULL,
                        `cableEfficiencyPercent` INTEGER NOT NULL,
                        `voltageSagMv` INTEGER NOT NULL,
                        `lastTestedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_saved_chargers_lastTestedAt` ON `saved_chargers` (`lastTestedAt`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amperemaster_battery.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
