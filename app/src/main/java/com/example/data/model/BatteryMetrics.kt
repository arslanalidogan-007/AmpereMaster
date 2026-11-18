package com.example.data.model

/**
 * Repräsentiert die Ladequelle des Geräts.
 */
enum class ChargingSource {
    AC,         // Netzteil (Steckdose)
    USB,        // USB-Anschluss (PC, Zubehör)
    WIRELESS,   // Induktives / Kabelloses Laden
    DOCK,       // Dockingstation
    NONE        // Nicht verbunden (Akkubetrieb)
}

/**
 * Gesundheitszustand des Akkus laut Android BatteryManager.
 */
enum class BatteryHealthStatus {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
    UNKNOWN
}

/**
 * Kalibrierungsmodus für Dual-Cell-Akkus (2S-Zellkonfiguration vieler 65W–120W Smartphones).
 */
enum class DualCellMode {
    AUTO,
    SINGLE_CELL,
    DUAL_CELL
}

/**
 * Vollständiges Datenmodell für alle Live-Akkumetriken und -Sensordaten.
 */
data class BatteryMetrics(
    // Ladezustand in Prozent auf exakt 2 Nachkommastellen genau
    val levelPercent: Double = 0.0,

    // Status: Lädt das Gerät aktuell?
    val isCharging: Boolean = false,

    // Gibt an, ob Schnellladung (USB-PD / Super Fast Charging) aktiv ist
    val isFastCharging: Boolean = false,

    // Erkannte Ladequelle
    val chargingSource: ChargingSource = ChargingSource.NONE,

    // Momentane Stromstärke in Milliampere (mA). Positiv = Lädt, Negativ = Entlädt
    val currentMilliAmp: Double = 0.0,

    // Momentane Stromstärke in Ampere (A)
    val currentAmp: Double = 0.0,

    // Batteriespannung in Volt (V)
    val voltageVolt: Double = 0.0,

    // Verhandelte Ladespannung am USB-Eingang in Volt (z.B. 9V / 10V bei USB-PD)
    val chargerVoltageVolt: Double = 0.0,

    // Effektive Ladeleistung am Kabel/Netzteil in Watt (z.B. 25W - 28W)
    val powerWatts: Double = 0.0,

    // Reine Zell-Ladeleistung im Akku (V_batt * I)
    val batteryCellPowerWatts: Double = 0.0,

    // Akkutemperatur in Grad Celsius (°C)
    val temperatureCelsius: Float = 0f,

    // Akkutemperatur in Grad Fahrenheit (°F)
    val temperatureFahrenheit: Float = 32f,

    // Berechnete Restladezeit in Millisekunden (-1 wenn unbekannt/nicht ladend)
    val chargeTimeRemainingMs: Long = -1L,

    // Gibt an, ob der Kernel/die Hardware Stromstärkemessungen unterstützt
    val isHardwareCurrentSupported: Boolean = true,

    // Akkugesundheit
    val health: BatteryHealthStatus = BatteryHealthStatus.GOOD,

    // Geschätzte Gesamtkapazität in Milliamperestunden (mAh)
    val capacityMah: Int = 0,

    // Ladezyklen (Android 14+ / Kernel-spezifisch, sonst -1)
    val cycleCount: Int = -1,

    // Erkanntes Schnellladeprotokoll (z.B. USB-PD, PPS, Fast Charge, Qi)
    val detectedProtocol: String = "Standard",

    // Entladerate in % pro Stunde (im Akkubetrieb)
    val dischargeRatePerHour: Double = 0.0,

    // Standby-Drain Qualitätsbewertung
    val standbyDrainQuality: String = "",

    // v1.0.3: Netzteil- & Kabel-Gütebewertung
    val chargerQualityScore: Int = 0,
    val chargerQualityGrade: String = "–",
    val cableEfficiencyPercent: Int = 0,

    // v1.0.3: State of Health (SOH) & Design-Kapazität
    val designCapacityMah: Int = 4800,
    val sohPercent: Float = 100f,

    // v1.0.4: Dual-Cell Status, Screen-On vs. Standby Entladung & aktives Netzteilprofil
    val dualCellActive: Boolean = false,
    val screenOnDischargePerHour: Double = 0.0,
    val screenOffDischargePerHour: Double = 0.0,
    val screenOnDrainSharePercent: Int = 0,
    val activeChargerProfile: String = "",

    // Zeitstempel der letzten Messung
    val timestamp: Long = System.currentTimeMillis()
)
