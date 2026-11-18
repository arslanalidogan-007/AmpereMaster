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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BatteryHealthSnapshotEntity
import com.example.data.model.BatteryHealthStatus
import com.example.data.model.BatteryMetrics
import com.example.localization.LocalAppStrings
import com.example.sensor.DeviceInfo
import com.example.ui.components.SohHistoryCanvas
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

/**
 * Erweiterter Akkugesundheits- und Diagnosebereich für technisch interessierte Nutzer.
 */
@Composable
fun HealthScreen(
    metrics: BatteryMetrics,
    deviceInfo: DeviceInfo,
    designCapacityMah: Int = 5000,
    healthSnapshots: List<BatteryHealthSnapshotEntity> = emptyList(),
    onSetDesignCapacity: (Int) -> Unit = {},
    onRecordSnapshot: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    val (healthLabel, healthColor, healthIcon) = when (metrics.health) {
        BatteryHealthStatus.GOOD -> Triple(strings.healthGood, ElectricGreen, Icons.Default.CheckCircle)
        BatteryHealthStatus.OVERHEAT -> Triple(strings.healthOverheat, AlertRed, Icons.Default.BatteryAlert)
        BatteryHealthStatus.OVER_VOLTAGE -> Triple(strings.healthOverVoltage, AlertRed, Icons.Default.BatteryAlert)
        BatteryHealthStatus.COLD -> Triple(strings.healthCold, ChargingCyan, Icons.Default.Thermostat)
        BatteryHealthStatus.DEAD -> Triple(strings.healthDead, AlertRed, Icons.Default.BatteryAlert)
        else -> Triple(strings.healthUnknown, WarningAmber, Icons.Default.Info)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("health_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kopfzeile
        Text(
            text = strings.healthTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Große Gesundheits-Zustandskarte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, healthColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = strings.healthStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = healthLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = healthColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.healthSafeOperatingDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(healthColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = healthIcon,
                        contentDescription = null,
                        tint = healthColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // v1.0.3: State of Health (SOH) Alterungstracker & Kapazitäts-Historie
        SohHistoryCanvas(
            metrics = metrics,
            designCapacityMah = designCapacityMah,
            snapshots = healthSnapshots,
            onSetDesignCapacity = onSetDesignCapacity,
            onRecordSnapshot = onRecordSnapshot
        )

        // Detail-Metriken (Kapazität, Zyklen, Temperatur)
        DiagnosticCard(
            title = strings.batteryCapacity,
            value = "${metrics.capacityMah} mAh",
            subtitle = strings.batteryCapacitySubtitle,
            icon = Icons.Default.Favorite,
            accentColor = ChargingCyan
        )

        DiagnosticCard(
            title = strings.chargeCycles,
            value = if (metrics.cycleCount >= 0) "${metrics.cycleCount} ${strings.cyclesUnit}" else strings.cycleNotSupported,
            subtitle = strings.chargeCyclesSubtitle,
            icon = Icons.Default.Loop,
            accentColor = ElectricGreen
        )

        // Thermische Parameter
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
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.thermalDataTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.currentTempLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format(Locale.US, "%.1f °C (%.1f °F)", metrics.temperatureCelsius, metrics.temperatureFahrenheit),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.recommendedChargeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("15 °C – 35 °C", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = ElectricGreen)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.criticalTempLimit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("> 45 °C", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = AlertRed)
                }
            }
        }

        // v1.0.2: Akkuschonungs-Rechner & Zyklenersparnis (Cycle Saving Guard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, ElectricGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = strings.cycleSavingTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = strings.cycleSavingDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = strings.cycleSavingEstimate,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
