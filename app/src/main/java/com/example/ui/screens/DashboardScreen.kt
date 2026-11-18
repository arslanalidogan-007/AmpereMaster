package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.SavedChargerEntity
import com.example.data.model.BatteryMetrics
import com.example.data.model.ChargerBenchmarkState
import com.example.localization.LocalAppStrings
import com.example.sensor.DeviceInfo
import com.example.ui.components.ChargerBenchmarkCard
import com.example.ui.components.ChargingCurveCanvas
import com.example.ui.components.ChargingHeroCard
import com.example.ui.components.HardwareInfoCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

/**
 * Haupt-Dashboard mit adaptivem Tablet-Split-Layout, Hero-Akkuanzeige, 4 Kern-Metriken,
 * Netzteil-Benchmark inkl. Saved Chargers, Ladekurve und Hardware-Erkennung.
 */
@Composable
fun DashboardScreen(
    metrics: BatteryMetrics,
    targetLevel: Int,
    samples: List<BatterySampleEntity>,
    deviceInfo: DeviceInfo,
    benchmarkState: ChargerBenchmarkState = ChargerBenchmarkState(),
    savedChargers: List<SavedChargerEntity> = emptyList(),
    activeChargerProfile: String = "",
    onStartBenchmark: () -> Unit = {},
    onStopBenchmark: () -> Unit = {},
    onSaveChargerProfile: (name: String, ratedWatts: Int) -> Unit = { _, _ -> },
    onAssignChargerProfile: (name: String) -> Unit = {},
    onDeleteSavedCharger: (Long) -> Unit = {},
    onOpenConcept: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    val voltageSecondary = when {
        metrics.isCharging && metrics.chargerVoltageVolt > 5.0 ->
            String.format(Locale.US, "USB: %.1f V", metrics.chargerVoltageVolt)
        metrics.dualCellActive -> "${strings.batteryCellLabel} (2S)"
        else -> strings.batteryCellLabel
    }

    val powerSecondary = when {
        metrics.isCharging && metrics.isFastCharging ->
            String.format(Locale.US, "USB-PD • %.1f W", metrics.batteryCellPowerWatts)
        metrics.isCharging ->
            String.format(Locale.US, "%.1f W (%s)", metrics.batteryCellPowerWatts, strings.batteryCellLabel)
        else ->
            String.format(Locale.US, "%s (%.2f A)", strings.dischargeLabel, metrics.currentAmp)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        val isWide = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isWide) {
                // Tablet / Foldable Split-Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChargingHeroCard(
                            metrics = metrics,
                            targetLevel = targetLevel
                        )
                        MetricsGrid(
                            metrics = metrics,
                            voltageSecondary = voltageSecondary,
                            powerSecondary = powerSecondary
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChargerBenchmarkCard(
                            metrics = metrics,
                            benchmarkState = benchmarkState,
                            savedChargers = savedChargers,
                            activeChargerProfile = activeChargerProfile,
                            onStartBenchmark = onStartBenchmark,
                            onStopBenchmark = onStopBenchmark,
                            onSaveChargerProfile = onSaveChargerProfile,
                            onAssignChargerProfile = onAssignChargerProfile,
                            onDeleteSavedCharger = onDeleteSavedCharger
                        )
                        ChargingCurveCanvas(samples = samples)
                    }
                }
            } else {
                // Kompakt-Layout (Smartphone)
                ChargingHeroCard(
                    metrics = metrics,
                    targetLevel = targetLevel
                )

                MetricsGrid(
                    metrics = metrics,
                    voltageSecondary = voltageSecondary,
                    powerSecondary = powerSecondary
                )

                ChargerBenchmarkCard(
                    metrics = metrics,
                    benchmarkState = benchmarkState,
                    savedChargers = savedChargers,
                    activeChargerProfile = activeChargerProfile,
                    onStartBenchmark = onStartBenchmark,
                    onStopBenchmark = onStopBenchmark,
                    onSaveChargerProfile = onSaveChargerProfile,
                    onAssignChargerProfile = onAssignChargerProfile,
                    onDeleteSavedCharger = onDeleteSavedCharger
                )

                ChargingCurveCanvas(samples = samples)
            }

            HardwareInfoCard(
                deviceInfo = deviceInfo,
                isHardwareCurrentSupported = metrics.isHardwareCurrentSupported,
                onOpenConcept = onOpenConcept
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricsGrid(
    metrics: BatteryMetrics,
    voltageSecondary: String,
    powerSecondary: String
) {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = strings.metricCurrent,
                value = String.format(Locale.US, "%.0f", metrics.currentMilliAmp),
                unit = "mA",
                icon = Icons.Default.Speed,
                accentColor = if (metrics.isCharging) ElectricGreen else ChargingCyan,
                secondaryValue = String.format(Locale.US, "%.2f A", metrics.currentAmp),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = strings.metricVoltage,
                value = String.format(Locale.US, "%.2f", metrics.voltageVolt),
                unit = "V",
                icon = Icons.Default.ElectricBolt,
                accentColor = ChargingCyan,
                secondaryValue = voltageSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = strings.metricPower,
                value = String.format(Locale.US, "%.1f", metrics.powerWatts),
                unit = "W",
                icon = Icons.Default.FlashOn,
                accentColor = ElectricGreen,
                secondaryValue = powerSecondary,
                modifier = Modifier.weight(1f)
            )

            val tempColor = if (metrics.temperatureCelsius > 42.0f) WarningAmber else ChargingCyan
            MetricCard(
                title = strings.metricTemperature,
                value = String.format(Locale.US, "%.1f", metrics.temperatureCelsius),
                unit = "°C",
                icon = Icons.Default.Thermostat,
                accentColor = tempColor,
                secondaryValue = String.format(Locale.US, "%.1f °F", metrics.temperatureFahrenheit),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
