package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BatterySampleEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.localization.LocalAppStrings
import com.example.ui.components.ChargingCurveCanvas
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vollständige Ladekurven-Detailansicht mit Pinch-to-Zoom, Dual-Sitzungs-Vergleich,
 * Messpunkt-Statistiken, CSV-Export und Verlauf-Löschen-Funktion.
 */
@Composable
fun ChargingGraphScreen(
    samples: List<BatterySampleEntity>,
    allSessions: List<ChargingSessionEntity> = emptyList(),
    isCompareMode: Boolean = false,
    compareSessionA: ChargingSessionEntity? = null,
    compareSessionB: ChargingSessionEntity? = null,
    compareSamplesA: List<BatterySampleEntity> = emptyList(),
    compareSamplesB: List<BatterySampleEntity> = emptyList(),
    onSetCompareMode: (Boolean) -> Unit = {},
    onSelectSessionA: (ChargingSessionEntity?) -> Unit = {},
    onSelectSessionB: (ChargingSessionEntity?) -> Unit = {},
    showClearConfirm: Boolean,
    onShowClearConfirm: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onCsvExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    val sessionFormatter = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

    fun formatSessionLabel(session: ChargingSessionEntity?): String {
        if (session == null) return strings.compareSelectPrompt
        val date = sessionFormatter.format(Date(session.startTime))
        val endPct = if (session.endLevelPercent > 0.0) session.endLevelPercent else session.startLevelPercent
        val profileTag = if (session.chargerProfileName.isNotBlank()) " • ${session.chargerProfileName}" else ""
        return String.format(
            Locale.US,
            "%s • %.0f%%→%.0f%% • %s%s",
            date,
            session.startLevelPercent,
            endPct,
            session.chargingSource,
            profileTag
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("charging_graph_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kopfzeile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.graphTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.graphSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (samples.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCsvExport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChargingCyan),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = strings.csvExportTitle,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
                    }

                    OutlinedButton(
                        onClick = { onShowClearConfirm(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.graphClearHistory,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // v1.0.3: Vergleichsmodus Toggle + Sitzungsauswahl (Session A vs. Session B)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isCompareMode,
                onClick = { onSetCompareMode(!isCompareMode) },
                label = { Text(strings.compareMode) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChargingCyan.copy(alpha = 0.2f),
                    selectedLabelColor = ChargingCyan,
                    selectedLeadingIconColor = ChargingCyan
                ),
                modifier = Modifier.testTag("compare_mode_toggle")
            )
        }

        if (isCompareMode) {
            if (allSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = strings.compareNoSessions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionSelectorRow(
                        title = strings.compareSessionA,
                        selectedText = formatSessionLabel(compareSessionA),
                        accent = ElectricGreen,
                        sessions = allSessions,
                        formatLabel = ::formatSessionLabel,
                        onSelect = onSelectSessionA,
                        testTag = "compare_session_a_selector"
                    )
                    SessionSelectorRow(
                        title = strings.compareSessionB,
                        selectedText = formatSessionLabel(compareSessionB),
                        accent = WarningAmber,
                        sessions = allSessions,
                        formatLabel = ::formatSessionLabel,
                        onSelect = onSelectSessionB,
                        testTag = "compare_session_b_selector"
                    )
                }
            }
        }

        // Interaktiver Canvas Graph mit Pinch-to-Zoom & Dual-Vergleich
        ChargingCurveCanvas(
            samples = samples,
            isCompareMode = isCompareMode,
            compareSamplesA = compareSamplesA,
            compareSamplesB = compareSamplesB
        )

        // Vergleichskarte für beide ausgewählte Sitzungen oder Einzel-Statistik
        if (isCompareMode && (compareSessionA != null || compareSessionB != null)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = ChargingCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.compareMode,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    compareSessionA?.let { a ->
                        val durA = if (a.endTime > a.startTime) Math.max(1L, (a.endTime - a.startTime) / 60_000L) else 0L
                        StatRow(
                            label = "${strings.compareSessionA} (${strings.peakPower})",
                            value = String.format(Locale.US, "%.1f W • Ø %.0f mA • %d min", a.maxPowerWatts, a.avgCurrentMilliAmp, durA),
                            valueColor = ElectricGreen
                        )
                    }

                    compareSessionB?.let { b ->
                        val durB = if (b.endTime > b.startTime) Math.max(1L, (b.endTime - b.startTime) / 60_000L) else 0L
                        StatRow(
                            label = "${strings.compareSessionB} (${strings.peakPower})",
                            value = String.format(Locale.US, "%.1f W • Ø %.0f mA • %d min", b.maxPowerWatts, b.avgCurrentMilliAmp, durB),
                            valueColor = WarningAmber
                        )
                    }
                }
            }
        } else if (samples.isNotEmpty()) {
            val maxCurrent = samples.maxOfOrNull { it.currentMilliAmp } ?: 0.0
            val minCurrent = samples.minOfOrNull { it.currentMilliAmp } ?: 0.0
            val avgCurrent = samples.map { it.currentMilliAmp }.average()
            val maxVoltage = samples.maxOfOrNull { it.voltageVolt } ?: 0.0
            val avgVoltage = samples.map { it.voltageVolt }.average()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = ChargingCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.statsSeriesTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    StatRow(strings.statsDataPoints, "${samples.size} ${strings.measurementsUnit}")
                    StatRow(strings.statsMaxCurrent, String.format(Locale.US, "%.0f mA", maxCurrent), ElectricGreen)
                    StatRow(strings.statsMinCurrent, String.format(Locale.US, "%.0f mA", minCurrent))
                    StatRow(strings.statsAvgCurrent, String.format(Locale.US, "%.0f mA", avgCurrent), ChargingCyan)
                    StatRow(strings.statsMaxVoltage, String.format(Locale.US, "%.2f V", maxVoltage))
                    StatRow(strings.statsAvgVoltage, String.format(Locale.US, "%.2f V", avgVoltage))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Bestätigungsdialog zum Löschen der Historie
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { onShowClearConfirm(false) },
            title = { Text(strings.graphClearHistory) },
            text = { Text(strings.graphClearConfirm) },
            confirmButton = {
                Button(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onShowClearConfirm(false) }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SessionSelectorRow(
    title: String,
    selectedText: String,
    accent: Color,
    sessions: List<ChargingSessionEntity>,
    formatLabel: (ChargingSessionEntity?) -> String,
    onSelect: (ChargingSessionEntity?) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = title,
                    tint = accent
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = { Text(formatLabel(session), style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(session)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}
