package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BatterySampleEntity
import com.example.localization.LocalAppStrings
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Performante, interaktive Jetpack Compose Canvas Ladekurve.
 * Unterstützt Pinch-to-Zoom (1x–8x), horizontales Panning, 1-Finger-Scrubber
 * sowie den Dual-Sitzungs-Vergleichsmodus (Session A vs. Session B).
 */
@Composable
fun ChargingCurveCanvas(
    samples: List<BatterySampleEntity>,
    modifier: Modifier = Modifier,
    isCompareMode: Boolean = false,
    compareSamplesA: List<BatterySampleEntity> = emptyList(),
    compareSamplesB: List<BatterySampleEntity> = emptyList()
) {
    val strings = LocalAppStrings.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var zoomScale by remember(isCompareMode) { mutableFloatStateOf(1f) }
    var panOffset by remember(isCompareMode) { mutableFloatStateOf(0f) } // normalisiert [0f, 1f - 1f/zoomScale]

    var selectedSample by remember(isCompareMode) { mutableStateOf<BatterySampleEntity?>(null) }
    var selectedCompareElapsedMs by remember(isCompareMode) { mutableStateOf<Long?>(null) }

    val activeEmpty = if (isCompareMode) {
        compareSamplesA.isEmpty() && compareSamplesB.isEmpty()
    } else {
        samples.isEmpty()
    }

    if (activeEmpty) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isCompareMode) strings.compareSelectPrompt else strings.graphEmpty,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.graphEmptyTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val sortedSamples = remember(samples) { samples.sortedBy { it.timestamp } }
    val sortedA = remember(compareSamplesA) { compareSamplesA.sortedBy { it.timestamp } }
    val sortedB = remember(compareSamplesB) { compareSamplesB.sortedBy { it.timestamp } }

    // Zeitbasis: Entweder absolute Zeit (Single Mode) oder relative verstrichene Zeit ab 0 ms (Compare Mode)
    val baseStart = if (isCompareMode) 0L else sortedSamples.first().timestamp
    val baseEnd = if (isCompareMode) {
        val durA = if (sortedA.size >= 2) sortedA.last().timestamp - sortedA.first().timestamp else 60_000L
        val durB = if (sortedB.size >= 2) sortedB.last().timestamp - sortedB.first().timestamp else 60_000L
        Math.max(60_000L, Math.max(durA, durB))
    } else {
        Math.max(sortedSamples.first().timestamp + 1000L, sortedSamples.last().timestamp)
    }
    val totalSpan = Math.max(1000L, baseEnd - baseStart)

    val visibleFraction = 1f / zoomScale.coerceIn(1f, 8f)
    val clampedPan = panOffset.coerceIn(0f, (1f - visibleFraction).coerceAtLeast(0f))
    val visibleStart = baseStart + (clampedPan * totalSpan).toLong()
    val visibleEnd = baseStart + ((clampedPan + visibleFraction) * totalSpan).toLong()
    val visibleSpan = Math.max(500L, visibleEnd - visibleStart)

    fun formatElapsed(ms: Long): String {
        val secTotal = (ms / 1000L).coerceAtLeast(0L)
        val mins = secTotal / 60L
        val secs = secTotal % 60L
        return String.format(Locale.US, "+%dm %02ds", mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(16.dp)
            .testTag("charging_curve_canvas")
    ) {
        // Kopfzeile mit Zoom-Steuerung
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCompareMode) strings.compareMode else strings.graphTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.zoomHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        val nextZoom = (zoomScale / 1.5f).coerceIn(1f, 8f)
                        zoomScale = nextZoom
                        panOffset = panOffset.coerceIn(0f, (1f - 1f / nextZoom).coerceAtLeast(0f))
                    },
                    enabled = zoomScale > 1.05f,
                    modifier = Modifier.size(48.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = strings.zoomReset,
                        tint = if (zoomScale > 1.05f) ChargingCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.1f×", zoomScale),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (zoomScale > 1.05f) ElectricGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        val nextZoom = (zoomScale * 1.5f).coerceIn(1f, 8f)
                        val oldVis = 1f / zoomScale
                        val newVis = 1f / nextZoom
                        val center = panOffset + oldVis / 2f
                        zoomScale = nextZoom
                        panOffset = (center - newVis / 2f).coerceIn(0f, (1f - newVis).coerceAtLeast(0f))
                    },
                    enabled = zoomScale < 7.9f,
                    modifier = Modifier.size(48.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = if (zoomScale < 7.9f) ChargingCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (zoomScale > 1.05f) {
                    OutlinedButton(
                        onClick = {
                            zoomScale = 1f
                            panOffset = 0f
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("zoom_reset_button")
                    ) {
                        Text(strings.zoomReset, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Legende für Dual-Vergleichsmodus
        if (isCompareMode) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ElectricGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.compareSessionA, style = MaterialTheme.typography.labelSmall, color = ElectricGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(WarningAmber))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.compareSessionB, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrubber Tooltip Anzeige bei Berührung
        if (isCompareMode && selectedCompareElapsedMs != null) {
            val elapsed = selectedCompareElapsedMs!!
            val sampleA = if (sortedA.isNotEmpty()) {
                val startA = sortedA.first().timestamp
                sortedA.minByOrNull { Math.abs((it.timestamp - startA) - elapsed) }
            } else null
            val sampleB = if (sortedB.isNotEmpty()) {
                val startB = sortedB.first().timestamp
                sortedB.minByOrNull { Math.abs((it.timestamp - startB) - elapsed) }
            } else null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, ChargingCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatElapsed(elapsed),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ChargingCyan
                )
                sampleA?.let { a ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.compareSessionA, style = MaterialTheme.typography.labelSmall, color = ElectricGreen)
                        Text(
                            text = String.format(Locale.US, "%.2f%% • %.0f mA • %.1f W", a.levelPercent, a.currentMilliAmp, a.powerWatts),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ElectricGreen
                        )
                    }
                }
                sampleB?.let { b ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.compareSessionB, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                        Text(
                            text = String.format(Locale.US, "%.2f%% • %.0f mA • %.1f W", b.levelPercent, b.currentMilliAmp, b.powerWatts),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = WarningAmber
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else if (!isCompareMode && selectedSample != null) {
            val sample = selectedSample!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, ChargingCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timeFormatter.format(Date(sample.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f %%", sample.levelPercent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.0f mA • %.2f V", sample.currentMilliAmp, sample.voltageVolt),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ChargingCyan
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f W", sample.powerWatts),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Interaktiver Canvas Graph (Pinch-to-Zoom + Pan + 1-Finger Scrubber)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clipToBounds()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(visibleStart, visibleSpan, isCompareMode, sortedSamples, sortedA, sortedB) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    // 2-Finger Pinch-to-Zoom & Pan
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    if (zoomChange != 1f || panChange.x != 0f) {
                                        val oldZoom = zoomScale
                                        val newZoom = (oldZoom * zoomChange).coerceIn(1f, 8f)
                                        val oldVis = 1f / oldZoom
                                        val newVis = 1f / newZoom
                                        val center = panOffset + oldVis / 2f
                                        val panShift = if (size.width > 0) -(panChange.x / size.width) * newVis else 0f
                                        zoomScale = newZoom
                                        panOffset = (center - newVis / 2f + panShift).coerceIn(0f, (1f - newVis).coerceAtLeast(0f))
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                } else if (pressedCount == 1) {
                                    val change = event.changes.first()
                                    val curZoom = zoomScale
                                    val curVis = 1f / curZoom
                                    val curStart = baseStart + (panOffset.coerceIn(0f, (1f - curVis).coerceAtLeast(0f)) * totalSpan).toLong()
                                    val curSpan = Math.max(500L, (curVis * totalSpan).toLong())
                                    val progressX = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val targetTime = curStart + (progressX * curSpan).toLong()

                                    if (isCompareMode) {
                                        selectedCompareElapsedMs = targetTime.coerceAtLeast(0L)
                                    } else {
                                        selectedSample = sortedSamples.minByOrNull { Math.abs(it.timestamp - targetTime) }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 16.dp.toPx()
                val graphHeight = height - paddingBottom

                // Horizontale Gitterlinien (0%, 25%, 50%, 75%, 100%)
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = graphHeight - (i.toFloat() / gridSteps.toFloat()) * graphHeight
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                if (isCompareMode) {
                    fun drawSessionSeries(
                        series: List<BatterySampleEntity>,
                        lineColor: Color,
                        dashed: Boolean
                    ) {
                        if (series.isEmpty()) return
                        val startTs = series.first().timestamp
                        val pts = series.mapNotNull { s ->
                            val rel = s.timestamp - startTs
                            val normX = ((rel - visibleStart).toFloat() / visibleSpan.toFloat()) * width
                            val normY = graphHeight - ((s.levelPercent.toFloat().coerceIn(0f, 100f) / 100f) * graphHeight)
                            Offset(normX, normY)
                        }
                        if (pts.size < 2) {
                            val pt = pts.first()
                            drawCircle(lineColor, radius = 5.dp.toPx(), center = pt)
                            return
                        }
                        val path = Path()
                        path.moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            val p0 = pts[i - 1]
                            val p1 = pts[i]
                            val midX = (p0.x + p1.x) / 2f
                            path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
                            )
                        )
                    }

                    drawSessionSeries(sortedA, ElectricGreen, dashed = false)
                    drawSessionSeries(sortedB, WarningAmber, dashed = true)

                    selectedCompareElapsedMs?.let { elapsed ->
                        val selX = ((elapsed - visibleStart).toFloat() / visibleSpan.toFloat()) * width
                        if (selX in 0f..width) {
                            drawLine(
                                color = ChargingCyan,
                                start = Offset(selX, 0f),
                                end = Offset(selX, graphHeight),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }
                    }
                } else {
                    if (sortedSamples.size < 2) {
                        val single = sortedSamples.first()
                        val y = graphHeight - ((single.levelPercent.toFloat() / 100f) * graphHeight)
                        drawCircle(ElectricGreen, radius = 6.dp.toPx(), center = Offset(width / 2f, y))
                        return@Canvas
                    }

                    val points = sortedSamples.map { sample ->
                        val normX = ((sample.timestamp - visibleStart).toFloat() / visibleSpan.toFloat()) * width
                        val normY = graphHeight - ((sample.levelPercent.toFloat().coerceIn(0f, 100f) / 100f) * graphHeight)
                        Offset(normX, normY)
                    }

                    val linePath = Path()
                    val fillPath = Path()

                    linePath.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, graphHeight)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        val midX = (p0.x + p1.x) / 2f
                        linePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, graphHeight)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ElectricGreen.copy(alpha = 0.35f),
                                ChargingCyan.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = graphHeight
                        )
                    )

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(listOf(ChargingCyan, ElectricGreen)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    selectedSample?.let { sel ->
                        val selX = ((sel.timestamp - visibleStart).toFloat() / visibleSpan.toFloat()) * width
                        val selY = graphHeight - ((sel.levelPercent.toFloat() / 100f) * graphHeight)
                        if (selX in -10f..(width + 10f)) {
                            drawLine(
                                color = ChargingCyan,
                                start = Offset(selX, 0f),
                                end = Offset(selX, graphHeight),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                            drawCircle(
                                color = ElectricGreen.copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = Offset(selX, selY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(selX, selY)
                            )
                        }
                    }
                }
            }
        }

        // Zeitfenster-Anzeige unterhalb der X-Achse
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isCompareMode) formatElapsed(visibleStart) else timeFormatter.format(Date(visibleStart)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isCompareMode) formatElapsed(visibleEnd) else timeFormatter.format(Date(visibleEnd)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
