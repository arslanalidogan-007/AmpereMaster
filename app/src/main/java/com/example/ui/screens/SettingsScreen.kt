package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import com.example.data.model.DualCellMode
import com.example.localization.AppLanguage
import com.example.localization.LocalAppStrings
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ThemeMode

/**
 * Einstellungen für Erscheinungsbild (Hell/Dunkel/True Black), 10 Sprachen, Ladeziel,
 * Dual-Cell-Kalibrierung, Widget-Anzeige, Überhitzungsschutz & CSV-Export.
 */
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    currentLanguage: AppLanguage,
    targetChargeLevel: Int,
    soundAlertEnabled: Boolean,
    vibrationAlertEnabled: Boolean,
    backgroundServiceEnabled: Boolean,
    tempAlertEnabled: Boolean,
    tempAlertThreshold: Int,
    dualCellMode: DualCellMode = DualCellMode.AUTO,
    widgetDetailedMode: Boolean = true,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onTargetLevelChange: (Int) -> Unit,
    onSoundAlertChange: (Boolean) -> Unit,
    onVibrationAlertChange: (Boolean) -> Unit,
    onBackgroundServiceChange: (Boolean) -> Unit,
    onTempAlertChange: (Boolean) -> Unit,
    onTempAlertThresholdChange: (Int) -> Unit,
    onDualCellModeChange: (DualCellMode) -> Unit = {},
    onWidgetDetailedModeChange: (Boolean) -> Unit = {},
    onCsvExport: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenOverviewConcept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kopfzeile
        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // 1. Erscheinungsbild (Theme Selection)
        SectionCard(title = strings.themeSection, icon = Icons.Default.DarkMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    title = strings.themeLight,
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    title = strings.themeDark,
                    isSelected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    title = "True Black",
                    isSelected = currentTheme == ThemeMode.TRUE_BLACK,
                    onClick = { onThemeChange(ThemeMode.TRUE_BLACK) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (currentTheme == ThemeMode.TRUE_BLACK) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.themeTrueBlackDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricGreen
                )
            }
        }

        // 2. Sprache / Language
        SectionCard(title = strings.languageSection, icon = Icons.Default.Language) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.chunked(2).forEach { rowLanguages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLanguages.forEach { lang ->
                            val isSelected = currentLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) ChargingCyan.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) ChargingCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onLanguageChange(lang) }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = lang.flagEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) ChargingCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Ladeziel-Konfiguration & Alarme
        SectionCard(title = strings.alertSection, icon = Icons.Default.NotificationsActive) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.chargeTargetTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.chargeTargetDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$targetChargeLevel %",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGreen
                    )
                }

                Slider(
                    value = targetChargeLevel.toFloat(),
                    onValueChange = { onTargetLevelChange(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricGreen,
                        activeTrackColor = ElectricGreen,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("target_level_slider")
                )

                // Schnellauswahl-Buttons (80% Akkuschonung vs. 100% Vollladung)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetPresetChip("80 % (${strings.presetPreserve})", targetChargeLevel == 80) { onTargetLevelChange(80) }
                    TargetPresetChip("90 %", targetChargeLevel == 90) { onTargetLevelChange(90) }
                    TargetPresetChip("100 % (${strings.presetFull})", targetChargeLevel == 100) { onTargetLevelChange(100) }
                }

                // Audio Alarm Toggle
                SettingSwitchRow(
                    title = strings.soundAlert,
                    description = strings.soundAlertDesc,
                    icon = Icons.Default.VolumeUp,
                    isChecked = soundAlertEnabled,
                    onCheckedChange = onSoundAlertChange
                )

                // Vibration Alarm Toggle
                SettingSwitchRow(
                    title = strings.vibrationAlert,
                    description = strings.vibrationAlertDesc,
                    icon = Icons.Default.Vibration,
                    isChecked = vibrationAlertEnabled,
                    onCheckedChange = onVibrationAlertChange
                )

                // Background Service Toggle
                SettingSwitchRow(
                    title = strings.backgroundService,
                    description = strings.backgroundServiceDesc,
                    icon = Icons.Default.PhoneAndroid,
                    isChecked = backgroundServiceEnabled,
                    onCheckedChange = onBackgroundServiceChange
                )
            }
        }

        // 4. v1.0.2: Überhitzungsschutz (Thermal Protection)
        SectionCard(title = strings.tempAlertTitle, icon = Icons.Default.Thermostat) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow(
                    title = strings.tempAlertTitle,
                    description = strings.tempAlertDesc,
                    icon = Icons.Default.Thermostat,
                    isChecked = tempAlertEnabled,
                    onCheckedChange = onTempAlertChange
                )

                if (tempAlertEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.tempThreshold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$tempAlertThreshold °C",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ChargingCyan
                            )
                        }

                        Slider(
                            value = tempAlertThreshold.toFloat(),
                            onValueChange = { onTempAlertThresholdChange(it.toInt()) },
                            valueRange = 38f..52f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = ChargingCyan,
                                activeTrackColor = ChargingCyan,
                                inactiveTrackColor = ChargingCyan.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("temp_threshold_slider")
                        )
                    }
                }
            }
        }

        // 5. v1.0.4: Dual-Cell Kalibrierung (2S)
        SectionCard(title = strings.dualCellTitle, icon = Icons.Default.BatteryChargingFull) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.dualCellDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        title = strings.dualCellAuto,
                        isSelected = dualCellMode == DualCellMode.AUTO,
                        onClick = { onDualCellModeChange(DualCellMode.AUTO) },
                        modifier = Modifier.weight(1f).testTag("dual_cell_auto")
                    )
                    ThemeOptionButton(
                        title = strings.dualCellSingle,
                        isSelected = dualCellMode == DualCellMode.SINGLE_CELL,
                        onClick = { onDualCellModeChange(DualCellMode.SINGLE_CELL) },
                        modifier = Modifier.weight(1f).testTag("dual_cell_single")
                    )
                    ThemeOptionButton(
                        title = strings.dualCellDouble,
                        isSelected = dualCellMode == DualCellMode.DUAL_CELL,
                        onClick = { onDualCellModeChange(DualCellMode.DUAL_CELL) },
                        modifier = Modifier.weight(1f).testTag("dual_cell_double")
                    )
                }
            }
        }

        // 6. v1.0.4: Home-Screen Widget Konfiguration
        SectionCard(title = strings.widgetSectionTitle, icon = Icons.Default.Widgets) {
            SettingSwitchRow(
                title = strings.widgetDetailedLabel,
                description = strings.widgetDetailedDesc,
                icon = Icons.Default.Widgets,
                isChecked = widgetDetailedMode,
                onCheckedChange = onWidgetDetailedModeChange
            )
        }

        // 7. v1.0.2/v1.0.4: CSV-Datenexport (FileProvider)
        SectionCard(title = strings.csvExportTitle, icon = Icons.Default.FileDownload) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.csvExportDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MenuNavigationRow(
                    title = strings.csvExportTitle,
                    icon = Icons.Default.FileDownload,
                    onClick = onCsvExport
                )
            }
        }

        // 8. Zusatzmenüs: Changelog, Konzept & Support
        SectionCard(title = strings.supportSection, icon = Icons.Default.SupportAgent) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuNavigationRow(
                    title = strings.overviewConceptTitle,
                    icon = Icons.Default.Info,
                    onClick = onOpenOverviewConcept
                )

                MenuNavigationRow(
                    title = strings.changelogTitle,
                    icon = Icons.Default.History,
                    onClick = onOpenChangelog
                )

                MenuNavigationRow(
                    title = "${strings.developerTitle} (Ali Dogan Arslan)",
                    icon = Icons.Default.SupportAgent,
                    onClick = onOpenSupport
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ChargingCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ThemeOptionButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ChargingCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isSelected) ChargingCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) ChargingCyan else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TargetPresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ElectricGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isSelected) ElectricGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) ElectricGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) ElectricGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricGreen
            )
        )
    }
}

@Composable
private fun MenuNavigationRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ChargingCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
