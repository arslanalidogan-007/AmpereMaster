package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Verantwortlich für das Auslösen von akustischen und haptischen Alarmen bei Erreichen des Ladeziels.
 */
class BatteryAlarmManager(private val context: Context) {

    private var hasAlertedForCurrentSession = false
    private var lastTargetLevel = -1
    private var hasAlertedTemp = false

    /**
     * Prüft, ob der aktuelle Akkustand das konfigurierte Ladeziel erreicht hat und löst ggf. den Alarm aus.
     */
    fun checkAndTriggerAlarm(
        currentLevel: Double,
        targetLevel: Int,
        isCharging: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (!isCharging) {
            // Zurücksetzen, wenn das Gerät abgesteckt wird
            hasAlertedForCurrentSession = false
            return
        }

        if (targetLevel != lastTargetLevel) {
            lastTargetLevel = targetLevel
            hasAlertedForCurrentSession = false
        }

        // Wenn Ladeziel erreicht oder überschritten und in dieser Sitzung noch nicht alarmiert wurde
        if (currentLevel >= targetLevel.toDouble() && !hasAlertedForCurrentSession) {
            hasAlertedForCurrentSession = true
            triggerAlarm(soundEnabled, vibrationEnabled)
        }
    }

    /**
     * Prüft, ob die Akkutemperatur den konfigurierten Schwellenwert (z.B. 42°C) überschritten hat.
     */
    fun checkAndTriggerTemperatureAlarm(
        currentTempCelsius: Float,
        thresholdCelsius: Int,
        tempAlertEnabled: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (!tempAlertEnabled) {
            hasAlertedTemp = false
            return
        }

        // Hysterese: Erneut scharf schalten, wenn die Temperatur wieder um 2.0°C unter den Schwellenwert fällt
        if (currentTempCelsius <= thresholdCelsius - 2.0f) {
            hasAlertedTemp = false
        }

        if (currentTempCelsius >= thresholdCelsius && !hasAlertedTemp) {
            hasAlertedTemp = true
            triggerAlarm(soundEnabled, vibrationEnabled)
        }
    }

    private fun triggerAlarm(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        // 1. Akustischer Signalton
        if (soundEnabled) {
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone?.play()
            } catch (e: Exception) {
                Log.w("BatteryAlarm", "Fehler beim Abspielen des Alarmtons", e)
            }
        }

        // 2. Vibrationsmuster (zwei prägnante Impulse: 300ms an, 200ms aus, 400ms an)
        if (vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 300, 200, 400)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 300, 200, 400), -1)
                    }
                }
            } catch (e: Exception) {
                Log.w("BatteryAlarm", "Fehler beim Vibrieren", e)
            }
        }
    }

    fun reset() {
        hasAlertedForCurrentSession = false
    }
}
