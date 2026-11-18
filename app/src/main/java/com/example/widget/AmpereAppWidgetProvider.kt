package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.AmpereApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.BatteryMetrics
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import java.util.Locale

/**
 * AppWidgetProvider für das AmpereMaster Home-Screen-Widget mit Mehrsprachen-Support
 * sowie umschaltbarer Kompakt- vs. SOH-Detailansicht (v1.0.4).
 */
class AmpereAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as? AmpereApplication
        val currentMetrics = app?.repository?.batteryMetrics?.value ?: BatteryMetrics()

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentMetrics)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            metrics: BatteryMetrics
        ) {
            val prefs = context.getSharedPreferences("amperemaster_prefs", Context.MODE_PRIVATE)
            val langCode = prefs.getString("language_code", Locale.getDefault().language) ?: "de"
            val strings = AppStrings.get(AppLanguage.fromCode(langCode))
            val detailedMode = prefs.getBoolean("widget_detailed_mode", true)

            val views = RemoteViews(context.packageName, R.layout.app_widget_battery)

            // Klick auf das Widget öffnet die App
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Prozentanzeige
            val levelStr = String.format(Locale.US, "%.2f %%", metrics.levelPercent)
            views.setTextViewText(R.id.widget_level_percent, levelStr)

            // Status & Protokoll (lokalisiert)
            val statusText = if (metrics.isCharging) {
                val base = if (metrics.detectedProtocol.isNotBlank() && metrics.detectedProtocol != "Standard") {
                    metrics.detectedProtocol
                } else {
                    "${strings.charging} (${metrics.chargingSource.name})"
                }
                if (metrics.activeChargerProfile.isNotBlank()) "⚡ $base • ${metrics.activeChargerProfile}" else "⚡ $base"
            } else {
                strings.discharging
            }
            views.setTextViewText(R.id.widget_status_text, statusText)

            // Details: mA, W, Temp (+ SOH im Detailmodus)
            val detailsText = if (detailedMode) {
                String.format(
                    Locale.US,
                    "%.0f mA • %.1f W • %.1f °C • SOH %.0f%%",
                    metrics.currentMilliAmp,
                    metrics.powerWatts,
                    metrics.temperatureCelsius,
                    metrics.sohPercent
                )
            } else {
                String.format(
                    Locale.US,
                    "%.0f mA • %.1f W • %.1f °C",
                    metrics.currentMilliAmp,
                    metrics.powerWatts,
                    metrics.temperatureCelsius
                )
            }
            views.setTextViewText(R.id.widget_details, detailsText)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, metrics: BatteryMetrics) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AmpereAppWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids != null && ids.isNotEmpty()) {
                    for (id in ids) {
                        updateWidget(context, appWidgetManager, id, metrics)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AmpereWidget", "Fehler beim Widget-Update", e)
            }
        }
    }
}
