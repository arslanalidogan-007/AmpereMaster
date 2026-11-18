package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AmpereApplication
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.local.entity.SavedChargerEntity
import com.example.data.model.BatteryMetrics
import com.example.data.model.ChargerBenchmarkState
import com.example.data.model.ChargingSessionSummary
import com.example.data.model.DualCellMode
import com.example.localization.AppLanguage
import com.example.sensor.DeviceInfo
import com.example.sensor.DeviceInspector
import com.example.service.BatteryTrackingService
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel zur Verwaltung des gesamten UI-Zustands, Metriken-Streams, Benchmark, SOH-Historie und Sitzungs-Vergleich.
 */
class AmpereViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AmpereApplication).repository

    // Live-Akkudaten
    val metrics: StateFlow<BatteryMetrics> = repository.batteryMetrics

    // Konfigurierte App-Einstellungen
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val currentLanguage: StateFlow<AppLanguage> = repository.currentLanguage
    val targetChargeLevel: StateFlow<Int> = repository.targetChargeLevel
    val soundAlertEnabled: StateFlow<Boolean> = repository.soundAlertEnabled
    val vibrationAlertEnabled: StateFlow<Boolean> = repository.vibrationAlertEnabled
    val backgroundServiceEnabled: StateFlow<Boolean> = repository.backgroundServiceEnabled
    val tempAlertEnabled: StateFlow<Boolean> = repository.tempAlertEnabled
    val tempAlertThreshold: StateFlow<Int> = repository.tempAlertThreshold
    val designCapacityMah: StateFlow<Int> = repository.designCapacityMah
    val dualCellMode: StateFlow<DualCellMode> = repository.dualCellMode
    val widgetDetailedMode: StateFlow<Boolean> = repository.widgetDetailedMode
    val activeChargerProfile: StateFlow<String> = repository.activeChargerProfile

    // Ladesitzungs-Zusammenfassung nach Abstecken
    val lastSessionSummary: StateFlow<ChargingSessionSummary?> = repository.lastSessionSummary

    // v1.0.3: 30s Netzteil- & Kabel-Stresstest
    val benchmarkState: StateFlow<ChargerBenchmarkState> = repository.benchmarkState

    // Ladekurven-Historie aus der Room-Datenbank
    val latestSamples: StateFlow<List<BatterySampleEntity>> = repository.latestSamples
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alle aufgezeichneten Ladesitzungen für den Direktvergleich
    val allSessions: StateFlow<List<ChargingSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // SOH / Kapazitäts-Historie aus Room
    val healthSnapshots: StateFlow<List<BatteryHealthSnapshotEntity>> = repository.healthSnapshots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // v1.0.4: Gespeicherte Netzteile & Kabel aus Room
    val savedChargers: StateFlow<List<SavedChargerEntity>> = repository.savedChargers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Vergleichsmodus für 2 Ladesitzungen
    private val _isCompareMode = MutableStateFlow(false)
    val isCompareMode: StateFlow<Boolean> = _isCompareMode.asStateFlow()

    private val _compareSessionA = MutableStateFlow<ChargingSessionEntity?>(null)
    val compareSessionA: StateFlow<ChargingSessionEntity?> = _compareSessionA.asStateFlow()

    private val _compareSessionB = MutableStateFlow<ChargingSessionEntity?>(null)
    val compareSessionB: StateFlow<ChargingSessionEntity?> = _compareSessionB.asStateFlow()

    private val _compareSamplesA = MutableStateFlow<List<BatterySampleEntity>>(emptyList())
    val compareSamplesA: StateFlow<List<BatterySampleEntity>> = _compareSamplesA.asStateFlow()

    private val _compareSamplesB = MutableStateFlow<List<BatterySampleEntity>>(emptyList())
    val compareSamplesB: StateFlow<List<BatterySampleEntity>> = _compareSamplesB.asStateFlow()

    // Hardware- und Geräte-Erkennung
    val deviceInfo: DeviceInfo = DeviceInspector.inspect()

    // Dialog-Zustände
    private val _showChangelog = MutableStateFlow(false)
    val showChangelog: StateFlow<Boolean> = _showChangelog.asStateFlow()

    private val _showSupport = MutableStateFlow(false)
    val showSupport: StateFlow<Boolean> = _showSupport.asStateFlow()

    private val _showOverviewConcept = MutableStateFlow(false)
    val showOverviewConcept: StateFlow<Boolean> = _showOverviewConcept.asStateFlow()

    private val _showClearConfirm = MutableStateFlow(false)
    val showClearConfirm: StateFlow<Boolean> = _showClearConfirm.asStateFlow()

    // --- Benutzerinteraktionen & Einstellungsänderungen ---

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
    }

    fun setTargetChargeLevel(level: Int) {
        repository.setTargetChargeLevel(level)
    }

    fun setSoundAlertEnabled(enabled: Boolean) {
        repository.setSoundAlertEnabled(enabled)
    }

    fun setVibrationAlertEnabled(enabled: Boolean) {
        repository.setVibrationAlertEnabled(enabled)
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        repository.setBackgroundServiceEnabled(enabled)
        val context = getApplication<Application>().applicationContext
        if (enabled && metrics.value.isCharging) {
            BatteryTrackingService.start(context)
        } else if (!enabled) {
            BatteryTrackingService.stop(context)
        }
    }

    fun setTempAlertEnabled(enabled: Boolean) {
        repository.setTempAlertEnabled(enabled)
    }

    fun setTempAlertThreshold(threshold: Int) {
        repository.setTempAlertThreshold(threshold)
    }

    fun dismissSessionSummary() {
        repository.dismissSessionSummary()
    }

    // --- v1.0.3 Actions ---

    fun startChargerBenchmark() {
        repository.startChargerBenchmark()
    }

    fun stopChargerBenchmark() {
        repository.stopChargerBenchmark()
    }

    fun setDesignCapacityMah(capacityMah: Int) {
        repository.setDesignCapacityMah(capacityMah)
    }

    fun recordHealthSnapshotNow() {
        repository.recordHealthSnapshotNow()
    }

    // --- v1.0.4 Actions ---

    fun setDualCellMode(mode: DualCellMode) {
        repository.setDualCellMode(mode)
    }

    fun setWidgetDetailedMode(detailed: Boolean) {
        repository.setWidgetDetailedMode(detailed)
    }

    fun saveChargerProfile(name: String, ratedWatts: Int) {
        repository.saveChargerProfile(name, ratedWatts)
    }

    fun assignActiveChargerProfile(name: String) {
        repository.assignActiveChargerProfile(name)
    }

    fun deleteSavedCharger(chargerId: Long) {
        repository.deleteSavedCharger(chargerId)
    }

    fun setCompareMode(enabled: Boolean) {
        _isCompareMode.value = enabled
        if (enabled) {
            val sessions = allSessions.value
            if (_compareSessionA.value == null && sessions.isNotEmpty()) {
                selectCompareSessionA(sessions[0])
            }
            if (_compareSessionB.value == null && sessions.size >= 2) {
                selectCompareSessionB(sessions[1])
            }
        }
    }

    fun selectCompareSessionA(session: ChargingSessionEntity?) {
        _compareSessionA.value = session
        if (session == null) {
            _compareSamplesA.value = emptyList()
            return
        }
        viewModelScope.launch {
            _compareSamplesA.value = repository.getSamplesForSession(session.id)
        }
    }

    fun selectCompareSessionB(session: ChargingSessionEntity?) {
        _compareSessionB.value = session
        if (session == null) {
            _compareSamplesB.value = emptyList()
            return
        }
        viewModelScope.launch {
            _compareSamplesB.value = repository.getSamplesForSession(session.id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _compareSessionA.value = null
            _compareSessionB.value = null
            _compareSamplesA.value = emptyList()
            _compareSamplesB.value = emptyList()
            _showClearConfirm.value = false
        }
    }

    fun setShowChangelog(show: Boolean) {
        _showChangelog.value = show
    }

    fun setShowSupport(show: Boolean) {
        _showSupport.value = show
    }

    fun setShowOverviewConcept(show: Boolean) {
        _showOverviewConcept.value = show
    }

    fun setShowClearConfirm(show: Boolean) {
        _showClearConfirm.value = show
    }
}
