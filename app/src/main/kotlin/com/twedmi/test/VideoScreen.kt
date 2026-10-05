package com.twedmi.test

import android.util.Log
import androidx.compose.runtime.Composable
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.Audio
import com.twedmediainfo.android.parameters.General
import com.twedmediainfo.android.parameters.Video
import java.io.File

@Composable
fun VideoScreen() {
    MediaScreen(
        title = "Archivos de Video",
        extensions = VIDEO_EXTENSIONS,
        analyzeFile = ::analyzeVideoFile
    )
}

private fun analyzeVideoFile(file: File): Map<String, String>? {
    Log.i(TAG, "=== Analyzing video file: ${file.name} ===")
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

        // Metadatos ricos del contenedor (película / producción)
        result.putIfNotEmpty("Película", mediaInfo.getGeneral(General.MOVIE))
        result.putIfNotEmpty("Director", mediaInfo.getGeneral(General.DIRECTOR))
        result.putIfNotEmpty("Género", mediaInfo.getGeneral(General.GENRE))
        result.putIfNotEmpty("Fecha (grabación)", mediaInfo.getGeneral(General.RECORDED_DATE))
        result.putIfNotEmpty("Encoder (aplicación)", mediaInfo.getGeneral(General.ENCODED_APPLICATION))
        result.putIfNotEmpty("Encoder (librería)", mediaInfo.getGeneral(General.ENCODED_LIBRARY))

        // Streams de video
        val videoCount = mediaInfo.countStreams(StreamKind.VIDEO)
        result["Streams de video"] = videoCount.toString()

        if (videoCount > 0) {
            result["Codec"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.FORMAT)
            result["Codec (string)"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.FORMAT_STRING)
            result["Perfil"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.FORMAT_PROFILE)
            result["Nivel"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.FORMAT_LEVEL)

            val width = mediaInfo.get(StreamKind.VIDEO, 0, Video.WIDTH)
            val height = mediaInfo.get(StreamKind.VIDEO, 0, Video.HEIGHT)
            result["Resolución"] = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x${height}" else ""

            result["Aspect ratio"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.DISPLAY_ASPECT_RATIO_STRING)
            result["FPS"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.FRAME_RATE_STRING)
            result["Bitrate"] = formatBitrate(mediaInfo.get(StreamKind.VIDEO, 0, Video.BITRATE))
            result["Bitrate (string)"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.BITRATE_STRING)
            result["Duración"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.DURATION_STRING)
            result["Color space"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.COLOR_SPACE)
            result["Chroma subsampling"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.CHROMA_SUBSAMPLING)
            result["Bit depth"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.BIT_DEPTH)
            result["Scan type"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.SCAN_TYPE)
            result["HDR format"] = mediaInfo.get(StreamKind.VIDEO, 0, Video.HDR_FORMAT)
        }

        // Streams de audio (si existen)
        val audioCount = mediaInfo.countStreams(StreamKind.AUDIO)
        result["Streams de audio"] = audioCount.toString()

        if (audioCount > 0) {
            result["Audio codec"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.FORMAT)
            result["Audio canales"] = mediaInfo.get(StreamKind.AUDIO, 0, Audio.CHANNELS_STRING)
            result["Audio bitrate"] = formatBitrate(mediaInfo.get(StreamKind.AUDIO, 0, Audio.BITRATE))
        }

        mediaInfo.close()
        mediaInfo.destroy()

        Log.i(TAG, "=== ✅ Video analysis complete for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during video analysis", e)
        try { mediaInfo.destroy() } catch (_: Throwable) {}
        null
    }
}