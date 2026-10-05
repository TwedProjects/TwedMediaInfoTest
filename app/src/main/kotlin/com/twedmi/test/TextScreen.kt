package com.twedmi.test

import android.util.Log
import androidx.compose.runtime.Composable
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.General
import com.twedmediainfo.android.parameters.Text
import java.io.File

@Composable
fun TextScreen() {
    MediaScreen(
        title = "Subtítulos (en contenedores A/V)",
        extensions = TEXT_CONTAINER_EXTENSIONS,
        analyzeFile = ::analyzeTextFile
    )
}

private fun analyzeTextFile(file: File): Map<String, String>? {
    Log.i(TAG, "=== Analyzing text streams in: ${file.name} ===")
    Log.d(TAG, "File path: ${file.absolutePath}")

    val mediaInfo = TwedMediaInfo()
    val result = mutableMapOf<String, String>()

    return try {
        if (!mediaInfo.open(file.absolutePath)) {
            Log.e(TAG, "Failed to open file")
            mediaInfo.destroy()
            return null
        }
        Log.i(TAG, "✅ File opened successfully")

        // Contenedor (General)
        result["Formato contenedor"] = mediaInfo.getGeneral(General.FORMAT)
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral(General.FILE_SIZE).toLongOrNull() ?: 0L)
        result["Duración total"] = formatDuration(mediaInfo.getGeneral(General.DURATION))

        // Metadatos ricos del contenedor (resumen de subtítulos)
        result.putIfNotEmpty("Título (contenedor)", mediaInfo.getGeneral(General.TITLE))
        result.putIfNotEmpty("Formatos de subtítulos", mediaInfo.getGeneral(General.TEXT_FORMAT_LIST))
        result.putIfNotEmpty("Idiomas de subtítulos", mediaInfo.getGeneral(General.TEXT_LANGUAGE_LIST))

        // Streams de texto
        val textCount = mediaInfo.countStreams(StreamKind.TEXT)
        result["Streams de subtítulos"] = textCount.toString()

        if (textCount == 0) {
            result["Estado"] = "⚠️ Este archivo no contiene streams de subtítulos"
        } else {
            for (i in 0 until textCount) {
                val streamLabel = if (textCount > 1) "Subtítulo #${i + 1}" else "Subtítulo"

                val format = mediaInfo.get(StreamKind.TEXT, i, Text.FORMAT)
                val formatString = mediaInfo.get(StreamKind.TEXT, i, Text.FORMAT_STRING)
                if (format.isNotEmpty()) {
                    result["$streamLabel - Formato"] = if (formatString.isNotEmpty()) formatString else format
                }

                val codecId = mediaInfo.get(StreamKind.TEXT, i, Text.CODEC_ID)
                if (codecId.isNotEmpty()) {
                    result["$streamLabel - Codec ID"] = codecId
                }

                val muxingMode = mediaInfo.get(StreamKind.TEXT, i, Text.MUXING_MODE)
                if (muxingMode.isNotEmpty()) {
                    result["$streamLabel - Muxing"] = muxingMode
                }

                val language = mediaInfo.get(StreamKind.TEXT, i, Text.LANGUAGE_STRING)
                if (language.isNotEmpty()) {
                    result["$streamLabel - Idioma"] = language
                }

                val title = mediaInfo.get(StreamKind.TEXT, i, Text.TITLE)
                if (title.isNotEmpty()) {
                    result["$streamLabel - Título"] = title
                }

                val isDefault = mediaInfo.get(StreamKind.TEXT, i, Text.DEFAULT)
                if (isDefault.isNotEmpty() && isDefault == "Yes") {
                    result["$streamLabel - Por defecto"] = "✓ Sí"
                }

                val isForced = mediaInfo.get(StreamKind.TEXT, i, Text.FORCED)
                if (isForced.isNotEmpty() && isForced == "Yes") {
                    result["$streamLabel - Forzado"] = "✓ Sí"
                }

                val width = mediaInfo.get(StreamKind.TEXT, i, Text.WIDTH)
                val height = mediaInfo.get(StreamKind.TEXT, i, Text.HEIGHT)
                if (width.isNotEmpty() && height.isNotEmpty()) {
                    result["$streamLabel - Dimensiones"] = "${width}x${height} caracteres"
                }

                val linesCount = mediaInfo.get(StreamKind.TEXT, i, Text.LINES_COUNT)
                if (linesCount.isNotEmpty()) {
                    result["$streamLabel - Líneas totales"] = linesCount
                }

                val eventsTotal = mediaInfo.get(StreamKind.TEXT, i, Text.EVENTS_TOTAL)
                if (eventsTotal.isNotEmpty()) {
                    result["$streamLabel - Eventos"] = eventsTotal
                }

                val duration = mediaInfo.get(StreamKind.TEXT, i, Text.DURATION_STRING)
                if (duration.isNotEmpty()) {
                    result["$streamLabel - Duración"] = duration
                }

                val streamSize = mediaInfo.get(StreamKind.TEXT, i, Text.STREAM_SIZE_STRING)
                if (streamSize.isNotEmpty()) {
                    result["$streamLabel - Tamaño"] = streamSize
                }
            }
        }

        mediaInfo.close()
        mediaInfo.destroy()

        Log.i(TAG, "=== ✅ Text analysis complete for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during text analysis", e)
        try { mediaInfo.destroy() } catch (_: Throwable) {}
        null
    }
}