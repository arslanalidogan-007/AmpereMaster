package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entität zur Speicherung einer zusammenhängenden Ladesitzung.
 */
@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Startzeitpunkt der Ladesitzung (Epoch Millis)
    val startTime: Long,

    // Endzeitpunkt der Ladesitzung (0 falls noch aktiv)
    val endTime: Long = 0,

    // Akkustand zu Beginn in Prozent
    val startLevelPercent: Double,

    // Akkustand zum Ende in Prozent
    val endLevelPercent: Double = 0.0,

    // Genutzte Ladequelle (AC, USB, WIRELESS)
    val chargingSource: String,

    // Maximale gemessene Stromstärke in mA
    val maxCurrentMilliAmp: Double = 0.0,

    // Durchschnittliche gemessene Stromstärke in mA
    val avgCurrentMilliAmp: Double = 0.0,

    // Maximale gemessene Leistung in Watt
    val maxPowerWatts: Double = 0.0,

    // v1.0.4: Zugeordnetes Netzteil- & Kabelprofil
    val chargerProfileName: String = ""
)

/**
 * Einzelner Messpunkt für die Ladekurve (Graph).
 */
@Entity(
    tableName = "battery_samples",
    indices = [Index(value = ["sessionId"]), Index(value = ["timestamp"])]
)
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Verweis auf die zugehörige Ladesitzung (oder 0 für freie Verlaufsmessung)
    val sessionId: Long = 0,

    // Zeitstempel der Messung
    val timestamp: Long,

    // Akkustand in Prozent (mit 2 Nachkommastellen)
    val levelPercent: Double,

    // Momentanstrom in mA
    val currentMilliAmp: Double,

    // Spannung in Volt
    val voltageVolt: Double,

    // Leistung in Watt
    val powerWatts: Double,

    // Temperatur in Grad Celsius
    val temperatureCelsius: Float,

    // Status, ob zu diesem Zeitpunkt geladen wurde
    val isCharging: Boolean
)

/**
 * Snapshot der geschätzten Akkukapazität und des State of Health (SOH) über die Zeit.
 */
@Entity(
    tableName = "battery_health_snapshots",
    indices = [Index(value = ["timestamp"])]
)
data class BatteryHealthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Zeitstempel der Messung
    val timestamp: Long,

    // Geschätzte verbleibende Realkapazität in mAh
    val estimatedCapacityMah: Int,

    // Werkseitige Nennkapazität in mAh
    val designCapacityMah: Int,

    // State of Health in Prozent (0f..100f)
    val sohPercent: Float,

    // Ladezyklen zum Zeitpunkt der Messung (-1 falls nicht unterstützt)
    val cycleCount: Int,

    // Akkutemperatur in Grad Celsius
    val temperatureCelsius: Float
)

/**
 * Gespeichertes Netzteil- und Ladekabel-Profil aus Benchmark oder Live-Sitzung (v1.0.4).
 */
@Entity(
    tableName = "saved_chargers",
    indices = [Index(value = ["lastTestedAt"])]
)
data class SavedChargerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val ratedWatts: Int = 25,
    val grade: String,
    val score: Int,
    val peakCurrentMilliAmp: Double,
    val peakPowerWatts: Double,
    val cableEfficiencyPercent: Int,
    val voltageSagMv: Int,
    val lastTestedAt: Long
)
