package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.LocalAppStrings
import com.example.localization.ProvideAppLocalization
import com.example.service.BatteryTrackingService
import com.example.ui.components.ChangelogDialog
import com.example.ui.components.OverviewConceptDialog
import com.example.ui.components.SessionSummaryDialog
import com.example.ui.components.SupportDialog
import com.example.ui.screens.ChargingGraphScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmpereMasterTheme
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.viewmodel.AmpereViewModel
import com.example.util.CsvExportUtil

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vollständiges Edge-to-Edge Design aktivieren
        enableEdgeToEdge()

        setContent {
            val viewModel: AmpereViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.currentLanguage.collectAsStateWithLifecycle()

            // Benachrichtigungs-Berechtigung für Android 13+ (POST_NOTIFICATIONS)
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted && viewModel.backgroundServiceEnabled.value && viewModel.metrics.value.isCharging) {
                    BatteryTrackingService.start(context)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Benachrichtigungsabfrage abgefangen", e)
                    }
                }
            }

            AmpereMasterTheme(themeMode = themeMode) {
                ProvideAppLocalization(language = language) {
                    AmpereMasterApp(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Haupt-App-Container mit adaptivem Smartphone/Tablet-Layout (NavigationBar vs. NavigationRail),
 * dynamischen System-Insets und persistenter 4-Tab-Navigation.
 */
@Composable
fun AmpereMasterApp(viewModel: AmpereViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val targetLevel by viewModel.targetChargeLevel.collectAsStateWithLifecycle()
    val samples by viewModel.latestSamples.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val soundAlert by viewModel.soundAlertEnabled.collectAsStateWithLifecycle()
    val vibrationAlert by viewModel.vibrationAlertEnabled.collectAsStateWithLifecycle()
    val bgService by viewModel.backgroundServiceEnabled.collectAsStateWithLifecycle()
    val tempAlertEnabled by viewModel.tempAlertEnabled.collectAsStateWithLifecycle()
    val tempAlertThreshold by viewModel.tempAlertThreshold.collectAsStateWithLifecycle()
    val lastSessionSummary by viewModel.lastSessionSummary.collectAsStateWithLifecycle()

    // v1.0.3 States
    val benchmarkState by viewModel.benchmarkState.collectAsStateWithLifecycle()
    val designCapacityMah by viewModel.designCapacityMah.collectAsStateWithLifecycle()
    val healthSnapshots by viewModel.healthSnapshots.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val isCompareMode by viewModel.isCompareMode.collectAsStateWithLifecycle()
    val compareSessionA by viewModel.compareSessionA.collectAsStateWithLifecycle()
    val compareSessionB by viewModel.compareSessionB.collectAsStateWithLifecycle()
    val compareSamplesA by viewModel.compareSamplesA.collectAsStateWithLifecycle()
    val compareSamplesB by viewModel.compareSamplesB.collectAsStateWithLifecycle()

    // v1.0.4 States
    val dualCellMode by viewModel.dualCellMode.collectAsStateWithLifecycle()
    val widgetDetailedMode by viewModel.widgetDetailedMode.collectAsStateWithLifecycle()
    val savedChargers by viewModel.savedChargers.collectAsStateWithLifecycle()
    val activeChargerProfile by viewModel.activeChargerProfile.collectAsStateWithLifecycle()

    val showChangelog by viewModel.showChangelog.collectAsStateWithLifecycle()
    val showSupport by viewModel.showSupport.collectAsStateWithLifecycle()
    val showOverviewConcept by viewModel.showOverviewConcept.collectAsStateWithLifecycle()
    val showClearConfirm by viewModel.showClearConfirm.collectAsStateWithLifecycle()

    // Android System BackHandler: Wenn in Unterseite, zurück zur Hauptübersicht
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Bolt else Icons.Outlined.Bolt,
                                    contentDescription = strings.tabDashboard
                                )
                            },
                            label = { Text(strings.tabDashboard) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricGreen,
                                selectedTextColor = ElectricGreen,
                                indicatorColor = ElectricGreen.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Default.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = strings.tabGraph
                                )
                            },
                            label = { Text(strings.tabGraph) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ChargingCyan,
                                selectedTextColor = ChargingCyan,
                                indicatorColor = ChargingCyan.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_tab_graph")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Default.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                    contentDescription = strings.tabHealth
                                )
                            },
                            label = { Text(strings.tabHealth) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricGreen,
                                selectedTextColor = ElectricGreen,
                                indicatorColor = ElectricGreen.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_tab_health")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                    contentDescription = strings.tabSettings
                                )
                            },
                            label = { Text(strings.tabSettings) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ChargingCyan,
                                selectedTextColor = ChargingCyan,
                                indicatorColor = ChargingCyan.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("tablet_nav_rail"),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Bolt else Icons.Outlined.Bolt,
                                    contentDescription = strings.tabDashboard
                                )
                            },
                            label = { Text(strings.tabDashboard) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ElectricGreen,
                                selectedTextColor = ElectricGreen,
                                indicatorColor = ElectricGreen.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_rail_dashboard")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Default.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = strings.tabGraph
                                )
                            },
                            label = { Text(strings.tabGraph) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ChargingCyan,
                                selectedTextColor = ChargingCyan,
                                indicatorColor = ChargingCyan.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_rail_graph")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Default.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                    contentDescription = strings.tabHealth
                                )
                            },
                            label = { Text(strings.tabHealth) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ElectricGreen,
                                selectedTextColor = ElectricGreen,
                                indicatorColor = ElectricGreen.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_rail_health")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                    contentDescription = strings.tabSettings
                                )
                            },
                            label = { Text(strings.tabSettings) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ChargingCyan,
                                selectedTextColor = ChargingCyan,
                                indicatorColor = ChargingCyan.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_rail_settings")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1080.dp)
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                metrics = metrics,
                                targetLevel = targetLevel,
                                samples = samples,
                                deviceInfo = viewModel.deviceInfo,
                                benchmarkState = benchmarkState,
                                savedChargers = savedChargers,
                                activeChargerProfile = activeChargerProfile,
                                onStartBenchmark = { viewModel.startChargerBenchmark() },
                                onStopBenchmark = { viewModel.stopChargerBenchmark() },
                                onSaveChargerProfile = { name, watts -> viewModel.saveChargerProfile(name, watts) },
                                onAssignChargerProfile = { name -> viewModel.assignActiveChargerProfile(name) },
                                onDeleteSavedCharger = { id -> viewModel.deleteSavedCharger(id) },
                                onOpenConcept = { viewModel.setShowOverviewConcept(true) }
                            )
                            1 -> ChargingGraphScreen(
                                samples = samples,
                                allSessions = allSessions,
                                isCompareMode = isCompareMode,
                                compareSessionA = compareSessionA,
                                compareSessionB = compareSessionB,
                                compareSamplesA = compareSamplesA,
                                compareSamplesB = compareSamplesB,
                                onSetCompareMode = { viewModel.setCompareMode(it) },
                                onSelectSessionA = { viewModel.selectCompareSessionA(it) },
                                onSelectSessionB = { viewModel.selectCompareSessionB(it) },
                                showClearConfirm = showClearConfirm,
                                onShowClearConfirm = { viewModel.setShowClearConfirm(it) },
                                onClearHistory = { viewModel.clearHistory() },
                                onCsvExport = { CsvExportUtil.exportSamplesToCsv(context, samples, language) }
                            )
                            2 -> HealthScreen(
                                metrics = metrics,
                                deviceInfo = viewModel.deviceInfo,
                                designCapacityMah = designCapacityMah,
                                healthSnapshots = healthSnapshots,
                                onSetDesignCapacity = { viewModel.setDesignCapacityMah(it) },
                                onRecordSnapshot = { viewModel.recordHealthSnapshotNow() }
                            )
                            3 -> SettingsScreen(
                                currentTheme = themeMode,
                                currentLanguage = language,
                                targetChargeLevel = targetLevel,
                                soundAlertEnabled = soundAlert,
                                vibrationAlertEnabled = vibrationAlert,
                                backgroundServiceEnabled = bgService,
                                tempAlertEnabled = tempAlertEnabled,
                                tempAlertThreshold = tempAlertThreshold,
                                dualCellMode = dualCellMode,
                                widgetDetailedMode = widgetDetailedMode,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onLanguageChange = { viewModel.setLanguage(it) },
                                onTargetLevelChange = { viewModel.setTargetChargeLevel(it) },
                                onSoundAlertChange = { viewModel.setSoundAlertEnabled(it) },
                                onVibrationAlertChange = { viewModel.setVibrationAlertEnabled(it) },
                                onBackgroundServiceChange = { viewModel.setBackgroundServiceEnabled(it) },
                                onTempAlertChange = { viewModel.setTempAlertEnabled(it) },
                                onTempAlertThresholdChange = { viewModel.setTempAlertThreshold(it) },
                                onDualCellModeChange = { viewModel.setDualCellMode(it) },
                                onWidgetDetailedModeChange = { viewModel.setWidgetDetailedMode(it) },
                                onCsvExport = { CsvExportUtil.exportSamplesToCsv(context, samples, language) },
                                onOpenChangelog = { viewModel.setShowChangelog(true) },
                                onOpenSupport = { viewModel.setShowSupport(true) },
                                onOpenOverviewConcept = { viewModel.setShowOverviewConcept(true) }
                            )
                        }
                    }
                }
            }

            // Zusatzdialoge
            lastSessionSummary?.let { summary ->
                SessionSummaryDialog(
                    summary = summary,
                    onDismiss = { viewModel.dismissSessionSummary() },
                    onViewGraph = {
                        viewModel.dismissSessionSummary()
                        selectedTab = 1
                    }
                )
            }

            if (showOverviewConcept) {
                OverviewConceptDialog(onDismiss = { viewModel.setShowOverviewConcept(false) })
            }

            if (showChangelog) {
                ChangelogDialog(onDismiss = { viewModel.setShowChangelog(false) })
            }

            if (showSupport) {
                SupportDialog(onDismiss = { viewModel.setShowSupport(false) })
            }
        }
    }
}
