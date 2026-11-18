package com.example.sensor

import android.os.Build

/**
 * Informationsmodell über das physische Gerät und Herstellereigenschaften.
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val androidVersion: String,
    val apiLevel: Int,
    val brandDisplayName: String,
    val isSamsung: Boolean,
    val isXiaomi: Boolean,
    val isGooglePixel: Boolean,
    val hasNotchOrCutoutExpected: Boolean
)

/**
 * Ermittelt Hardware- und Hersteller-Informationen für optimierte Anzeige und Insets-Sicherheit.
 */
object DeviceInspector {

    fun inspect(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        val deviceCode = Build.DEVICE.orEmpty()
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE.orEmpty()

        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val isXiaomi = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("redmi", ignoreCase = true) ||
                Build.BRAND.equals("poco", ignoreCase = true)
        val isGooglePixel = Build.MANUFACTURER.equals("google", ignoreCase = true) ||
                Build.BRAND.equals("google", ignoreCase = true)

        val brandDisplayName = when {
            isSamsung -> "Samsung Galaxy ($model)"
            isXiaomi -> "Xiaomi / Redmi ($model)"
            isGooglePixel -> "Google Pixel ($model)"
            else -> "$manufacturer $model"
        }

        // Bei modernen Geräten ab API 28 (Android 9 Pie) sind Display-Notches/Kamerainseln üblich
        val hasNotchOrCutoutExpected = apiLevel >= Build.VERSION_CODES.P

        return DeviceInfo(
            manufacturer = manufacturer,
            model = model,
            deviceCode = deviceCode,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            brandDisplayName = brandDisplayName,
            isSamsung = isSamsung,
            isXiaomi = isXiaomi,
            isGooglePixel = isGooglePixel,
            hasNotchOrCutoutExpected = hasNotchOrCutoutExpected
        )
    }
}
