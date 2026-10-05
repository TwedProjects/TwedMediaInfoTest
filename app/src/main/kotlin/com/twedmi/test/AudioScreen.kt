package com.twedmi.test

import android.util.Log
import androidx.compose.runtime.Composable
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.Audio
import com.twedmediainfo.android.parameters.General
import java.io.File

@Composable
fun AudioScreen() {
    MediaScreen(
        title = "Archivos de Audio",
        extensions = AUDIO_EXTENSIONS,
        analyzeFile = ::analyzeAudioFile
    )
}

private fun analyzeAudioFile(file: File): Map<String, String>? {
    Log.i(TAG, "=== Analyzing audio file: ${file.name} ===")
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
        result["Duración"] = formatDuration(mediaInfo.getGeneral(General.DURATION))
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral(General.FILE_SIZE).toLongOrNull() ?: 0L)
        result["Bitrate total"] = formatBitrate(mediaInfo.getGeneral(General.OVERALL_BITRATE))

        // Metadatos ricos del contenedor (tags de álbum)
        result.putIfNotEmpty("Título (contenedor)", mediaInfo.getGeneral(General.TITLE))
        result.putIfNotEmpty("Género", mediaInfo.getGeneral(General.GENRE))
        result.putIfNotEmpty("Fecha (grabación)", mediaInfo.getGeneral(General.RECORDED_DATE))
        result.putIfNotEmpty("Encoder (aplicación)", mediaInfo.getGeneral(General.ENCODED_APPLICATION))

        // Streams de audio
        val audioCount = mediaInfo.countStreams(StreamKind.AUDIO)
        result["Streams de audio"] = audioCount.toString()

        if (audioCount > 0) {
            result["Codec"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.FORMAT)
            result["Codec (string)"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.FORMAT_STRING)
            result["Duración"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.DURATION_STRING)
            result["Bitrate"] = formatBitrate(mediaInfo.get(StreamKind.AUDIO, 0, Audio.BITRATE))
            result["Bitrate (string)"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.BITRATE_STRING)
            result["Canales"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.CHANNELS)
            result["Canales (string)"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.CHANNELS_STRING)
            result["Layout"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.CHANNEL_LAYOUT)
            result["Sample rate"] = formatSampleRate(mediaInfo.get(StreamKind.AUDIO, 0, Audio.SAMPLING_RATE))
            result["Bit depth"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.BIT_DEPTH)
            result["Título"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.TITLE)
            result["Artista"] = mediaInfo.get(StreamKind.AUDIO, 0, "Performer")
            result["Álbum"] = mediaInfo.get(StreamKind.AUDIO, 0, "Album")
            result["Idioma"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.LANGUAGE)
            result["Encoder"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.ENCODED_LIBRARY)
        }

        mediaInfo.close()
        mediaInfo.destroy()

        Log.i(TAG, "=== ✅ Audio analysis complete for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during audio analysis", e)
        try { mediaInfo.destroy() } catch (_: Throwable) {}
        null
    }
}