package com.example.mysmartkiln.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.mysmartkiln.ui.viewmodels.TemperatureSample
import java.io.File

object CsvShareUtils {

    /**
     * Escribe un CSV en cache y abre el sheet de compartir (WhatsApp, Drive, Mail…).
     */
    fun shareTemperatureCsv(
        context: Context,
        fileNameBase: String,
        samples: List<TemperatureSample>,
        chooserTitle: String = "Exportar datos"
    ): Boolean {
        if (samples.isEmpty()) return false
        return try {
            val safe = fileNameBase
                .replace(Regex("[^A-Za-z0-9._\\-]"), "_")
                .take(48)
                .ifBlank { "curva" }
            val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val file = File(dir, "${safe}.csv")
            file.writeText(buildCsv(samples))
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileNameBase)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Curva MySmartKiln: $fileNameBase (${samples.size} muestras)"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, chooserTitle))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun buildCsv(samples: List<TemperatureSample>): String {
        val sb = StringBuilder()
        sb.appendLine("elapsedMin,temperatureC")
        samples.forEach { s ->
            sb.append(s.elapsedMin)
            sb.append(',')
            sb.append(s.temperatureC)
            sb.appendLine()
        }
        return sb.toString()
    }
}
