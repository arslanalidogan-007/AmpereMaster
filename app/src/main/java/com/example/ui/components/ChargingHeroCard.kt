package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatteryMetrics
import com.example.data.model.ChargingSource
import com.example.localization.LocalAppStrings
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

/**
 * Hero-Komponente mit kreisförmiger Ladeanzeige, exaktem Akkustand auf 2 Nachkommastellen genau
 * und pulsierendem Lade-Indikator.
 */
@Composable
fun ChargingHeroCard(
    metrics: BatteryMetrics,
    targetLevel: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isCharging = metrics.isCharging

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryColor = when {
        !isCharging && metrics.levelPercent <= 20.0 -> WarningAmber
        isCharging -> ElectricGreen
        else -> ChargingCyan
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            .padding(24.dp)
            .testTag("charging_hero_card"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Kreisförmiger Prozent- und Statusmesser
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedAlpha = if (isCharging) glowAlpha else 0.8f

                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val sweepAngle = ((metrics.levelPercent / 100.0) * 360.0).toFloat().coerceIn(0f, 360f)

                    // Hintergrundkreis
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.15f),
                        style = Stroke(width = strokeWidth)
                    )

                    // Ziellinie als Markierung
                    val targetAngle = ((targetLevel / 100.0) * 360.0).toFloat()
                    drawArc(
                        color = WarningAmber.copy(alpha = 0.5f),
                        startAngle = -90f + targetAngle - 2f,
                        sweepAngle = 4f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Aktiver Ladefortschritt mit Farbverlauf
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.7f),
                                primaryColor.copy(alpha = animatedAlpha),
                                primaryColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Zentraler Textinhalt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = strings.charging,
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Exakter Prozentwert mit 2 Nachkommastellen
                    Text(
                        text = String.format(Locale.US, "%.2f", metrics.levelPercent),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status-Pill mit Ladequelle
            val (sourceText, sourceIcon) = when (metrics.chargingSource) {
                ChargingSource.AC -> strings.sourceAc to Icons.Default.Power
                ChargingSource.USB -> strings.sourceUsb to Icons.Default.Usb
                ChargingSource.WIRELESS -> strings.sourceWireless to Icons.Default.Wifi
                ChargingSource.DOCK -> "Dock" to Icons.Default.Power
                ChargingSource.NONE -> (if (isCharging) strings.charging else strings.discharging) to Icons.Default.ElectricBolt
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val chargingLabel = when {
                    !isCharging -> "${strings.discharging} • ${metrics.detectedProtocol}"
                    metrics.isFastCharging -> "$sourceText • ${metrics.detectedProtocol}"
                    else -> "$sourceText • ${strings.charging}"
                }
                Text(
                    text = chargingLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Entladerate oder Restladezeit bis zum Zielwert
            if (isCharging) {
                Spacer(modifier = Modifier.height(10.dp))
                val remainingText = formatRemainingTime(metrics.chargeTimeRemainingMs, targetLevel, strings)
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (metrics.dischargeRatePerHour > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%s: -%.1f %% / h (%s)",
                        strings.dischargeRateTitle,
                        metrics.dischargeRatePerHour,
                        if (metrics.standbyDrainQuality == "Optimal") strings.standbyOptimal else (if (metrics.standbyDrainQuality == "Normal") strings.standbyNormal else strings.standbyHigh)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (metrics.screenOnDischargePerHour > 0.0 || metrics.screenOffDischargePerHour > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%s: %.1f%%/h (%d%%) • %s: %.1f%%/h",
                            strings.screenOnLabel,
                            metrics.screenOnDischargePerHour,
                            metrics.screenOnDrainSharePercent,
                            strings.screenOffLabel,
                            metrics.screenOffDischargePerHour
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChargingCyan
                    )
                }
            }
        }
    }
}

private fun formatRemainingTime(
    millis: Long,
    targetLevel: Int,
    strings: com.example.localization.StringsModel
): String {
    if (millis < 0) return strings.calculatingTime
    if (millis == 0L) return "${strings.metricTargetLimit} ($targetLevel%) ${strings.targetReachedLabel}"

    val totalMinutes = (millis / (1000 * 60)).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        "${strings.metricRemainingTime} ($targetLevel%): $hours ${strings.timeHoursShort} $minutes ${strings.timeMinutesShort}"
    } else {
        "${strings.metricRemainingTime} ($targetLevel%): $minutes ${strings.timeMinutesShort}"
    }
}
