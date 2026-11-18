package com.example.data.model

/**
 * Zustand und Ergebnis des 30-Sekunden Netzteil- & Kabel-Stresstests.
 */
data class ChargerBenchmarkState(
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val secondsRemaining: Int = 30,
    val totalSeconds: Int = 30,
    val progress: Float = 0f,
    val sampleCount: Int = 0,
    val peakCurrentMilliAmp: Double = 0.0,
    val avgCurrentMilliAmp: Double = 0.0,
    val peakPowerWatts: Double = 0.0,
    val avgPowerWatts: Double = 0.0,
    val minVoltageVolt: Double = 0.0,
    val maxVoltageVolt: Double = 0.0,
    val voltageSagMv: Int = 0,
    val stabilityPercent: Int = 100,
    val efficiencyPercent: Int = 0,
    val score: Int = 0,
    val grade: String = "–"
)
