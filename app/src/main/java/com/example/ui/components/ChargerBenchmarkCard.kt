package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SavedChargerEntity
import com.example.data.model.BatteryMetrics
import com.example.data.model.ChargerBenchmarkState
import com.example.localization.LocalAppStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Karte für Netzteil- & Kabel-Gütebewertung inkl. interaktivem 30-Sekunden-Stresstest
 * sowie gespeicherter Netzteil- & Kabel-Datenbank („Saved Chargers“, v1.0.4).
 */
@Composable
fun ChargerBenchmarkCard(
    metrics: BatteryMetrics,
    benchmarkState: ChargerBenchmarkState,
    savedChargers: List<SavedChargerEntity> = emptyList(),
    activeChargerProfile: String = "",
    onStartBenchmark: () -> Unit,
    onStopBenchmark: () -> Unit,
    onSaveChargerProfile: (name: String, ratedWatts: Int) -> Unit = { _, _ -> },
    onAssignChargerProfile: (name: String) -> Unit = {},
    onDeleteSavedCharger: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showSaveDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }

    val activeGrade = if (benchmarkState.isCompleted || benchmarkState.isRunning) {
        benchmarkState.grade
    } else {
        metrics.chargerQualityGrade
    }
    val activeScore = if (benchmarkState.isCompleted || benchmarkState.isRunning) {
        benchmarkState.score
    } else {
        metrics.chargerQualityScore
    }
    val activeEfficiency = if (benchmarkState.isCompleted || benchmarkState.isRunning) {
        benchmarkState.efficiencyPercent
    } else {
        metrics.cableEfficiencyPercent
    }

    val targetGradeColor = when (activeGrade) {
        "A+", "A" -> ElectricGreen
        "B" -> ChargingCyan
        "C" -> WarningAmber
        "D" -> AlertRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val gradeColor by animateColorAsState(targetValue = targetGradeColor, label = "gradeColor")

    val gradeLabel = when (activeGrade) {
        "A+" -> strings.gradeExcellent
        "A" -> strings.gradeGood
        "B" -> strings.gradeFair
        "C" -> strings.gradeWeak
        "D" -> strings.gradePoor
        else -> strings.stressTestNeedsCharger
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, gradeColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("charger_benchmark_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kopfzeile mit Live-Bewertung und Klassen-Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(gradeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = strings.chargerQualityTitle,
                        tint = gradeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.chargerQualityTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (metrics.isCharging || benchmarkState.isCompleted) {
                            "$gradeLabel • ${metrics.detectedProtocol}"
                        } else {
                            strings.stressTestNeedsCharger
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeChargerProfile.isNotBlank()) {
                        Text(
                            text = "${strings.activeProfileLabel}: $activeChargerProfile",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ChargingCyan
                        )
                    }
                }
            }

            if (activeScore > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradeColor.copy(alpha = 0.16f))
                        .border(1.dp, gradeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeGrade,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = gradeColor
                        )
                        Text(
                            text = "$activeScore/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live-Effizienzleiste wenn Netzteil verbunden oder Ergebnis vorhanden
        if (metrics.isCharging || benchmarkState.isCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BenchmarkMiniMetric(
                    label = strings.cableEfficiency,
                    value = "$activeEfficiency %",
                    accent = gradeColor,
                    modifier = Modifier.weight(1f)
                )
                BenchmarkMiniMetric(
                    label = strings.metricPower,
                    value = String.format(
                        Locale.US,
                        "%.1f W",
                        if (benchmarkState.isCompleted || benchmarkState.isRunning) benchmarkState.peakPowerWatts else metrics.powerWatts
                    ),
                    accent = ChargingCyan,
                    modifier = Modifier.weight(1f)
                )
                BenchmarkMiniMetric(
                    label = strings.metricCurrent,
                    value = String.format(
                        Locale.US,
                        "%.0f mA",
                        if (benchmarkState.isCompleted || benchmarkState.isRunning) benchmarkState.peakCurrentMilliAmp else Math.abs(metrics.currentMilliAmp)
                    ),
                    accent = ElectricGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stresstest Fortschritt & Detailwerte
        if (benchmarkState.isRunning) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.stressTestRunning,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ChargingCyan
                    )
                    Text(
                        text = "${benchmarkState.secondsRemaining} s",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGreen
                    )
                }
                LinearProgressIndicator(
                    progress = { benchmarkState.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ElectricGreen,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        if (benchmarkState.isCompleted || (benchmarkState.isRunning && benchmarkState.sampleCount > 0)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (benchmarkState.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = gradeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.stressTestComplete,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = gradeColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.maxStableCurrent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.0f mA (Ø %.0f mA)",
                            benchmarkState.peakCurrentMilliAmp,
                            benchmarkState.avgCurrentMilliAmp
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.voltageStability,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${benchmarkState.stabilityPercent} % (Δ ${benchmarkState.voltageSagMv} mV)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (benchmarkState.stabilityPercent >= 85) ElectricGreen else WarningAmber
                    )
                }
            }
        }

        // Start / Stop & Profil speichern Buttons (mind. 48dp Touch-Target)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (benchmarkState.isRunning) {
                OutlinedButton(
                    onClick = onStopBenchmark,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("stress_test_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = strings.stressTestStop,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.stressTestStop)
                }
            } else {
                Button(
                    onClick = onStartBenchmark,
                    enabled = metrics.isCharging,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("stress_test_start_button")
                ) {
                    Icon(
                        imageVector = if (benchmarkState.isCompleted) Icons.Default.Speed else Icons.Default.PlayArrow,
                        contentDescription = strings.stressTestStart,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.stressTestStart,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (metrics.isCharging || benchmarkState.isCompleted) {
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("save_charger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = strings.saveChargerButton,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Gespeicherte Netzteile & Kabel-Datenbank (Saved Chargers)
        if (savedChargers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.savedChargersTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ChargingCyan
                )
                savedChargers.take(5).forEach { charger ->
                    val isSelected = charger.name == activeChargerProfile
                    val badgeColor = when (charger.grade) {
                        "A+", "A" -> ElectricGreen
                        "B" -> ChargingCyan
                        "C" -> WarningAmber
                        else -> AlertRed
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) ElectricGreen.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .border(
                                1.dp,
                                if (isSelected) ElectricGreen.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onAssignChargerProfile(charger.name) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${charger.name} (${charger.ratedWatts}W)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) ElectricGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s %d%% • %.1f W • %.0f mA • %s",
                                    charger.grade,
                                    charger.cableEfficiencyPercent,
                                    charger.peakPowerWatts,
                                    charger.peakCurrentMilliAmp,
                                    dateFormat.format(Date(charger.lastTestedAt))
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor
                            )
                        }
                        IconButton(
                            onClick = { onDeleteSavedCharger(charger.id) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var chargerName by remember { mutableStateOf("") }
        var ratedWatts by remember { mutableStateOf("25") }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(strings.saveChargerButton) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chargerName,
                        onValueChange = { chargerName = it.take(40) },
                        label = { Text(strings.chargerNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ratedWatts,
                        onValueChange = { raw -> ratedWatts = raw.filter { it.isDigit() }.take(3) },
                        label = { Text(strings.chargerWattsLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = chargerName.isNotBlank(),
                    onClick = {
                        val watts = ratedWatts.toIntOrNull() ?: 25
                        onSaveChargerProfile(chargerName, watts)
                        showSaveDialog = false
                    }
                ) {
                    Text(strings.saveChargerButton)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BenchmarkMiniMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}
