/*
 * Copyright 2026 TwedMediaInfo Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twedmi.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.twedmediainfo.android.AudioParameter
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmi.test.ui.theme.ComposeEmptyActivityTheme
import java.io.File

private const val TAG = "TwedMediaInfoTest"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeEmptyActivityTheme {
                MediaInfoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoApp() {
    val context = LocalContext.current

    var directoryPath by remember {
        mutableStateOf("/storage/emulated/0/Music/Music/")
    }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileInfo by remember { mutableStateOf<Map<String, String>?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* recomposición automática al cambiar hasPermission */ }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("TwedMediaInfo Test") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Versión de la biblioteca (valida que carga nativa funciona)
            VersionBanner()

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !hasPermission -> {
                    PermissionRequest {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        requestPermissionLauncher.launch(permission)
                    }
                }

                selectedFile != null -> {
                    if (fileError != null) {
                        ErrorView(
                            message = fileError!!,
                            onBack = {
                                selectedFile = null
                                fileInfo = null
                                fileError = null
                            }
                        )
                    } else if (fileInfo != null) {
                        FileInfoView(
                            file = selectedFile!!,
                            info = fileInfo!!,
                            onBack = {
                                selectedFile = null
                                fileInfo = null
                                fileError = null
                            }
                        )
                    }
                }

                else -> {
                    DirectorySelector(
                        directoryPath = directoryPath,
                        onDirectoryPathChange = { directoryPath = it },
                        onLoadFiles = {
                            errorMessage = null
                            files = loadFilesFromDirectory(directoryPath) { error ->
                                errorMessage = error
                            }
                        },
                        errorMessage = errorMessage
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (files.isNotEmpty()) {
                        FileList(
                            files = files,
                            onFileClick = { file ->
                                selectedFile = file
                                fileError = null
                                fileInfo = analyzeFile(file) { err -> fileError = err }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionBanner() {
    var versions by remember { mutableStateOf<Pair<String, String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (versions == null && error == null) {
        try {
            Log.d(TAG, "=== Testing getMediaInfoVersion() ===")
            val mi = TwedMediaInfo.getMediaInfoVersion()
            Log.i(TAG, "MediaInfoLib version: $mi")
            
            Log.d(TAG, "=== Testing getZenLibVersion() ===")
            val zl = TwedMediaInfo.getZenLibVersion()
            Log.i(TAG, "ZenLib version: $zl")
            
            versions = mi to zl
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting versions", e)
            error = e.message ?: e.javaClass.simpleName
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Estado del wrapper",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (error != null) {
                Text(
                    text = "❌ No se pudo cargar la biblioteca nativa",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val (mi, zl) = versions ?: ("" to "")
                Text(
                    text = "MediaInfoLib: $mi",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "ZenLib: $zl",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Se requiere permiso para leer archivos multimedia",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder permiso")
        }
    }
}

@Composable
fun DirectorySelector(
    directoryPath: String,
    onDirectoryPathChange: (String) -> Unit,
    onLoadFiles: () -> Unit,
    errorMessage: String?
) {
    Text(
        text = "Directorio a explorar:",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = directoryPath,
        onValueChange = onDirectoryPathChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Ruta del directorio") },
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onLoadFiles,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cargar archivos")
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun FileList(files: List<File>, onFileClick: (File) -> Unit) {
    Text(
        text = "Archivos encontrados (${files.size}):",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(files) { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFileClick(file) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatFileSize(file.length()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileInfoView(
    file: File,
    info: Map<String, String>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Volver a la lista")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = file.name,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = file.absolutePath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Información del archivo",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                info.forEach { (key, value) ->
                    if (value.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$key:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(140.dp)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Volver a la lista")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "❌ Error al analizar archivo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private val MEDIA_EXTENSIONS = setOf(
    "mp3", "ogg", "opus", "flac", "m4a", "aac", "wav", "wma", "alac",
    "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv"
)

private fun loadFilesFromDirectory(
    path: String,
    onError: (String) -> Unit
): List<File> {
    return try {
        val dir = File(path)
        if (!dir.exists()) {
            onError("El directorio no existe: $path")
            return emptyList()
        }
        if (!dir.isDirectory) {
            onError("La ruta no es un directorio: $path")
            return emptyList()
        }

        val files = dir.listFiles { file ->
            file.isFile && file.extension.lowercase() in MEDIA_EXTENSIONS
        }?.sortedBy { it.name.lowercase() } ?: emptyList()

        if (files.isEmpty()) {
            onError("No se encontraron archivos multimedia en este directorio")
        }

        files
    } catch (e: SecurityException) {
        onError("No tienes permiso para acceder a este directorio")
        emptyList()
    } catch (e: Exception) {
        onError("Error al leer el directorio: ${e.message}")
        emptyList()
    }
}

private fun analyzeFile(
    file: File,
    onError: (String) -> Unit
): Map<String, String>? {
    Log.i(TAG, "=== Analyzing file: ${file.name} ===")
    Log.d(TAG, "File path: ${file.absolutePath}")
    
    val mediaInfo = TwedMediaInfo()
    val result = mutableMapOf<String, String>()

    return try {
        Log.d(TAG, "=== Testing open() ===")
        if (!mediaInfo.open(file.absolutePath)) {
            Log.e(TAG, "Failed to open file")
            onError("No se pudo abrir el archivo con MediaInfoLib")
            mediaInfo.destroy()
            return null
        }
        Log.i(TAG, "✅ File opened successfully")

        // Información general
        Log.d(TAG, "=== Testing getGeneral() ===")
        result["Formato"] = mediaInfo.getGeneral("Format").also { 
            Log.i(TAG, "Format: $it")
        }
        result["Duración"] = formatDuration(mediaInfo.getGeneral("Duration")).also {
            Log.i(TAG, "Duration: $it")
        }
        result["Tamaño (reportado)"] = formatFileSize(mediaInfo.getGeneral("FileSize").toLongOrNull() ?: 0L).also {
            Log.i(TAG, "FileSize: $it")
        }
        result["Bitrate total"] = formatBitrate(mediaInfo.getGeneral("OverallBitRate")).also {
            Log.i(TAG, "OverallBitRate: $it")
        }

        // Streams de audio
        Log.d(TAG, "=== Testing countStreams(StreamKind.Audio) ===")
        val audioCount = mediaInfo.countStreams(StreamKind.Audio)
        result["Streams de audio"] = audioCount.toString()
        Log.i(TAG, "Audio streams: $audioCount")

        if (audioCount > 0) {
            Log.d(TAG, "=== Testing get() with AudioParameter constants ===")
            
            // Formato
            result["Codec de audio"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.FORMAT).also {
                Log.i(TAG, "Audio Format: $it")
            }
            result["Formato (string)"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.FORMAT_STRING).also {
                Log.i(TAG, "Audio Format/String: $it")
            }
            
            // Duración
            result["Duración audio"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.DURATION_STRING).also {
                Log.i(TAG, "Audio Duration/String: $it")
            }
            
            // Bitrate
            result["Bitrate de audio"] = formatBitrate(mediaInfo.get(StreamKind.Audio, 0, AudioParameter.BITRATE)).also {
                Log.i(TAG, "Audio BitRate: $it")
            }
            result["Bitrate (string)"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.BITRATE_STRING).also {
                Log.i(TAG, "Audio BitRate/String: $it")
            }
            
            // Canales
            result["Canales"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.CHANNELS).also {
                Log.i(TAG, "Audio Channels: $it")
            }
            result["Canales (string)"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.CHANNELS_STRING).also {
                Log.i(TAG, "Audio Channels/String: $it")
            }
            result["Layout de canales"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.CHANNEL_LAYOUT).also {
                Log.i(TAG, "Audio ChannelLayout: $it")
            }
            
            // Sampling
            result["Sample rate"] = formatSampleRate(mediaInfo.get(StreamKind.Audio, 0, AudioParameter.SAMPLING_RATE)).also {
                Log.i(TAG, "Audio SamplingRate: $it")
            }
            
            // Profundidad
            result["Bit depth"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.BIT_DEPTH).also {
                Log.i(TAG, "Audio BitDepth: $it")
            }
            
            // Metadata
            result["Título"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.TITLE).also {
                Log.i(TAG, "Audio Title: $it")
            }
            result["Idioma"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.LANGUAGE).also {
                Log.i(TAG, "Audio Language: $it")
            }
            result["Encoder"] = mediaInfo.get(StreamKind.Audio, 0, AudioParameter.ENCODED_LIBRARY).also {
                Log.i(TAG, "Audio Encoded_Library: $it")
            }
            
            // Prueba de edge case: parámetro inexistente
            Log.d(TAG, "=== Testing get() with non-existent parameter ===")
            val nonExistent = mediaInfo.get(StreamKind.Audio, 0, "NonExistentParameter12345")
            Log.i(TAG, "Non-existent parameter result: '$nonExistent' (expected empty string)")
            if (nonExistent.isNotEmpty()) {
                Log.w(TAG, "⚠️ Non-existent parameter returned non-empty value!")
            }
        }

        // Streams de video (si existen)
        Log.d(TAG, "=== Testing countStreams(StreamKind.Video) ===")
        val videoCount = mediaInfo.countStreams(StreamKind.Video)
        result["Streams de video"] = videoCount.toString()
        Log.i(TAG, "Video streams: $videoCount")

        if (videoCount > 0) {
            Log.d(TAG, "=== Testing get() with Video parameters ===")
            result["Codec de video"] = mediaInfo.get(StreamKind.Video, 0, "Format").also {
                Log.i(TAG, "Video Format: $it")
            }
            val width = mediaInfo.get(StreamKind.Video, 0, "Width").also {
                Log.i(TAG, "Video Width: $it")
            }
            val height = mediaInfo.get(StreamKind.Video, 0, "Height").also {
                Log.i(TAG, "Video Height: $it")
            }
            result["Resolución"] = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x$height" else ""
            result["FPS"] = mediaInfo.get(StreamKind.Video, 0, "FrameRate").also {
                Log.i(TAG, "Video FrameRate: $it")
            }
            result["Bitrate de video"] = formatBitrate(mediaInfo.get(StreamKind.Video, 0, "BitRate")).also {
                Log.i(TAG, "Video BitRate: $it")
            }
        }

        Log.d(TAG, "=== Testing close() ===")
        mediaInfo.close()
        Log.i(TAG, "✅ File closed successfully")
        
        Log.d(TAG, "=== Testing destroy() ===")
        mediaInfo.destroy()
        Log.i(TAG, "✅ Instance destroyed successfully")
        
        Log.i(TAG, "=== ✅ All tests passed for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during analysis", e)
        onError("Excepción al analizar: ${e.javaClass.simpleName}: ${e.message}")
        try {
            mediaInfo.destroy()
        } catch (_: Throwable) {
            // Ignorar errores de limpieza
        }
        null
    }
}

private fun formatDuration(durationMs: String): String {
    val ms = durationMs.toLongOrNull() ?: return durationMs.ifEmpty { "N/A" }
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "N/A"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return String.format("%.2f %s", size, units[unitIndex])
}

private fun formatBitrate(bitrate: String): String {
    val bps = bitrate.toLongOrNull() ?: return bitrate.ifEmpty { "N/A" }
    val kbps = bps / 1000
    return "$kbps kbps"
}

private fun formatSampleRate(sampleRate: String): String {
    val hz = sampleRate.toIntOrNull() ?: return sampleRate.ifEmpty { "N/A" }
    return "$hz Hz"
}