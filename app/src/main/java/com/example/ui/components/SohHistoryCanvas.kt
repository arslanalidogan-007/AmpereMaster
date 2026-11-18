package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.model.BatteryMetrics
import com.example.localization.LocalAppStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Akkukapazitäts-Alterungstracker (SOH - State of Health) mit Kalibrierung und Mehrwochen-Verlaufsdiagramm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SohHistoryCanvas(
    metrics: BatteryMetrics,
    designCapacityMah: Int,
    snapshots: List<BatteryHealthSnapshotEntity>,
    onSetDesignCapacity: (Int) -> Unit,
    onRecordSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var selectedRangeDays by remember { mutableIntStateOf(30) } // 7, 30, 0 (All)
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var selectedSnapshot by remember { mutableStateOf<BatteryHealthSnapshotEntity?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }

    val filteredSnapshots = remember(snapshots, selectedRangeDays) {
        val sorted = snapshots.sortedBy { it.timestamp }
        if (selectedRangeDays <= 0) {
            sorted
        } else {
            val cutoff = System.currentTimeMillis() - selectedRangeDays * 24L * 60L * 60L * 1000L
            val within = sorted.filter { it.timestamp >= cutoff }
            if (within.isNotEmpty()) within else sorted
        }
    }

    val sohColor = when {
        metrics.sohPercent >= 88f -> ElectricGreen
        metrics.sohPercent >= 78f -> ChargingCyan
        metrics.sohPercent >= 68f -> WarningAmber
        else -> AlertRed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, sohColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("soh_history_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kopfzeile mit aktuellem SOH-Status
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
                        .background(sohColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = strings.sohTitle,
                        tint = sohColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.sohTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${metrics.capacityMah} mAh / $designCapacityMah mAh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(sohColor.copy(alpha = 0.16f))
                    .border(1.dp, sohColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1f %%", metrics.sohPercent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = sohColor
                    )
                    Text(
                        text = strings.sohCurrentHealth,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Aktions-Buttons: Design-Kapazität kalibrieren & Messpunkt manuell speichern
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showCalibrateDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("soh_calibrate_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = strings.sohCalibrate,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.sohCalibrate, maxLines = 1)
            }

            OutlinedButton(
                onClick = onRecordSnapshot,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("soh_record_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = strings.sohRecordNow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.sohRecordNow, maxLines = 1)
            }
        }

        // Zeitraum-Filter (7d / 30d / Alle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.sohHistoryTitle,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    7 to strings.sohRange7d,
                    30 to strings.sohRange30d,
                    0 to strings.sohRangeAll
                ).forEach { (days, label) ->
                    FilterChip(
                        selected = selectedRangeDays == days,
                        onClick = {
                            selectedRangeDays = days
                            selectedSnapshot = null
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricGreen.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricGreen
                        )
                    )
                }
            }
        }

        // Scrubber Tooltip falls ein Snapshot berührt wurde
        selectedSnapshot?.let { snap ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, ChargingCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormatter.format(Date(snap.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${snap.estimatedCapacityMah} / ${snap.designCapacityMah} mAh",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ChargingCyan
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1f %%", snap.sohPercent),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = ElectricGreen
                )
            }
        }

        // Verlaufsdiagramm auf Canvas
        if (filteredSnapshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.sohEmptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val minTs = filteredSnapshots.first().timestamp
            val maxTs = filteredSnapshots.last().timestamp
            val spanTs = Math.max(1000L, maxTs - minTs)
            val minSohBound = (filteredSnapshots.minOf { it.sohPercent } - 8f).coerceIn(30f, 80f)
            val maxSohBound = 100f
            val sohSpan = Math.max(10f, maxSohBound - minSohBound)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredSnapshots) {
                            detectTapGestures { offset ->
                                val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                val targetTs = minTs + (ratio * spanTs).toLong()
                                selectedSnapshot = filteredSnapshots.minByOrNull { Math.abs(it.timestamp - targetTs) }
                            }
                        }
                        .pointerInput(filteredSnapshots) {
                            detectDragGestures { change, _ ->
                                val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                                val targetTs = minTs + (ratio * spanTs).toLong()
                                selectedSnapshot = filteredSnapshots.minByOrNull { Math.abs(it.timestamp - targetTs) }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    // Gitterlinien
                    for (i in 0..3) {
                        val y = (i / 3f) * height
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }

                    val pts = filteredSnapshots.mapIndexed { idx, snap ->
                        val x = if (filteredSnapshots.size == 1) {
                            width / 2f
                        } else {
                            ((snap.timestamp - minTs).toFloat() / spanTs.toFloat()) * width
                        }
                        val normY = ((snap.sohPercent - minSohBound) / sohSpan).coerceIn(0f, 1f)
                        val y = height - (normY * height)
                        Offset(x, y)
                    }

                    if (pts.size >= 2) {
                        val linePath = Path()
                        val fillPath = Path()
                        linePath.moveTo(pts.first().x, pts.first().y)
                        fillPath.moveTo(pts.first().x, height)
                        fillPath.lineTo(pts.first().x, pts.first().y)

                        for (i in 1 until pts.size) {
                            val p0 = pts[i - 1]
                            val p1 = pts[i]
                            val midX = (p0.x + p1.x) / 2f
                            linePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                        fillPath.lineTo(pts.last().x, height)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(sohColor.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(listOf(ChargingCyan, sohColor)),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    pts.forEachIndexed { idx, pt ->
                        val isSelected = selectedSnapshot?.id == filteredSnapshots[idx].id && filteredSnapshots[idx].id != 0L
                        drawCircle(
                            color = if (isSelected) Color.White else sohColor,
                            radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }

    // Kalibrierungs-Dialog für die Werks-Nennkapazität (mAh)
    if (showCalibrateDialog) {
        var inputText by remember(designCapacityMah) { mutableStateOf(designCapacityMah.toString()) }
        val presets = listOf(4000, 4500, 5000, 5500, 6000)

        AlertDialog(
            onDismissRequest = { showCalibrateDialog = false },
            title = { Text(strings.sohDesignCapacity) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { raw -> inputText = raw.filter { it.isDigit() }.take(5) },
                        label = { Text("${strings.sohDesignCapacity} (mAh)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = inputText.toIntOrNull() == preset,
                                onClick = { inputText = preset.toString() },
                                label = { Text("$preset mAh") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputText.toIntOrNull()
                        if (parsed != null) {
                            onSetDesignCapacity(parsed.coerceIn(2000, 12000))
                        }
                        showCalibrateDialog = false
                    }
                ) {
                    Text(strings.sohCalibrate)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCalibrateDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
