package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AmpereApplication

/**
 * Empfängt Systemereignisse beim Einstecken oder Abziehen des Ladekabels (ACTION_POWER_CONNECTED / DISCONNECTED)
 * und startet oder beendet den Foreground Service entsprechend den Nutzereinstellungen.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val app = context.applicationContext as? AmpereApplication
        val isServiceEnabled = app?.repository?.backgroundServiceEnabled?.value ?: true

        try {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (isServiceEnabled) {
                        BatteryTrackingService.start(context)
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    BatteryTrackingService.stop(context)
                }
            }

            // Sofort frischen Akku-Intent auslesen und an das Repository übermitteln
            val stickyIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (stickyIntent != null) {
                app?.repository?.processBatteryChangedIntent(stickyIntent)
            }
        } catch (e: Exception) {
            android.util.Log.w("PowerReceiver", "Fehler beim Behandeln des Ladeereignisses", e)
        }
    }
}
