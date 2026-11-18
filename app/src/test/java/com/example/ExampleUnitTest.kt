package com.example

import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.sensor.ExponentialFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun exponentialFilter_smoothesNoisyInputs() {
        val filter = ExponentialFilter(alpha = 0.25)

        // Initiale Messung
        val first = filter.filter(1000.0)
        assertEquals(1000.0, first, 0.001)

        // Rauschende Messung (z.B. Kernel-Sprung)
        val second = filter.filter(2000.0)
        // Sollte zwischen 1000 und 2000 liegen
        assertTrue(second > 1000.0 && second < 2000.0)
    }

    @Test
    fun smartUnitConversion_detectsSamsungMilliAmpsAndAospMicroAmps() {
        fun convert(raw: Long, isCharging: Boolean): Double {
            val absRaw = Math.abs(raw).toDouble()
            if (absRaw == 0.0) return 0.0
            val ma = when {
                absRaw >= 15_000.0 -> absRaw / 1000.0
                absRaw in 50.0..14_999.0 -> absRaw
                absRaw in 1.0..49.0 -> absRaw * 1000.0
                else -> absRaw
            }
            return if (isCharging) Math.abs(ma) else -Math.abs(ma)
        }

        // Samsung Galaxy liefert 3000 (mA) -> darf NICHT mehr durch 1000 zu 3 mA werden!
        val samsungMa = convert(3000L, true)
        assertEquals(3000.0, samsungMa, 0.001)

        // Google Pixel / AOSP liefert 3.000.000 (µA) -> 3000 mA
        val aospMa = convert(3_000_000L, true)
        assertEquals(3000.0, aospMa, 0.001)

        // USB-PD Leistung bei 9.3 V und 3.0 A entspricht ca. 27.9 W (~ 28 W wie auf dem Kabel)
        val powerWatts = 9.3 * (samsungMa / 1000.0)
        assertEquals(27.9, powerWatts, 0.01)
    }

    @Test
    fun allTenLanguages_haveValidStrings() {
        assertEquals(10, AppLanguage.entries.size)

        AppLanguage.entries.forEach { lang ->
            val strings = AppStrings.get(lang)
            assertNotNull(strings)
            assertEquals("AmpereMaster", strings.appName)
            assertTrue(strings.tabDashboard.isNotBlank())
            assertTrue(strings.tabGraph.isNotBlank())
            assertTrue(strings.tabHealth.isNotBlank())
            assertTrue(strings.tabSettings.isNotBlank())
            assertTrue(strings.developerName.contains("Ali Dogan Arslan"))
            assertTrue(strings.developerEmail.contains("arslanalidogan@gmail.com"))
            assertTrue(strings.developerDescription.isNotBlank())
            assertTrue(strings.contactDeveloper.isNotBlank())
            assertTrue(strings.sendEmailChooser.isNotBlank())
            assertTrue(strings.emailSubject.isNotBlank())
            assertTrue(strings.overviewConceptTitle.isNotBlank())
            assertTrue(strings.conceptWhatItDoesHeading.isNotBlank())
            assertTrue(strings.conceptWhatItDoesBody.isNotBlank())
            assertTrue(strings.conceptTargetAudienceHeading.isNotBlank())
            assertTrue(strings.conceptTargetAudienceBody.isNotBlank())
            assertTrue(strings.conceptCoreValueHeading.isNotBlank())
            assertTrue(strings.conceptCoreValueBody.isNotBlank())
        }
    }

    @Test
    fun arabicLanguage_isMarkedAsRtl() {
        assertTrue(AppLanguage.AR.isRtl)
        assertEquals(false, AppLanguage.DE.isRtl)
        assertEquals(false, AppLanguage.EN.isRtl)
    }

    @Test
    fun batteryPercentage_strictlyAnchoredToOsInteger() {
        val baseLevel = 64
        var fractionalOffset = 0.42

        // Simuliere Berechnung
        val computed = Math.round((baseLevel.toDouble() + fractionalOffset) * 100.0) / 100.0
        assertEquals(64.42, computed, 0.001)
        assertEquals(baseLevel, Math.floor(computed).toInt())
    }

    @Test
    fun discharging_neverIncrementsDecimalPlaces() {
        var fractionalOffset = 0.75
        val isCharging = false

        // Simuliere Eingang von 10 Ticks mit schwankenden Sensorströmen während des Entladens
        val deltaPercents = listOf(-0.002, 0.001, -0.003, 0.004, -0.001) // auch positive Rauschwerte

        for (delta in deltaPercents) {
            val deltaToApply = if (isCharging) {
                Math.max(0.0, delta)
            } else {
                Math.min(0.0, delta) // Streng <= 0 beim Entladen!
            }
            fractionalOffset = (fractionalOffset + deltaToApply).coerceIn(0.0, 0.99)
        }

        // Offset darf NIEMALS über den Ausgangswert von 0.75 gestiegen sein
        assertTrue(fractionalOffset <= 0.75)
        assertEquals(0.744, fractionalOffset, 0.001)
    }

    @Test
    fun allLanguages_versionIs104() {
        AppLanguage.entries.forEach { lang ->
            val strings = AppStrings.get(lang)
            assertTrue(strings.versionTitle.contains("1.0.4"))
            assertTrue(strings.chargerQualityTitle.isNotBlank())
            assertTrue(strings.sohTitle.isNotBlank())
            assertTrue(strings.compareMode.isNotBlank())
            assertTrue(strings.zoomReset.isNotBlank())
            assertTrue(strings.savedChargersTitle.isNotBlank())
            assertTrue(strings.dualCellTitle.isNotBlank())
            assertTrue(strings.widgetSectionTitle.isNotBlank())
            assertTrue(strings.statsSeriesTitle.isNotBlank())
        }
    }

    @Test
    fun csvExportUtil_buildsValidCsvHeaderAndRows() {
        val sample = com.example.data.local.entity.BatterySampleEntity(
            sessionId = 42L,
            timestamp = 1700000000000L,
            levelPercent = 72.45,
            currentMilliAmp = 2850.0,
            voltageVolt = 4.21,
            powerWatts = 24.5,
            temperatureCelsius = 34.2f,
            isCharging = true
        )
        val csv = com.example.util.CsvExportUtil.buildCsvContent(listOf(sample))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("Timestamp,DateTime,LevelPercent"))
        assertTrue(lines[1].contains("72.45"))
        assertTrue(lines[1].contains("2850.0"))
        assertTrue(lines[1].contains("true"))
    }

    @Test
    fun dualCellMode_hasExpectedEntries() {
        val modes = com.example.data.model.DualCellMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(com.example.data.model.DualCellMode.AUTO))
        assertTrue(modes.contains(com.example.data.model.DualCellMode.SINGLE_CELL))
        assertTrue(modes.contains(com.example.data.model.DualCellMode.DUAL_CELL))
    }

    @Test
    fun evaluateChargerQuality_gradesFastAcAndSlowUsbCorrectly() {
        val fastResult = com.example.data.repository.BatteryRepository.evaluateChargerQuality(
            isCharging = true,
            source = com.example.data.model.ChargingSource.AC,
            currentMa = 3000.0,
            voltageVolt = 4.2,
            chargerVoltageVolt = 9.0,
            powerWatts = 27.0,
            cellPowerWatts = 25.0
        )
        assertTrue(fastResult.first >= 90)
        assertEquals("A+", fastResult.second)
        assertTrue(fastResult.third in 85..99)

        val slowUsbResult = com.example.data.repository.BatteryRepository.evaluateChargerQuality(
            isCharging = true,
            source = com.example.data.model.ChargingSource.USB,
            currentMa = 450.0,
            voltageVolt = 3.9,
            chargerVoltageVolt = 5.0,
            powerWatts = 2.5,
            cellPowerWatts = 1.75
        )
        assertTrue(slowUsbResult.first < fastResult.first)
        assertTrue(slowUsbResult.second in listOf("B", "C", "D"))

        val unpluggedResult = com.example.data.repository.BatteryRepository.evaluateChargerQuality(
            isCharging = false,
            source = com.example.data.model.ChargingSource.NONE,
            currentMa = -400.0,
            voltageVolt = 3.8,
            chargerVoltageVolt = 0.0,
            powerWatts = 1.5,
            cellPowerWatts = 1.5
        )
        assertEquals(0, unpluggedResult.first)
        assertEquals("–", unpluggedResult.second)
    }

    @Test
    fun scoreToGrade_mapsAllThresholds() {
        assertEquals("A+", com.example.data.repository.BatteryRepository.scoreToGrade(95))
        assertEquals("A", com.example.data.repository.BatteryRepository.scoreToGrade(82))
        assertEquals("B", com.example.data.repository.BatteryRepository.scoreToGrade(68))
        assertEquals("C", com.example.data.repository.BatteryRepository.scoreToGrade(50))
        assertEquals("D", com.example.data.repository.BatteryRepository.scoreToGrade(30))
        assertEquals("–", com.example.data.repository.BatteryRepository.scoreToGrade(0))
    }
}
