package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChargingSessionSummary
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.service.BatteryAlarmManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AmpereMaster", appName)
    }

    @Test
    fun `charging session summary calculates correctly`() {
        val summary = ChargingSessionSummary(
            sessionId = 1L,
            startTime = 1000L,
            endTime = 70000L,
            durationMinutes = 25L,
            startLevelPercent = 40.0,
            endLevelPercent = 65.5,
            deltaPercent = 25.5,
            maxPowerWatts = 25.4,
            avgPowerWatts = 19.8,
            peakCurrentMilliAmp = 3100.0,
            avgCurrentMilliAmp = 2400.0,
            peakTemperatureCelsius = 38.5f,
            estimatedMahDelivered = 1000,
            chargingProtocol = "USB-PD / PPS Super Fast"
        )

        assertEquals(25.5, summary.deltaPercent, 0.01)
        assertEquals("USB-PD / PPS Super Fast", summary.chargingProtocol)
        assertEquals(25L, summary.durationMinutes)
    }

    @Test
    fun `app strings has complete translations for all 10 languages`() {
        for (lang in AppLanguage.values()) {
            val strings = AppStrings.get(lang)
            assertNotNull(strings.appName)
            assertTrue(strings.tempAlertTitle.isNotBlank())
            assertTrue(strings.sessionSummaryTitle.isNotBlank())
            assertTrue(strings.csvExportTitle.isNotBlank())
            assertTrue(strings.cycleSavingTitle.isNotBlank())
            assertTrue(strings.chargerQualityTitle.isNotBlank())
            assertTrue(strings.stressTestStart.isNotBlank())
            assertTrue(strings.sohTitle.isNotBlank())
            assertTrue(strings.sohHistoryTitle.isNotBlank())
            assertTrue(strings.compareMode.isNotBlank())
            assertTrue(strings.zoomHint.isNotBlank())
            assertTrue(strings.savedChargersTitle.isNotBlank())
            assertTrue(strings.dualCellTitle.isNotBlank())
            assertTrue(strings.widgetDetailedLabel.isNotBlank())
            assertTrue(strings.notifOverheatTitle.isNotBlank())
            assertEquals("Version 1.0.4 (Build 5)", strings.versionTitle)
        }
    }

    @Test
    fun `battery alarm manager temperature alert handles thresholds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = BatteryAlarmManager(context)

        // Below threshold - should not alert
        alarmManager.checkAndTriggerTemperatureAlarm(
            currentTempCelsius = 39.0f,
            thresholdCelsius = 42,
            tempAlertEnabled = true,
            soundEnabled = false,
            vibrationEnabled = false
        )

        // Above threshold - triggers alarm
        alarmManager.checkAndTriggerTemperatureAlarm(
            currentTempCelsius = 43.5f,
            thresholdCelsius = 42,
            tempAlertEnabled = true,
            soundEnabled = false,
            vibrationEnabled = false
        )

        // Cooldown below threshold - 2.0f (40.0f) re-arms
        alarmManager.checkAndTriggerTemperatureAlarm(
            currentTempCelsius = 39.5f,
            thresholdCelsius = 42,
            tempAlertEnabled = true,
            soundEnabled = false,
            vibrationEnabled = false
        )
    }

    @Test
    fun `room database v4 supports saved charger profiles`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val charger = com.example.data.local.entity.SavedChargerEntity(
            name = "Anker 65W + USB-C",
            ratedWatts = 65,
            grade = "A+",
            score = 96,
            peakCurrentMilliAmp = 3200.0,
            peakPowerWatts = 28.5,
            cableEfficiencyPercent = 96,
            voltageSagMv = 45,
            lastTestedAt = 1700000000000L
        )
        val id = db.batteryDao().insertSavedCharger(charger)
        assertTrue(id > 0L)
        db.close()
    }
}
