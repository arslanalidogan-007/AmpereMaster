package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.AmpereApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.BatteryMetrics
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.StringsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Foreground Service zur kontinuierlichen Erfassung von Ladedaten und Aktualisierung
 * der Statusleisten-Benachrichtigung auch bei ausgeschaltetem Bildschirm.
 */
class BatteryTrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var alarmManager: BatteryAlarmManager
    private lateinit var notificationManager: NotificationManager
    private var wasChargingInService: Boolean = false

    override fun onCreate() {
        super.onCreate()
        alarmManager = BatteryAlarmManager(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? AmpereApplication
        val repository = app?.repository
        val strings = AppStrings.get(repository?.currentLanguage?.value ?: AppLanguage.DE)

        val initialNotification = buildNotification(strings.appName, strings.calculatingTime, 0.0)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            android.util.Log.e("BatteryService", "startForeground konnte nicht ausgeführt werden", e)
        }

        if (repository != null) {
            serviceScope.launch {
                repository.batteryMetrics.collectLatest { metrics ->
                    val currentStrings = AppStrings.get(repository.currentLanguage.value)
                    updateNotification(metrics, currentStrings)

                    if (metrics.isCharging) {
                        wasChargingInService = true
                    }

                    // Alarmierung prüfen (Ladeziel)
                    alarmManager.checkAndTriggerAlarm(
                        currentLevel = metrics.levelPercent,
                        targetLevel = repository.targetChargeLevel.value,
                        isCharging = metrics.isCharging,
                        soundEnabled = repository.soundAlertEnabled.value,
                        vibrationEnabled = repository.vibrationAlertEnabled.value
                    )

                    // Überhitzungsschutz prüfen
                    alarmManager.checkAndTriggerTemperatureAlarm(
                        currentTempCelsius = metrics.temperatureCelsius,
                        thresholdCelsius = repository.tempAlertThreshold.value,
                        tempAlertEnabled = repository.tempAlertEnabled.value,
                        soundEnabled = repository.soundAlertEnabled.value,
                        vibrationEnabled = repository.vibrationAlertEnabled.value
                    )

                    if (repository.tempAlertEnabled.value && metrics.temperatureCelsius >= repository.tempAlertThreshold.value) {
                        showOverheatNotification(metrics.temperatureCelsius, repository.tempAlertThreshold.value, currentStrings)
                    }

                    // Nur beim echten Übergang Laden -> Abgesteckt beenden
                    if (!metrics.isCharging) {
                        if (wasChargingInService) {
                            wasChargingInService = false
                            showSessionCompletedNotification(metrics, currentStrings)
                        }
                        stopSelf()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun showOverheatNotification(temp: Float, threshold: Int, strings: StringsModel) {
        val notif = NotificationCompat.Builder(this, CHANNEL_TEMP_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.notifOverheatTitle)
            .setContentText(String.format(Locale.US, "%.1f °C (%s: %d °C)", temp, strings.tempThreshold, threshold))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_TEMP_ALERT_ID, notif)
    }

    private fun showSessionCompletedNotification(metrics: BatteryMetrics, strings: StringsModel) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(strings.notifSessionDoneTitle)
            .setContentText(String.format(Locale.US, "%.2f %% • %s", metrics.levelPercent, metrics.detectedProtocol))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_SUMMARY_ID, notif)
    }

    private fun updateNotification(metrics: BatteryMetrics, strings: StringsModel) {
        val statusWord = if (metrics.isCharging) strings.charging else strings.discharging
        val title = String.format(Locale.US, "%.2f %% • %s", metrics.levelPercent, statusWord)
        val content = String.format(
            Locale.US,
            "%.0f mA • %.2f V • %.1f W • %.1f °C",
            metrics.currentMilliAmp,
            metrics.voltageVolt,
            metrics.powerWatts,
            metrics.temperatureCelsius
        )

        val notification = buildNotification(title, content, metrics.levelPercent)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String, percent: Double): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, percent.toInt(), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelMonitor = NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.battery_monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.battery_monitor_channel_desc)
                setShowBadge(false)
            }

            val channelAlarm = NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.battery_alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.battery_alarm_channel_desc)
                enableVibration(true)
            }

            val channelTemp = NotificationChannel(
                CHANNEL_TEMP_ALARM,
                "AmpereMaster Überhitzungsschutz",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnungen bei Überschreitung des Temperatur-Schwellenwerts"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channelMonitor)
            notificationManager.createNotificationChannel(channelAlarm)
            notificationManager.createNotificationChannel(channelTemp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_TEMP_ALERT_ID = 1002
        const val NOTIFICATION_SUMMARY_ID = 1003
        const val CHANNEL_MONITOR = "amperemaster_monitor"
        const val CHANNEL_ALARM = "amperemaster_alarm"
        const val CHANNEL_TEMP_ALARM = "amperemaster_temp_alarm"

        fun start(context: Context) {
            try {
                val intent = Intent(context, BatteryTrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.w("BatteryService", "Foreground service konnte nicht gestartet werden", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, BatteryTrackingService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                android.util.Log.w("BatteryService", "Fehler beim Stoppen des Dienstes", e)
            }
        }
    }
}
