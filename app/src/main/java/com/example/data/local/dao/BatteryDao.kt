package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.local.entity.SavedChargerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für den Zugriff auf Ladesitzungen, Kurven-Messwerte, SOH-Historie und Netzteil-Profile.
 */
@Dao
interface BatteryDao {

    // --- Ladesitzungen ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargingSessionEntity)

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChargingSessionEntity?

    @Query("DELETE FROM charging_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearAllSessions()

    // --- Kurven-Messpunkte ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: BatterySampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<BatterySampleEntity>)

    @Query("SELECT * FROM battery_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSamplesForSession(sessionId: Long): Flow<List<BatterySampleEntity>>

    @Query("SELECT * FROM battery_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSessionOnce(sessionId: Long): List<BatterySampleEntity>

    @Query("SELECT * FROM battery_samples WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getSamplesSince(sinceTimestamp: Long): Flow<List<BatterySampleEntity>>

    @Query("SELECT * FROM battery_samples ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestSamples(limit: Int = 100): Flow<List<BatterySampleEntity>>

    @Query("DELETE FROM battery_samples WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOldSamples(olderThanTimestamp: Long)

    @Query("DELETE FROM battery_samples")
    suspend fun clearAllSamples()

    // --- SOH / Kapazitäts-Historie ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthSnapshot(snapshot: BatteryHealthSnapshotEntity): Long

    @Query("SELECT * FROM battery_health_snapshots ORDER BY timestamp ASC")
    fun getAllHealthSnapshots(): Flow<List<BatteryHealthSnapshotEntity>>

    @Query("SELECT * FROM battery_health_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHealthSnapshot(): BatteryHealthSnapshotEntity?

    @Query("DELETE FROM battery_health_snapshots")
    suspend fun clearHealthSnapshots()

    // --- Gespeicherte Netzteile & Kabel ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedCharger(charger: SavedChargerEntity): Long

    @Query("SELECT * FROM saved_chargers ORDER BY lastTestedAt DESC")
    fun getAllSavedChargers(): Flow<List<SavedChargerEntity>>

    @Query("DELETE FROM saved_chargers WHERE id = :chargerId")
    suspend fun deleteSavedChargerById(chargerId: Long)
}
