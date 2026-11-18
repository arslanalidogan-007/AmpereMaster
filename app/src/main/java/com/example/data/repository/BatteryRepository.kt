package com.example.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.local.entity.SavedChargerEntity
import com.example.data.model.BatteryHealthStatus
import com.example.data.model.BatteryMetrics
import com.example.data.model.ChargerBenchmarkState
import com.example.data.model.ChargingSessionSummary
import com.example.data.model.ChargingSource
import com.example.data.model.DualCellMode
import com.example.localization.AppLanguage
import com.example.sensor.ExponentialFilter
import com.example.ui.theme.ThemeMode
import com.example.widget.AmpereAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Zentrales Repository für Batteriemessdaten, Glättungsfilter, Room-Persistenz und Einstellungen.
 */
class BatteryRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val batteryDao = database.batteryDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("amperemaster_prefs", Context.MODE_PRIVATE)

    // Filter zur Eliminierung von Kernel-Sensorrauschen
    private val currentSmoother = ExponentialFilter(alpha = 0.25)
    private val voltageSmoother = ExponentialFilter(alpha = 0.3)

    // Ticker für kontinuierliche 1-Sekunden Live-Aktualisierung
    private var tickerJob: Job? = null
    @Volatile private var lastBatteryIntent: Intent? = null
    @Volatile private var baseSystemLevel: Int = -1
    @Volatile private var fractionalOffset: Double = 0.0
    @Volatile private var lastTickerTimestamp: Long = System.currentTimeMillis()
    @Volatile private var lastIsCharging: Boolean? = null

    // Live-Metriken
    private val _batteryMetrics = MutableStateFlow(BatteryMetrics())
    val batteryMetrics: StateFlow<BatteryMetrics> = _batteryMetrics.asStateFlow()

    // Einstellungen
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow(loadLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _targetChargeLevel = MutableStateFlow(prefs.getInt(KEY_TARGET_LEVEL, 80))
    val targetChargeLevel: StateFlow<Int> = _targetChargeLevel.asStateFlow()

    private val _soundAlertEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ALERT, true))
    val soundAlertEnabled: StateFlow<Boolean> = _soundAlertEnabled.asStateFlow()

    private val _vibrationAlertEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION_ALERT, true))
    val vibrationAlertEnabled: StateFlow<Boolean> = _vibrationAlertEnabled.asStateFlow()

    private val _backgroundServiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_BG_SERVICE, true))
    val backgroundServiceEnabled: StateFlow<Boolean> = _backgroundServiceEnabled.asStateFlow()

    // v1.0.2: Überhitzungsschutz
    private val _tempAlertEnabled = MutableStateFlow(prefs.getBoolean(KEY_TEMP_ALERT, true))
    val tempAlertEnabled: StateFlow<Boolean> = _tempAlertEnabled.asStateFlow()

    private val _tempAlertThreshold = MutableStateFlow(prefs.getInt(KEY_TEMP_ALERT_THRESHOLD, 42))
    val tempAlertThreshold: StateFlow<Int> = _tempAlertThreshold.asStateFlow()

    // v1.0.2: Ladesitzungs-Zusammenfassung nach Abstecken
    private val _lastSessionSummary = MutableStateFlow<ChargingSessionSummary?>(null)
    val lastSessionSummary: StateFlow<ChargingSessionSummary?> = _lastSessionSummary.asStateFlow()

    // v1.0.3: 30-Sekunden Netzteil- & Kabel-Stresstest
    private val _benchmarkState = MutableStateFlow(ChargerBenchmarkState())
    val benchmarkState: StateFlow<ChargerBenchmarkState> = _benchmarkState.asStateFlow()
    private var benchmarkJob: Job? = null

    // v1.0.3: Design-Kapazität (mAh) für SOH-Berechnung
    private val _designCapacityMah = MutableStateFlow(loadOrDetectDesignCapacity())
    val designCapacityMah: StateFlow<Int> = _designCapacityMah.asStateFlow()

    // v1.0.4: Dual-Cell Kalibrierung, Widget-Detailmodus, aktives Netzteilprofil & Screen-On/Off Korrelation
    private val _dualCellMode = MutableStateFlow(loadDualCellMode())
    val dualCellMode: StateFlow<DualCellMode> = _dualCellMode.asStateFlow()

    private val _widgetDetailedMode = MutableStateFlow(prefs.getBoolean(KEY_WIDGET_DETAILED, true))
    val widgetDetailedMode: StateFlow<Boolean> = _widgetDetailedMode.asStateFlow()

    private val _activeChargerProfile = MutableStateFlow(prefs.getString(KEY_ACTIVE_CHARGER_PROFILE, "") ?: "")
    val activeChargerProfile: StateFlow<String> = _activeChargerProfile.asStateFlow()

    private var screenOnDischargeAvg: Double = 0.0
    private var screenOffDischargeAvg: Double = 0.0
    private var screenOnSampleTicks: Int = 0
    private var screenOffSampleTicks: Int = 0

    private var lastSnapshotTimestamp: Long = prefs.getLong(KEY_LAST_SOH_SNAPSHOT, 0L)

    // Laufende Ladesitzungs-Statistiken
    private var currentSessionId: Long? = null
    private var lastRecordedSampleTime: Long = 0L
    private var sessionStartTime: Long = 0L
    private var sessionStartLevel: Double = 0.0
    private var sessionMaxPowerWatts: Double = 0.0
    private var sessionMaxCurrentMa: Double = 0.0
    private var sessionSumCurrentMa: Double = 0.0
    private var sessionCurrentCount: Int = 0
    private var sessionMaxTemp: Float = 0f
    private var sessionProtocol: String = ""

    // Ladekurven-, SOH- & Netzteil-Streams aus Room
    val latestSamples: Flow<List<BatterySampleEntity>> = batteryDao.getLatestSamples(500)
    val allSessions: Flow<List<ChargingSessionEntity>> = batteryDao.getAllSessions()
    val healthSnapshots: Flow<List<BatteryHealthSnapshotEntity>> = batteryDao.getAllHealthSnapshots()
    val savedChargers: Flow<List<SavedChargerEntity>> = batteryDao.getAllSavedChargers()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                processBatteryChangedIntent(intent)
            }
        }
    }

    init {
        // 1. Sticky Battery-Intent sofort und sicher abfragen (null Receiver benötigt keinerlei Flags auf allen Android-Versionen)
        try {
            val stickyIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (stickyIntent != null) {
                lastBatteryIntent = stickyIntent
                processBatteryChangedIntent(stickyIntent)
            }
        } catch (e: Exception) {
            Log.e("BatteryRepo", "Fehler beim Lesen des initialen Akku-Intents", e)
        }

        // 2. Kontinuierlichen BroadcastReceiver mit RECEIVER_EXPORTED auf Android 13+ registrieren
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("BatteryRepo", "Fehler beim Registrieren des dynamic receivers", e)
        }

        // 3. 1-Sekunden Live-Ticker starten für kontinuierliche 2-Dezimalstellen-Aktualisierung
        startPeriodicTicker()
    }

    /**
     * Startet eine periodische Coroutine, die jede Sekunde Ladedaten, Stromstärke und
     * hochpräzise Nachkommastellen (Coulomb-Counting / Fuel-Gauge) aktualisiert.
     */
    private fun startPeriodicTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(1000L)
                try {
                    // Stets den aktuellen Sticky-Intent vom System abfragen, um sofort auf Kabeltrennung zu reagieren
                    val stickyIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val intent = stickyIntent ?: lastBatteryIntent
                    if (intent != null) {
                        lastBatteryIntent = intent
                        val now = System.currentTimeMillis()
                        val deltaSec = Math.min(2.0, Math.max(0.1, (now - lastTickerTimestamp) / 1000.0))
                        lastTickerTimestamp = now
                        updateMetrics(intent, deltaSec)
                    }
                } catch (e: Exception) {
                    Log.e("BatteryRepo", "Fehler im Periodic Ticker", e)
                }
            }
        }
    }

    /**
     * Verarbeitet den Intent.ACTION_BATTERY_CHANGED Intent.
     */
    fun processBatteryChangedIntent(intent: Intent) {
        lastBatteryIntent = intent
        val now = System.currentTimeMillis()
        val deltaSec = Math.min(2.0, Math.max(0.0, (now - lastTickerTimestamp) / 1000.0))
        lastTickerTimestamp = now
        updateMetrics(intent, deltaSec)
    }

    /**
     * Liest Sensordaten aus und aktualisiert die Live-Metriken.
     * @param intent Der aktuelle Akku-Intent
     * @param deltaSec Vergangene Zeit in Sekunden seit dem letzten Durchlauf für Coulomb-Counting
     */
    private fun updateMetrics(intent: Intent, deltaSec: Double) {
        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val temperatureRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS
            else -> if (isCharging) ChargingSource.AC else ChargingSource.NONE
        }

        // Bei Umschalten zwischen Laden und Entladen: Glättungsfilter sofort zurücksetzen,
        // damit kein positiver Ladestrom nach dem Abstecken nachschwingt!
        if (lastIsCharging != null && lastIsCharging != isCharging) {
            currentSmoother.reset()
            voltageSmoother.reset()
        }
        lastIsCharging = isCharging

        // Exakten Betriebssystem-Prozentsatz berechnen (entspricht exakt der Android-Statusleiste)
        val systemPercent = if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100) / scale
        } else {
            rawLevel
        }

        // Basis-Prozentsatz bei System-Änderung synchronisieren
        if (systemPercent >= 0) {
            if (baseSystemLevel == -1) {
                baseSystemLevel = systemPercent
                val initialFraction = readHardwareFuelGaugeFraction(systemPercent)
                fractionalOffset = initialFraction ?: 0.0
            } else if (systemPercent != baseSystemLevel) {
                val previousLevel = baseSystemLevel
                baseSystemLevel = systemPercent
                if (systemPercent > previousLevel) {
                    // Aufgeladen: Neue Stufe beginnt bei .00
                    fractionalOffset = 0.0
                } else {
                    // Entladen: Fällt von oben in die neue Stufe ein (z.B. 63.99)
                    fractionalOffset = 0.99
                }
            }
        }

        // Spannung in Volt (mV -> V)
        val rawVoltageVolt = if (voltageMv > 0) voltageMv / 1000.0 else 3.85
        val smoothedVoltage = voltageSmoother.filter(rawVoltageVolt)

        // Temperatur in °C (Zehntelgrad -> °C)
        val temperatureCelsius = temperatureRaw / 10.0f
        val temperatureFahrenheit = (temperatureCelsius * 9f / 5f) + 32f

        // 1. USB-PD Ladespannung und Leistung ermitteln
        var maxChargingMicroVolt = 0
        var maxChargingMicroAmp = 0
        maxChargingMicroVolt = intent.getIntExtra("max_charging_voltage", 0)
        maxChargingMicroAmp = intent.getIntExtra("max_charging_current", 0)

        // Sysfs-Werte aus dem Kernel prüfen (sofern zugänglich)
        val sysfsCurrent = readSysfsLong(
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/batt_current_ua_now",
            "/sys/class/power_supply/battery/sec_reroute_current",
            "/sys/class/power_supply/sec-charger/current_now",
            "/sys/class/power_supply/usb/current_now"
        )
        val sysfsPowerMicroWatts = readSysfsLong(
            "/sys/class/power_supply/battery/power_now",
            "/sys/class/power_supply/usb/power_now",
            "/sys/class/power_supply/sec-charger/power_now"
        )
        val sysfsUsbMicroVolt = readSysfsLong(
            "/sys/class/power_supply/usb/voltage_now",
            "/sys/class/power_supply/usb/input_voltage",
            "/sys/class/power_supply/sec-charger/voltage_now"
        )

        // Momentane Stromstärke über BatteryManager abfragen
        var isHardwareSupported = true
        var rawCurrentMa = 0.0

        if (sysfsCurrent != null && sysfsCurrent != 0L) {
            rawCurrentMa = smartConvertToMilliAmps(sysfsCurrent, isCharging)
        } else if (batteryManager != null) {
            val currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (currentNow != Long.MIN_VALUE && currentNow != 0L) {
                rawCurrentMa = smartConvertToMilliAmps(currentNow, isCharging)
            } else {
                val currentAvg = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                if (currentAvg != Long.MIN_VALUE && currentAvg != 0L) {
                    rawCurrentMa = smartConvertToMilliAmps(currentAvg, isCharging)
                } else {
                    isHardwareSupported = false
                    rawCurrentMa = when {
                        isCharging && source == ChargingSource.AC -> 3000.0
                        isCharging && source == ChargingSource.WIRELESS -> 1200.0
                        isCharging && source == ChargingSource.USB -> 900.0
                        else -> -450.0
                    }
                }
            }
        } else {
            isHardwareSupported = false
            rawCurrentMa = if (isCharging) 2800.0 else -400.0
        }

        // Normalisierung: Laden = positiver Stromfluss, Entladen = negativer Stromfluss
        val rawCellFactor = when (_dualCellMode.value) {
            DualCellMode.DUAL_CELL -> if (rawVoltageVolt < 5.5) 2.0 else 1.0
            DualCellMode.SINGLE_CELL -> 1.0
            DualCellMode.AUTO -> 1.0
        }
        val dualCellActive = _dualCellMode.value == DualCellMode.DUAL_CELL || rawVoltageVolt >= 6.5

        val normalizedCurrentMa = if (isCharging) {
            Math.abs(rawCurrentMa)
        } else {
            -Math.abs(rawCurrentMa)
        }

        val smoothedCurrentMa = currentSmoother.filter(normalizedCurrentMa)
        val currentAmp = Math.abs(smoothedCurrentMa) / 1000.0

        // Zell-Ladeleistung im Akku (inkl. optionalem 2S-Dual-Cell-Korrekturfaktor)
        val batteryCellPowerWatts = smoothedVoltage * currentAmp * rawCellFactor

        // Ladespannung am USB-Port ermitteln (z. B. 9V / 9.5V bei USB-PD Fast Charging wie auf dem Nutzer-Kabel angezeigt!)
        val chargerVoltageVolt = when {
            sysfsUsbMicroVolt != null && sysfsUsbMicroVolt > 1_000_000L -> sysfsUsbMicroVolt / 1_000_000.0
            maxChargingMicroVolt > 1_000_000 -> maxChargingMicroVolt / 1_000_000.0
            isCharging && source == ChargingSource.AC && smoothedCurrentMa >= 1800.0 -> 9.3 // Samsung 25W/45W Super Fast Charging PPS typisch ~9.3V
            isCharging && source == ChargingSource.AC -> 5.2
            else -> smoothedVoltage
        }

        val isFastCharging = isCharging && (chargerVoltageVolt > 5.5 || smoothedCurrentMa >= 2000.0)

        // Gesamte Ladeleistung (wie vom digitalen Ladekabel gemessen)
        val effectivePowerWatts = when {
            sysfsPowerMicroWatts != null && sysfsPowerMicroWatts > 100_000L -> sysfsPowerMicroWatts / 1_000_000.0
            isCharging && isFastCharging -> {
                // USB-PD Leistung: USB-Spannung * USB-Stromstärke
                val pdWatts = chargerVoltageVolt * currentAmp
                Math.max(pdWatts, batteryCellPowerWatts / 0.90)
            }
            isCharging -> batteryCellPowerWatts / 0.88
            else -> batteryCellPowerWatts
        }

        // Akkukapazität und Zyklen
        val capacityMah = estimateBatteryCapacity()
        val cycleCount = readCycleCount()

        // Hochpräziser Ladezustand auf 2 Nachkommastellen berechnen (Coulomb-Counting & Fuel-Gauge)
        val effectiveBaseLevel = if (baseSystemLevel >= 0) baseSystemLevel else (if (systemPercent >= 0) systemPercent else 50)
        val levelPercent = computePreciseLevelPercent(
            baseLevel = effectiveBaseLevel,
            isCharging = isCharging,
            currentMa = smoothedCurrentMa,
            deltaTimeSec = deltaSec,
            capacityMah = capacityMah
        )

        // Restladezeit berechnen
        val chargeTimeRemainingMs = calculateRemainingTime(isCharging, levelPercent, smoothedCurrentMa)

        // Akkugesundheit zuordnen
        val health = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthStatus.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthStatus.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthStatus.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthStatus.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthStatus.COLD
            else -> BatteryHealthStatus.GOOD
        }

        // Erkanntes Schnellladeprotokoll ermitteln
        val detectedProtocol = when {
            !isCharging -> "Akkubetrieb"
            source == ChargingSource.WIRELESS -> "Qi Wireless"
            chargerVoltageVolt >= 8.5 || effectivePowerWatts >= 22.0 -> "USB-PD / PPS Super Fast"
            chargerVoltageVolt in 5.5..8.5 || effectivePowerWatts in 15.0..22.0 -> "Quick Charge"
            effectivePowerWatts in 10.0..15.0 || Math.abs(smoothedCurrentMa) >= 2000.0 -> "USB Fast (10-15W)"
            effectivePowerWatts in 4.5..10.0 -> "Standard USB (5-10W)"
            else -> "Standard USB (<5W)"
        }

        // Entladerate in % / Stunde (nur im Akkubetrieb)
        val dischargeRatePerHour = if (!isCharging && capacityMah > 0) {
            val rate = (Math.abs(smoothedCurrentMa) / capacityMah.toDouble()) * 100.0
            Math.round(rate * 10.0) / 10.0
        } else {
            0.0
        }

        val standbyDrainQuality = if (!isCharging) {
            when {
                dischargeRatePerHour < 3.0 -> "Optimal"
                dischargeRatePerHour in 3.0..7.0 -> "Normal"
                else -> "Erhöht"
            }
        } else {
            ""
        }

        // v1.0.4: Screen-On vs. Screen-Off Entlade-Korrelation
        val isScreenOn = try {
            powerManager?.isInteractive ?: true
        } catch (_: Exception) {
            true
        }
        if (!isCharging && dischargeRatePerHour > 0.0) {
            if (isScreenOn) {
                screenOnSampleTicks++
                screenOnDischargeAvg = if (screenOnSampleTicks == 1) dischargeRatePerHour else (screenOnDischargeAvg * 0.8 + dischargeRatePerHour * 0.2)
            } else {
                screenOffSampleTicks++
                screenOffDischargeAvg = if (screenOffSampleTicks == 1) dischargeRatePerHour else (screenOffDischargeAvg * 0.8 + dischargeRatePerHour * 0.2)
            }
        }
        val totalTicks = screenOnSampleTicks + screenOffSampleTicks
        val screenOnShare = if (totalTicks > 0) ((screenOnSampleTicks * 100) / totalTicks).coerceIn(0, 100) else 0

        // v1.0.3: Netzteil- & Kabel-Gütebewertung
        val (chargerScore, chargerGrade, cableEfficiency) = evaluateChargerQuality(
            isCharging = isCharging,
            source = source,
            currentMa = smoothedCurrentMa,
            voltageVolt = smoothedVoltage,
            chargerVoltageVolt = chargerVoltageVolt,
            powerWatts = effectivePowerWatts,
            cellPowerWatts = batteryCellPowerWatts
        )

        // v1.0.3: State of Health (SOH)
        val designMah = _designCapacityMah.value.coerceIn(2000, 12000)
        val sohPercent = ((capacityMah.toFloat() / designMah.toFloat()) * 100f).coerceIn(30f, 100f)
        val roundedSoh = Math.round(sohPercent * 10f) / 10f

        val metrics = BatteryMetrics(
            levelPercent = levelPercent,
            isCharging = isCharging,
            isFastCharging = isFastCharging,
            chargingSource = source,
            currentMilliAmp = Math.round(smoothedCurrentMa * 10.0) / 10.0,
            currentAmp = Math.round(currentAmp * 100.0) / 100.0,
            voltageVolt = Math.round(smoothedVoltage * 100.0) / 100.0,
            chargerVoltageVolt = Math.round(chargerVoltageVolt * 10.0) / 10.0,
            powerWatts = Math.round(effectivePowerWatts * 10.0) / 10.0,
            batteryCellPowerWatts = Math.round(batteryCellPowerWatts * 10.0) / 10.0,
            temperatureCelsius = temperatureCelsius,
            temperatureFahrenheit = temperatureFahrenheit,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            isHardwareCurrentSupported = isHardwareSupported,
            health = health,
            capacityMah = capacityMah,
            cycleCount = cycleCount,
            detectedProtocol = detectedProtocol,
            dischargeRatePerHour = dischargeRatePerHour,
            standbyDrainQuality = standbyDrainQuality,
            chargerQualityScore = chargerScore,
            chargerQualityGrade = chargerGrade,
            cableEfficiencyPercent = cableEfficiency,
            designCapacityMah = designMah,
            sohPercent = roundedSoh,
            dualCellActive = dualCellActive,
            screenOnDischargePerHour = Math.round(screenOnDischargeAvg * 10.0) / 10.0,
            screenOffDischargePerHour = Math.round(screenOffDischargeAvg * 10.0) / 10.0,
            screenOnDrainSharePercent = screenOnShare,
            activeChargerProfile = _activeChargerProfile.value,
            timestamp = System.currentTimeMillis()
        )

        _batteryMetrics.value = metrics

        // Home Screen AppWidget aktualisieren
        AmpereAppWidgetProvider.updateAllWidgets(context, metrics)

        // Datenpunkt für Kurve in Room speichern & Ladesitzung auswerten
        handleSessionAndRecordSample(metrics)

        // Periodischen SOH-Snapshot automatisch erfassen (alle 6 Stunden oder initial)
        maybeRecordPeriodicHealthSnapshot(metrics)
    }

    /**
     * Berechnet den hochpräzisen Ladezustand mit 2 Nachkommastellen.
     * Der Integer-Wert entspricht IMMER exakt der Betriebssystem-Statusleiste (baseLevel).
     *
     * - Beim Laden (Kabel eingesteckt): Die Dezimalstellen steigen physikalisch per Coulomb-Counting
     *   im Bereich [0.00, 0.99] an, bis das Betriebssystem auf die nächste Prozentzahl umschaltet.
     * - Beim Entladen (Kabel abgezogen): Die Dezimalstellen laufen NIEMALS hoch. Sie sinken minimal
     *   mit dem realen Verbrauch oder bleiben stabil innerhalb der aktuellen Prozentstufe [0.00, 0.99].
     */
    private fun computePreciseLevelPercent(
        baseLevel: Int,
        isCharging: Boolean,
        currentMa: Double,
        deltaTimeSec: Double,
        capacityMah: Int
    ): Double {
        if (baseLevel <= 0) return 0.0
        if (baseLevel >= 100) return 100.0

        val effCapacity = capacityMah.coerceIn(2000, 10000)
        if (deltaTimeSec > 0.0) {
            // Delta Q in mAh = currentMa * (deltaTimeSec / 3600.0)
            // Delta% = (Delta Q / effCapacity) * 100.0
            val deltaPercent = (currentMa * deltaTimeSec) / (3600.0 * effCapacity) * 100.0

            if (isCharging) {
                // Beim Laden: Nur positiven Zuwachs erlauben, begrenzt auf [0.0, 0.99]
                val positiveDelta = Math.max(0.0, deltaPercent)
                fractionalOffset = (fractionalOffset + positiveDelta).coerceIn(0.0, 0.99)
            } else {
                // Beim Entladen: NIEMALS ansteigen! Nur negative Veränderung oder 0 zulassen.
                val negativeDelta = Math.min(0.0, deltaPercent)
                fractionalOffset = (fractionalOffset + negativeDelta).coerceIn(0.0, 0.99)
            }
        }

        // Ergebnis: Exakter OS-Integer + berechnete Nachkommastelle, z.B. 64.48 %
        val precise = baseLevel.toDouble() + fractionalOffset
        return Math.round(precise * 100.0) / 100.0
    }

    /**
     * Versucht die initiale Hardware-Nachkommastelle direkt aus dem Linux-Kernel-Fuel-Gauge zu lesen.
     * Gültig nur, wenn der ganzzahlige Anteil exakt mit dem Android-OS übereinstimmt, um jegliche
     * Diskrepanzen zwischen App und Systemstatusleiste auszuschließen.
     */
    private fun readHardwareFuelGaugeFraction(baseLevel: Int): Double? {
        val fgRaw = readSysfsLong(
            "/sys/class/power_supply/battery/fg_capacity",
            "/sys/class/power_supply/battery/batt_capacity_pct",
            "/sys/class/power_supply/battery/capacity_in_percent",
            "/sys/class/power_supply/sec-charger/fg_capacity"
        )
        if (fgRaw != null && fgRaw > 0L) {
            val candidate = when {
                fgRaw > 1000L -> fgRaw / 100.0 // z.B. 6425 -> 64.25%
                fgRaw > 100L -> fgRaw / 10.0   // z.B. 642 -> 64.2%
                else -> fgRaw.toDouble()
            }
            if (baseLevel > 0 && Math.floor(candidate).toInt() == baseLevel) {
                return (candidate - baseLevel).coerceIn(0.0, 0.99)
            }
        }
        return null
    }

    /**
     * Konvertiert Rohwerte aus Kernel / BatteryManager intelligent in Milliampere (mA).
     * Erkennt automatisch:
     * - Standard-AOSP: Mikroampere (µA) -> z.B. 3.000.000 µA = 3000 mA
     * - Samsung One UI / Qualcomm: Wert ist bereits in mA -> z.B. 3000 = 3000 mA
     * - Ampere-Rohwerte: z.B. 3 = 3000 mA
     */
    private fun smartConvertToMilliAmps(raw: Long, isCharging: Boolean): Double {
        val absRaw = Math.abs(raw).toDouble()
        if (absRaw == 0.0) return 0.0

        val ma = when {
            // Standard Android Spezifikation in Mikroampere (µA)
            absRaw >= 15_000.0 -> absRaw / 1000.0

            // Samsung Galaxy One UI liefert BATTERY_PROPERTY_CURRENT_NOW direkt in mA
            // (z.B. 3000 mA statt 3.000.000 µA, was sonst fälschlicherweise zu 3 mA führen würde!)
            absRaw in 50.0..14_999.0 -> absRaw

            // Sehr kleine Rohwerte (1 bis 49): Treiber liefert direkt in Ampere (A)
            absRaw in 1.0..49.0 -> absRaw * 1000.0

            else -> absRaw
        }

        return if (isCharging) Math.abs(ma) else -Math.abs(ma)
    }

    /**
     * Versucht direkte Linux-Kernel-Werte aus sysfs power_supply zu lesen (sofern zugänglich).
     */
    private fun readSysfsLong(vararg paths: String): Long? {
        for (path in paths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    val content = file.readText().trim()
                    val value = content.toLongOrNull()
                    if (value != null && value != 0L && value != Long.MIN_VALUE) {
                        return value
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    /**
     * Schätzt oder liest die verbleibende Ladezeit bis zum gewünschten Zielwert.
     */
    private fun calculateRemainingTime(isCharging: Boolean, currentLevel: Double, currentMa: Double): Long {
        if (!isCharging) return -1L

        val target = _targetChargeLevel.value.toDouble()
        if (currentLevel >= target) return 0L

        // Android 9+ native API prüfen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && batteryManager != null) {
            try {
                val nativeRemaining = batteryManager.computeChargeTimeRemaining()
                if (nativeRemaining > 0) {
                    val progressRatio = (target - currentLevel) / (100.0 - currentLevel)
                    return (nativeRemaining * Math.max(0.1, progressRatio)).toLong()
                }
            } catch (e: Exception) {
                Log.w("BatteryRepo", "computeChargeTimeRemaining fehlgeschlagen", e)
            }
        }

        // Mathematische Näherung basierend auf Kapazität und Stromstärke
        val capacity = estimateBatteryCapacity().toDouble()
        val remainingCapacity = (target - currentLevel) / 100.0 * capacity
        val effectiveMa = Math.max(250.0, Math.abs(currentMa))

        // Berücksichtigung des Ladekurvenabfalls bei hohen Prozentwerten (Li-Ion CV-Phase)
        val cvFactor = if (currentLevel > 80.0) 1.35 else 1.1
        val hours = (remainingCapacity / effectiveMa) * cvFactor
        return (hours * 3600.0 * 1000.0).toLong()
    }

    /**
     * Schätzt die Nennkapazität der Batterie in mAh.
     */
    private fun estimateBatteryCapacity(): Int {
        if (batteryManager != null) {
            try {
                val chargeCounterMicroAh = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                if (chargeCounterMicroAh > 0) {
                    val currentMah = chargeCounterMicroAh / 1000
                    val level = if (baseSystemLevel > 5) baseSystemLevel else 50
                    val computed = (currentMah / (level / 100.0)).toInt()
                    if (computed in 2000..10000) {
                        return computed
                    }
                }
            } catch (e: Exception) {
                // Bei Ausnahmen Standardwert verwenden
            }
        }
        // Standard-Schätzwert für moderne Smartphones (4500-5000 mAh)
        return 4800
    }

    /**
     * Liest die Ladezyklen aus (sofern von Android 14+ oder Kernel unterstützt).
     */
    private fun readCycleCount(): Int {
        if (Build.VERSION.SDK_INT >= 34 && batteryManager != null) {
            try {
                // BATTERY_PROPERTY_CYCLE_COUNT hat den Integer-Wert 7 ab Android 14 (API 34)
                val cycleProperty = batteryManager.getIntProperty(7)
                if (cycleProperty > 0) return cycleProperty
            } catch (ignored: Exception) {
            }
        }
        return -1
    }

    /**
     * Verwaltet Ladesitzungen und speichert zyklisch Ladekurven-Messpunkte in Room.
     */
    private fun handleSessionAndRecordSample(metrics: BatteryMetrics) {
        val now = System.currentTimeMillis()

        // Ladesitzung starten, aktualisieren oder beenden
        if (metrics.isCharging && currentSessionId == null) {
            sessionStartTime = now
            sessionStartLevel = metrics.levelPercent
            sessionMaxPowerWatts = metrics.powerWatts
            sessionMaxCurrentMa = metrics.currentMilliAmp
            sessionSumCurrentMa = metrics.currentMilliAmp
            sessionCurrentCount = 1
            sessionMaxTemp = metrics.temperatureCelsius
            sessionProtocol = metrics.detectedProtocol

            scope.launch {
                val newSession = ChargingSessionEntity(
                    startTime = now,
                    startLevelPercent = metrics.levelPercent,
                    chargingSource = metrics.chargingSource.name,
                    chargerProfileName = _activeChargerProfile.value
                )
                currentSessionId = batteryDao.insertSession(newSession)
            }
        } else if (metrics.isCharging && currentSessionId != null) {
            sessionMaxPowerWatts = Math.max(sessionMaxPowerWatts, metrics.powerWatts)
            sessionMaxCurrentMa = Math.max(sessionMaxCurrentMa, metrics.currentMilliAmp)
            sessionMaxTemp = Math.max(sessionMaxTemp, metrics.temperatureCelsius)
            sessionSumCurrentMa += metrics.currentMilliAmp
            sessionCurrentCount++
        } else if (!metrics.isCharging && currentSessionId != null) {
            val finishedId = currentSessionId
            currentSessionId = null

            val durationMin = Math.max(1L, (now - sessionStartTime) / 60000L)
            val deltaPct = Math.max(0.0, metrics.levelPercent - sessionStartLevel)
            val avgCur = if (sessionCurrentCount > 0) sessionSumCurrentMa / sessionCurrentCount else sessionMaxCurrentMa
            val avgPwr = Math.round((metrics.voltageVolt * (avgCur / 1000.0) * 10.0)) / 10.0
            val estMah = Math.max(0, (avgCur * (durationMin / 60.0)).toInt())

            val summary = ChargingSessionSummary(
                sessionId = finishedId ?: 0L,
                startTime = sessionStartTime,
                endTime = now,
                durationMinutes = durationMin,
                startLevelPercent = Math.round(sessionStartLevel * 100.0) / 100.0,
                endLevelPercent = Math.round(metrics.levelPercent * 100.0) / 100.0,
                deltaPercent = Math.round(deltaPct * 100.0) / 100.0,
                maxPowerWatts = Math.round(sessionMaxPowerWatts * 10.0) / 10.0,
                avgPowerWatts = Math.max(0.0, avgPwr),
                peakCurrentMilliAmp = Math.round(sessionMaxCurrentMa * 10.0) / 10.0,
                avgCurrentMilliAmp = Math.round(avgCur * 10.0) / 10.0,
                peakTemperatureCelsius = sessionMaxTemp,
                estimatedMahDelivered = estMah,
                chargingProtocol = sessionProtocol.ifBlank { metrics.detectedProtocol }
            )
            _lastSessionSummary.value = summary

            if (finishedId != null) {
                scope.launch {
                    val session = batteryDao.getSessionById(finishedId)
                    if (session != null) {
                        batteryDao.updateSession(
                            session.copy(
                                endTime = now,
                                endLevelPercent = metrics.levelPercent,
                                maxPowerWatts = sessionMaxPowerWatts,
                                maxCurrentMilliAmp = sessionMaxCurrentMa,
                                avgCurrentMilliAmp = avgCur
                            )
                        )
                    }
                }
            }
        }

        // Maximal alle 8 Sekunden einen Punkt aufzeichnen, um die Datenbank performant zu halten
        if (now - lastRecordedSampleTime >= 8000L) {
            lastRecordedSampleTime = now
            scope.launch {
                val sample = BatterySampleEntity(
                    sessionId = currentSessionId ?: 0L,
                    timestamp = now,
                    levelPercent = metrics.levelPercent,
                    currentMilliAmp = metrics.currentMilliAmp,
                    voltageVolt = metrics.voltageVolt,
                    powerWatts = metrics.powerWatts,
                    temperatureCelsius = metrics.temperatureCelsius,
                    isCharging = metrics.isCharging
                )
                batteryDao.insertSample(sample)
            }
        }
    }

    // --- Einstellungen ändern & speichern ---

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        AmpereAppWidgetProvider.updateAllWidgets(context, _batteryMetrics.value)
    }

    fun setDualCellMode(mode: DualCellMode) {
        _dualCellMode.value = mode
        prefs.edit().putString(KEY_DUAL_CELL_MODE, mode.name).apply()
        lastBatteryIntent?.let { updateMetrics(it, 0.0) }
    }

    fun setWidgetDetailedMode(detailed: Boolean) {
        _widgetDetailedMode.value = detailed
        prefs.edit().putBoolean(KEY_WIDGET_DETAILED, detailed).apply()
        AmpereAppWidgetProvider.updateAllWidgets(context, _batteryMetrics.value)
    }

    fun saveChargerProfile(name: String, ratedWatts: Int) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        val bm = _benchmarkState.value
        val m = _batteryMetrics.value
        val hasBenchmark = bm.isCompleted || bm.sampleCount > 0

        val entity = SavedChargerEntity(
            name = cleanName,
            ratedWatts = ratedWatts.coerceIn(5, 240),
            grade = if (hasBenchmark && bm.grade != "–") bm.grade else m.chargerQualityGrade,
            score = if (hasBenchmark && bm.score > 0) bm.score else m.chargerQualityScore,
            peakCurrentMilliAmp = if (hasBenchmark) bm.peakCurrentMilliAmp else Math.abs(m.currentMilliAmp),
            peakPowerWatts = if (hasBenchmark) bm.peakPowerWatts else m.powerWatts,
            cableEfficiencyPercent = if (hasBenchmark && bm.efficiencyPercent > 0) bm.efficiencyPercent else m.cableEfficiencyPercent,
            voltageSagMv = if (hasBenchmark) bm.voltageSagMv else 0,
            lastTestedAt = System.currentTimeMillis()
        )

        scope.launch {
            batteryDao.insertSavedCharger(entity)
        }
        assignActiveChargerProfile(cleanName)
    }

    fun assignActiveChargerProfile(name: String) {
        val cleanName = name.trim()
        _activeChargerProfile.value = cleanName
        prefs.edit().putString(KEY_ACTIVE_CHARGER_PROFILE, cleanName).apply()
        _batteryMetrics.value = _batteryMetrics.value.copy(activeChargerProfile = cleanName)

        val sid = currentSessionId
        if (sid != null) {
            scope.launch {
                val session = batteryDao.getSessionById(sid)
                if (session != null) {
                    batteryDao.updateSession(session.copy(chargerProfileName = cleanName))
                }
            }
        }
    }

    fun deleteSavedCharger(chargerId: Long) {
        scope.launch {
            batteryDao.deleteSavedChargerById(chargerId)
        }
    }

    fun setTargetChargeLevel(level: Int) {
        val clamped = level.coerceIn(50, 100)
        _targetChargeLevel.value = clamped
        prefs.edit().putInt(KEY_TARGET_LEVEL, clamped).apply()
    }

    fun setSoundAlertEnabled(enabled: Boolean) {
        _soundAlertEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND_ALERT, enabled).apply()
    }

    fun setVibrationAlertEnabled(enabled: Boolean) {
        _vibrationAlertEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VIBRATION_ALERT, enabled).apply()
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        _backgroundServiceEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BG_SERVICE, enabled).apply()
    }

    fun setTempAlertEnabled(enabled: Boolean) {
        _tempAlertEnabled.value = enabled
        prefs.edit().putBoolean(KEY_TEMP_ALERT, enabled).apply()
    }

    fun setTempAlertThreshold(threshold: Int) {
        val clamped = threshold.coerceIn(38, 55)
        _tempAlertThreshold.value = clamped
        prefs.edit().putInt(KEY_TEMP_ALERT_THRESHOLD, clamped).apply()
    }

    fun dismissSessionSummary() {
        _lastSessionSummary.value = null
    }

    // --- v1.0.3: Design-Kapazität, SOH & Sitzungs-Vergleich ---

    fun setDesignCapacityMah(capacityMah: Int) {
        val clamped = capacityMah.coerceIn(2000, 12000)
        _designCapacityMah.value = clamped
        prefs.edit().putInt(KEY_DESIGN_CAPACITY_MAH, clamped).apply()
        recordHealthSnapshotNow()
    }

    fun recordHealthSnapshotNow() {
        val m = _batteryMetrics.value
        val design = _designCapacityMah.value.coerceIn(2000, 12000)
        val est = if (m.capacityMah > 0) m.capacityMah else estimateBatteryCapacity()
        val soh = Math.round(((est.toFloat() / design.toFloat()) * 100f).coerceIn(30f, 100f) * 10f) / 10f
        val now = System.currentTimeMillis()
        lastSnapshotTimestamp = now
        prefs.edit().putLong(KEY_LAST_SOH_SNAPSHOT, now).apply()
        scope.launch {
            batteryDao.insertHealthSnapshot(
                BatteryHealthSnapshotEntity(
                    timestamp = now,
                    estimatedCapacityMah = est,
                    designCapacityMah = design,
                    sohPercent = soh,
                    cycleCount = m.cycleCount,
                    temperatureCelsius = m.temperatureCelsius
                )
            )
        }
    }

    private fun maybeRecordPeriodicHealthSnapshot(metrics: BatteryMetrics) {
        val now = System.currentTimeMillis()
        val sixHoursMs = 6L * 60L * 60L * 1000L
        if (lastSnapshotTimestamp == 0L || (now - lastSnapshotTimestamp) >= sixHoursMs) {
            lastSnapshotTimestamp = now
            prefs.edit().putLong(KEY_LAST_SOH_SNAPSHOT, now).apply()
            scope.launch {
                batteryDao.insertHealthSnapshot(
                    BatteryHealthSnapshotEntity(
                        timestamp = now,
                        estimatedCapacityMah = metrics.capacityMah,
                        designCapacityMah = metrics.designCapacityMah,
                        sohPercent = metrics.sohPercent,
                        cycleCount = metrics.cycleCount,
                        temperatureCelsius = metrics.temperatureCelsius
                    )
                )
            }
        }
    }

    suspend fun getSamplesForSession(sessionId: Long): List<BatterySampleEntity> {
        return batteryDao.getSamplesForSessionOnce(sessionId)
    }

    // --- v1.0.3: 30-Sekunden Netzteil- & Kabel-Stresstest ---

    fun startChargerBenchmark() {
        val currentMetrics = _batteryMetrics.value
        if (!currentMetrics.isCharging) return
        if (_benchmarkState.value.isRunning) return

        benchmarkJob?.cancel()
        benchmarkJob = scope.launch {
            val totalSeconds = 30
            var peakMa = 0.0
            var sumMa = 0.0
            var peakWatts = 0.0
            var sumWatts = 0.0
            var minV = Double.MAX_VALUE
            var maxV = 0.0
            var count = 0

            _benchmarkState.value = ChargerBenchmarkState(
                isRunning = true,
                isCompleted = false,
                secondsRemaining = totalSeconds,
                totalSeconds = totalSeconds,
                progress = 0f
            )

            for (sec in 1..totalSeconds) {
                delay(1000L)
                val m = _batteryMetrics.value
                if (!m.isCharging) {
                    _benchmarkState.value = ChargerBenchmarkState()
                    return@launch
                }

                count++
                val curMa = Math.abs(m.currentMilliAmp)
                val pwr = m.powerWatts
                val volt = m.voltageVolt

                if (curMa > peakMa) peakMa = curMa
                if (pwr > peakWatts) peakWatts = pwr
                if (volt > 0.0 && volt < minV) minV = volt
                if (volt > maxV) maxV = volt
                sumMa += curMa
                sumWatts += pwr

                val avgMa = sumMa / count
                val avgPwr = sumWatts / count
                val sagMv = if (maxV >= minV && minV < Double.MAX_VALUE) ((maxV - minV) * 1000.0).toInt() else 0
                val stability = (100 - (sagMv / 8)).coerceIn(55, 100)
                val eff = if (m.cableEfficiencyPercent > 0) m.cableEfficiencyPercent else 90
                val baseScore = m.chargerQualityScore
                val finalScore = ((baseScore * 0.7) + (stability * 0.3)).toInt().coerceIn(20, 100)
                val grade = scoreToGrade(finalScore)

                _benchmarkState.value = ChargerBenchmarkState(
                    isRunning = sec < totalSeconds,
                    isCompleted = sec == totalSeconds,
                    secondsRemaining = totalSeconds - sec,
                    totalSeconds = totalSeconds,
                    progress = sec.toFloat() / totalSeconds.toFloat(),
                    sampleCount = count,
                    peakCurrentMilliAmp = Math.round(peakMa * 10.0) / 10.0,
                    avgCurrentMilliAmp = Math.round(avgMa * 10.0) / 10.0,
                    peakPowerWatts = Math.round(peakWatts * 10.0) / 10.0,
                    avgPowerWatts = Math.round(avgPwr * 10.0) / 10.0,
                    minVoltageVolt = if (minV < Double.MAX_VALUE) minV else 0.0,
                    maxVoltageVolt = maxV,
                    voltageSagMv = sagMv,
                    stabilityPercent = stability,
                    efficiencyPercent = eff,
                    score = finalScore,
                    grade = grade
                )
            }
        }
    }

    fun stopChargerBenchmark() {
        benchmarkJob?.cancel()
        _benchmarkState.value = ChargerBenchmarkState()
    }

    fun clearAllHistory() {
        scope.launch {
            batteryDao.clearAllSamples()
            batteryDao.clearAllSessions()
        }
    }

    private fun loadOrDetectDesignCapacity(): Int {
        val saved = prefs.getInt(KEY_DESIGN_CAPACITY_MAH, -1)
        if (saved in 2000..12000) return saved

        // Versuche Hersteller-Nennkapazität via PowerProfile Reflection zu ermitteln
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val profile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val cap = (method.invoke(profile) as? Double)?.toInt() ?: 0
            if (cap in 2000..12000) {
                prefs.edit().putInt(KEY_DESIGN_CAPACITY_MAH, cap).apply()
                return cap
            }
        } catch (_: Exception) {
        }
        return 5000
    }

    private fun loadDualCellMode(): DualCellMode {
        val saved = prefs.getString(KEY_DUAL_CELL_MODE, DualCellMode.AUTO.name)
        return try {
            DualCellMode.valueOf(saved ?: DualCellMode.AUTO.name)
        } catch (_: Exception) {
            DualCellMode.AUTO
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language) ?: "de"
        return AppLanguage.fromCode(code)
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_TARGET_LEVEL = "target_level"
        private const val KEY_SOUND_ALERT = "sound_alert"
        private const val KEY_VIBRATION_ALERT = "vibration_alert"
        private const val KEY_BG_SERVICE = "background_service"
        private const val KEY_TEMP_ALERT = "temp_alert"
        private const val KEY_TEMP_ALERT_THRESHOLD = "temp_alert_threshold"
        private const val KEY_DESIGN_CAPACITY_MAH = "design_capacity_mah"
        private const val KEY_LAST_SOH_SNAPSHOT = "last_soh_snapshot_time"
        private const val KEY_DUAL_CELL_MODE = "dual_cell_mode"
        private const val KEY_WIDGET_DETAILED = "widget_detailed_mode"
        private const val KEY_ACTIVE_CHARGER_PROFILE = "active_charger_profile"

        fun scoreToGrade(score: Int): String = when {
            score >= 90 -> "A+"
            score >= 78 -> "A"
            score >= 62 -> "B"
            score >= 45 -> "C"
            score > 0 -> "D"
            else -> "–"
        }

        /**
         * Bewertet Netzteil- und Kabelqualität anhand von Übertragungseffizienz, Stromstärke und Spannungsniveau.
         */
        fun evaluateChargerQuality(
            isCharging: Boolean,
            source: ChargingSource,
            currentMa: Double,
            voltageVolt: Double,
            chargerVoltageVolt: Double,
            powerWatts: Double,
            cellPowerWatts: Double
        ): Triple<Int, String, Int> {
            if (!isCharging) return Triple(0, "–", 0)

            val absMa = Math.abs(currentMa)
            val rawEff = if (powerWatts > 0.5) (cellPowerWatts / powerWatts) * 100.0 else 85.0
            val efficiencyPercent = rawEff.toInt().coerceIn(68, 99)

            val throughputScore = when (source) {
                ChargingSource.AC -> when {
                    absMa >= 2800.0 || powerWatts >= 22.0 -> 96
                    absMa >= 1800.0 || powerWatts >= 14.0 -> 88
                    absMa >= 1200.0 -> 74
                    absMa >= 700.0 -> 56
                    else -> 38
                }
                ChargingSource.WIRELESS -> when {
                    absMa >= 1300.0 || powerWatts >= 10.0 -> 88
                    absMa >= 800.0 -> 74
                    else -> 54
                }
                ChargingSource.USB -> when {
                    absMa >= 900.0 -> 82
                    absMa >= 450.0 -> 68
                    else -> 44
                }
                else -> 65
            }

            val voltageBonus = if (chargerVoltageVolt >= 8.5 || voltageVolt >= 4.0) 4 else 0
            val combinedScore = ((throughputScore * 0.72) + (efficiencyPercent * 0.28) + voltageBonus)
                .toInt()
                .coerceIn(25, 99)

            return Triple(combinedScore, scoreToGrade(combinedScore), efficiencyPercent)
        }
    }
}
