package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.BatterySampleEntity
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dienstprogramm zum Exportieren von gemessenen Akkudaten als echte CSV-Datei via FileProvider.
 */
object CsvExportUtil {

    fun buildCsvContent(samples: List<BatterySampleEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("Timestamp,DateTime,LevelPercent,CurrentMilliAmp,VoltageVolt,PowerWatts,TemperatureCelsius,IsCharging\n")

        for (sample in samples) {
            val dateStr = sdf.format(Date(sample.timestamp))
            sb.append(sample.timestamp).append(",")
            sb.append("\"").append(dateStr).append("\",")
            sb.append(String.format(Locale.US, "%.2f", sample.levelPercent)).append(",")
            sb.append(String.format(Locale.US, "%.1f", sample.currentMilliAmp)).append(",")
            sb.append(String.format(Locale.US, "%.2f", sample.voltageVolt)).append(",")
            sb.append(String.format(Locale.US, "%.1f", sample.powerWatts)).append(",")
            sb.append(String.format(Locale.US, "%.1f", sample.temperatureCelsius)).append(",")
            sb.append(sample.isCharging).append("\n")
        }
        return sb.toString()
    }

    fun exportSamplesToCsv(
        context: Context,
        samples: List<BatterySampleEntity>,
        language: AppLanguage = AppLanguage.DE
    ) {
        if (samples.isEmpty()) return

        val csvContent = buildCsvContent(samples)
        val strings = AppStrings.get(language)
        val fileName = "AmpereMaster_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"

        val shareIntent = try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(exportDir, fileName)
            csvFile.writeText(csvContent, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)

            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (_: Exception) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, csvContent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val chooser = Intent.createChooser(shareIntent, strings.csvShareChooser).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
