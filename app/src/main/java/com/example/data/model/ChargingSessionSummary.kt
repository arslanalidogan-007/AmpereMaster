package com.example.data.model

/**
 * Zusammenfassung einer abgeschlossenen Ladesitzung nach Abstecken des Ladekabels.
 */
data class ChargingSessionSummary(
    val sessionId: Long = 0L,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationMinutes: Long = 0L,
    val startLevelPercent: Double = 0.0,
    val endLevelPercent: Double = 0.0,
    val deltaPercent: Double = 0.0,
    val maxPowerWatts: Double = 0.0,
    val avgPowerWatts: Double = 0.0,
    val peakCurrentMilliAmp: Double = 0.0,
    val avgCurrentMilliAmp: Double = 0.0,
    val peakTemperatureCelsius: Float = 0f,
    val estimatedMahDelivered: Int = 0,
    val chargingProtocol: String = ""
)
