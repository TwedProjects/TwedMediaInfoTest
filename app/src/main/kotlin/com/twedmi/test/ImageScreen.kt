package com.twedmi.test

import android.util.Log
import androidx.compose.runtime.Composable
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.General
import com.twedmediainfo.android.parameters.Image
import java.io.File

@Composable
fun ImageScreen() {
    MediaScreen(
        title = "Archivos de Imagen",
        extensions = IMAGE_EXTENSIONS,
        analyzeFile = ::analyzeImageFile
    )
}

private fun analyzeImageFile(file: File): Map<String, String>? {
    Log.i(TAG, "=== Analyzing image file: ${file.name} ===")
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

        // Metadatos ricos del contenedor
        result.putIfNotEmpty("Título (contenedor)", mediaInfo.getGeneral(General.TITLE))
        result.putIfNotEmpty("Encoder (aplicación)", mediaInfo.getGeneral(General.ENCODED_APPLICATION))
        result.putIfNotEmpty("Fecha (contenedor)", mediaInfo.getGeneral(General.RECORDED_DATE))

        // Streams de imagen
        val imageCount = mediaInfo.countStreams(StreamKind.IMAGE)
        result["Streams de imagen"] = imageCount.toString()

        if (imageCount > 0) {
            result["Codec"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT)
            result["Codec (string)"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_STRING)
            result["Perfil"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_PROFILE)
            result["Compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_COMPRESSION)

            val type = mediaInfo.get(StreamKind.IMAGE, 0, Image.TYPE)
            if (type.isNotEmpty()) result["Tipo"] = type

            val width = mediaInfo.get(StreamKind.IMAGE, 0, Image.WIDTH)
            val height = mediaInfo.get(StreamKind.IMAGE, 0, Image.HEIGHT)
            result["Resolución"] = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x${height}" else ""
            result["Aspect ratio"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.DISPLAY_ASPECT_RATIO_STRING)
            result["Pixel aspect ratio"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.PIXEL_ASPECT_RATIO)

            result["Color space"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOR_SPACE)
            result["Chroma subsampling"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.CHROMA_SUBSAMPLING)
            result["Bit depth"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.BIT_DEPTH)
            result["Rango de color"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOUR_RANGE)
            result["Primarios"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOUR_PRIMARIES)
            result["Transfer"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.TRANSFER_CHARACTERISTICS)
            result["Matrix coef."] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MATRIX_COEFFICIENTS)

            val hdrFormat = mediaInfo.get(StreamKind.IMAGE, 0, Image.HDR_FORMAT)
            if (hdrFormat.isNotEmpty()) {
                result["HDR format"] = hdrFormat
                result["MaxCLL"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MAX_CLL)
                result["MaxFALL"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MAX_FALL)
                result["Mastering primaries"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MASTERING_DISPLAY_COLOR_PRIMARIES)
                result["Mastering luminance"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MASTERING_DISPLAY_LUMINANCE)
            }

            result["Modo compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COMPRESSION_MODE)
            result["Ratio compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COMPRESSION_RATIO)
            result["Tamaño stream"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.STREAM_SIZE_STRING)
            result["Encoder"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.ENCODED_LIBRARY)
            result["Fecha"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.ENCODED_DATE)
            result["Idioma"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.LANGUAGE)

            val summary = mediaInfo.get(StreamKind.IMAGE, 0, Image.SUMMARY)
            if (summary.isNotEmpty()) result["Resumen"] = summary
        }

        mediaInfo.close()
        mediaInfo.destroy()

        Log.i(TAG, "=== ✅ Image analysis complete for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during image analysis", e)
        try { mediaInfo.destroy() } catch (_: Throwable) {}
        null
    }
}