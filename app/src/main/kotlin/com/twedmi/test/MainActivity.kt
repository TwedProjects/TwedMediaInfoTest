package com.twedmi.test

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.Audio
import com.twedmediainfo.android.parameters.General
import com.twedmediainfo.android.parameters.Image
import com.twedmediainfo.android.parameters.Other
import com.twedmediainfo.android.parameters.Text
import com.twedmediainfo.android.parameters.Video
import com.twedmi.test.ui.theme.ComposeEmptyActivityTheme
import java.io.File

internal const val TAG = "TwedMediaInfoTest"

/** Marcador para campos vacíos o no presentes en el archivo. */
private const val NO_VALUE = "— sin valor / no presente —"

/** Fila de la vista de detalle: título de sección o campo clave/valor. */
sealed interface InfoRow {
    data class Section(val title: String) : InfoRow
    data class Field(val label: String, val value: String) : InfoRow
}

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

    var hasPermission by remember {
        mutableStateOf(checkStoragePermission(context))
    }

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
        ) {
            VersionBanner(
                modifier = Modifier.padding(16.dp)
            )

            if (!hasPermission) {
                PermissionRequest(
                    onPermissionGranted = {
                        hasPermission = true
                    }
                )
            } else {
                FileBrowser()
            }
        }
    }
}

private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun VersionBanner(modifier: Modifier = Modifier) {
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

    Card(modifier = modifier.fillMaxWidth()) {
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
fun PermissionRequest(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Se requiere permiso para acceder a todos los archivos",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "Android 11+ requiere acceso completo al almacenamiento"
            } else {
                "Se necesita permiso de lectura de almacenamiento"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }) {
            Text("Conceder permiso")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (checkStoragePermission(context)) {
                onPermissionGranted()
            }
        }) {
            Text("Verificar permiso")
        }
    }
}

// ==============================================================================
// Navegador de archivos (sin filtro de extensiones)
// ==============================================================================

@Composable
fun FileBrowser() {
    var directoryPath by remember {
        mutableStateOf("/storage/emulated/0/")
    }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileInfo by remember { mutableStateOf<List<InfoRow>?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
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
                Text(
                    text = "Explorador de archivos",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                            fileInfo = analyzeFile(file) ?: run {
                                fileError = "No se pudo analizar el archivo"
                                null
                            }
                        }
                    )
                }
            }
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
    info: List<InfoRow>,
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
                info.forEach { row ->
                    when (row) {
                        is InfoRow.Section -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        is InfoRow.Field -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${row.label}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(170.dp)
                                )
                                Text(
                                    text = row.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.value == NO_VALUE) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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

// ==============================================================================
// Carga de archivos (SIN filtro de extensiones)
// ==============================================================================

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

        val files = dir.listFiles { file -> file.isFile }
            ?.sortedBy { it.name.lowercase() } ?: emptyList()

        if (files.isEmpty()) {
            onError("No se encontraron archivos en este directorio")
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

// ==============================================================================
// Análisis unificado: General + Video + Audio + Image + Text + Other
// ==============================================================================

private fun field(label: String, value: String): InfoRow.Field =
    InfoRow.Field(label, value.ifEmpty { NO_VALUE })

private fun durationOrNA(raw: String): String =
    if (raw.isEmpty()) NO_VALUE else formatDuration(raw)

private fun sizeOrNA(raw: String): String =
    if (raw.isEmpty()) NO_VALUE else formatFileSize(raw.toLongOrNull() ?: 0L)

private fun bitrateOrNA(raw: String): String =
    if (raw.isEmpty()) NO_VALUE else formatBitrate(raw)

private fun analyzeFile(file: File): List<InfoRow>? {
    Log.i(TAG, "=== Analyzing file: ${file.name} ===")
    Log.d(TAG, "File path: ${file.absolutePath}")

    val mediaInfo = TwedMediaInfo()
    val rows = mutableListOf<InfoRow>()

    return try {
        if (!mediaInfo.open(file.absolutePath)) {
            Log.e(TAG, "Failed to open file")
            mediaInfo.destroy()
            return null
        }
        Log.i(TAG, "✅ File opened successfully")

        // ---------- GENERAL ----------
        rows += InfoRow.Section("General")
        rows += field("Nombre completo", mediaInfo.getGeneral(General.COMPLETE_NAME))
        rows += field("Formato", mediaInfo.getGeneral(General.FORMAT_STRING))
        rows += field("Formato (corto)", mediaInfo.getGeneral(General.FORMAT))
        rows += field("Tamaño", sizeOrNA(mediaInfo.getGeneral(General.FILE_SIZE)))
        rows += field("Duración", durationOrNA(mediaInfo.getGeneral(General.DURATION)))
        rows += field("Bitrate global", bitrateOrNA(mediaInfo.getGeneral(General.OVERALL_BITRATE)))
        rows += field("Título", mediaInfo.getGeneral(General.TITLE))
        rows += field("Álbum", mediaInfo.getGeneral(General.ALBUM))
        rows += field("Intérprete", mediaInfo.getGeneral(General.PERFORMER))
        rows += field("Género", mediaInfo.getGeneral(General.GENRE))
        rows += field("Fecha de grabación", mediaInfo.getGeneral(General.RECORDED_DATE))
        rows += field("Aplicación escritura", mediaInfo.getGeneral(General.ENCODED_APPLICATION))
        rows += field("Librería escritura", mediaInfo.getGeneral(General.ENCODED_LIBRARY))
        rows += field("Portada", mediaInfo.getGeneral(General.COVER))
        rows += field("Streams de video", mediaInfo.getGeneral(General.VIDEO_COUNT))
        rows += field("Streams de audio", mediaInfo.getGeneral(General.AUDIO_COUNT))
        rows += field("Streams de texto", mediaInfo.getGeneral(General.TEXT_COUNT))
        rows += field("Streams de imagen", mediaInfo.getGeneral(General.IMAGE_COUNT))
        rows += field("Streams Other", mediaInfo.getGeneral(General.OTHER_COUNT))

        // ---------- VIDEO ----------
        addVideoRows(mediaInfo, rows)

        // ---------- AUDIO ----------
        addAudioRows(mediaInfo, rows)

        // ---------- IMAGE ----------
        addImageRows(mediaInfo, rows)

        // ---------- TEXT ----------
        addTextRows(mediaInfo, rows)

        // ---------- OTHER ----------
        addOtherRows(mediaInfo, rows)

        mediaInfo.close()
        mediaInfo.destroy()

        Log.i(TAG, "=== ✅ Analysis complete for ${file.name} ===")
        rows
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during analysis", e)
        try { mediaInfo.destroy() } catch (_: Throwable) {}
        null
    }
}

private fun addVideoRows(mi: TwedMediaInfo, rows: MutableList<InfoRow>) {
    val count = mi.countStreams(StreamKind.VIDEO)
    if (count == 0) {
        rows += InfoRow.Section("Video")
        rows += InfoRow.Field("Streams", "0 — sin streams de video en este archivo")
        return
    }
    for (i in 0 until count) {
        rows += InfoRow.Section(if (count > 1) "Video #${i + 1}" else "Video")
        rows += field("ID", mi.get(StreamKind.VIDEO, i, Video.ID))
        rows += field("Formato", mi.get(StreamKind.VIDEO, i, Video.FORMAT_STRING))
        rows += field("Codec", mi.get(StreamKind.VIDEO, i, Video.FORMAT))
        rows += field("Codec ID", mi.get(StreamKind.VIDEO, i, Video.CODEC_ID))
        rows += field("Perfil", mi.get(StreamKind.VIDEO, i, Video.FORMAT_PROFILE))
        rows += field("Nivel", mi.get(StreamKind.VIDEO, i, Video.FORMAT_LEVEL))
        val w = mi.get(StreamKind.VIDEO, i, Video.WIDTH)
        val h = mi.get(StreamKind.VIDEO, i, Video.HEIGHT)
        rows += field("Resolución", if (w.isEmpty() || h.isEmpty()) NO_VALUE else "${w}x${h}")
        rows += field("Aspect ratio", mi.get(StreamKind.VIDEO, i, Video.DISPLAY_ASPECT_RATIO_STRING))
        rows += field("FPS", mi.get(StreamKind.VIDEO, i, Video.FRAME_RATE_STRING))
        rows += field("Bitrate", bitrateOrNA(mi.get(StreamKind.VIDEO, i, Video.BITRATE)))
        rows += field("Duración", durationOrNA(mi.get(StreamKind.VIDEO, i, Video.DURATION)))
        rows += field("Color space", mi.get(StreamKind.VIDEO, i, Video.COLOR_SPACE))
        rows += field("Chroma subsampling", mi.get(StreamKind.VIDEO, i, Video.CHROMA_SUBSAMPLING))
        rows += field("Bit depth", mi.get(StreamKind.VIDEO, i, Video.BIT_DEPTH))
        rows += field("Scan type", mi.get(StreamKind.VIDEO, i, Video.SCAN_TYPE))
        rows += field("HDR", mi.get(StreamKind.VIDEO, i, Video.HDR_FORMAT))
        rows += field("Tamaño stream", mi.get(StreamKind.VIDEO, i, Video.STREAM_SIZE_STRING))
        rows += field("Idioma", mi.get(StreamKind.VIDEO, i, Video.LANGUAGE_STRING))
        rows += field("Título", mi.get(StreamKind.VIDEO, i, Video.TITLE))
        rows += field("Default", mi.get(StreamKind.VIDEO, i, Video.DEFAULT))
        rows += field("Forced", mi.get(StreamKind.VIDEO, i, Video.FORCED))
    }
}

private fun addAudioRows(mi: TwedMediaInfo, rows: MutableList<InfoRow>) {
    val count = mi.countStreams(StreamKind.AUDIO)
    if (count == 0) {
        rows += InfoRow.Section("Audio")
        rows += InfoRow.Field("Streams", "0 — sin streams de audio en este archivo")
        return
    }
    for (i in 0 until count) {
        rows += InfoRow.Section(if (count > 1) "Audio #${i + 1}" else "Audio")
        rows += field("ID", mi.get(StreamKind.AUDIO, i, Audio.ID))
        rows += field("Formato", mi.get(StreamKind.AUDIO, i, Audio.FORMAT_STRING))
        rows += field("Codec", mi.get(StreamKind.AUDIO, i, Audio.FORMAT))
        rows += field("Codec ID", mi.get(StreamKind.AUDIO, i, Audio.CODEC_ID))
        rows += field("Duración", durationOrNA(mi.get(StreamKind.AUDIO, i, Audio.DURATION)))
        rows += field("Bitrate", bitrateOrNA(mi.get(StreamKind.AUDIO, i, Audio.BITRATE)))
        rows += field("Canales", mi.get(StreamKind.AUDIO, i, Audio.CHANNELS_STRING))
        rows += field("Layout", mi.get(StreamKind.AUDIO, i, Audio.CHANNEL_LAYOUT))
        rows += field("Sample rate", mi.get(StreamKind.AUDIO, i, Audio.SAMPLING_RATE_STRING))
        rows += field("Bit depth", mi.get(StreamKind.AUDIO, i, Audio.BIT_DEPTH))
        rows += field("Compresión", mi.get(StreamKind.AUDIO, i, Audio.COMPRESSION_MODE_STRING))
        rows += field("Tamaño stream", mi.get(StreamKind.AUDIO, i, Audio.STREAM_SIZE_STRING))
        rows += field("Idioma", mi.get(StreamKind.AUDIO, i, Audio.LANGUAGE_STRING))
        rows += field("Título", mi.get(StreamKind.AUDIO, i, Audio.TITLE))
        rows += field("Encoder", mi.get(StreamKind.AUDIO, i, Audio.ENCODED_LIBRARY))
        rows += field("Default", mi.get(StreamKind.AUDIO, i, Audio.DEFAULT))
        rows += field("Forced", mi.get(StreamKind.AUDIO, i, Audio.FORCED))
    }
}

private fun addImageRows(mi: TwedMediaInfo, rows: MutableList<InfoRow>) {
    val count = mi.countStreams(StreamKind.IMAGE)
    if (count == 0) {
        rows += InfoRow.Section("Image")
        rows += InfoRow.Field("Streams", "0 — sin streams de imagen en este archivo")
        return
    }
    for (i in 0 until count) {
        rows += InfoRow.Section(if (count > 1) "Image #${i + 1}" else "Image")
        rows += field("ID", mi.get(StreamKind.IMAGE, i, Image.ID))
        rows += field("Tipo", mi.get(StreamKind.IMAGE, i, Image.TYPE))
        rows += field("Formato", mi.get(StreamKind.IMAGE, i, Image.FORMAT_STRING))
        rows += field("Codec", mi.get(StreamKind.IMAGE, i, Image.FORMAT))
        val w = mi.get(StreamKind.IMAGE, i, Image.WIDTH)
        val h = mi.get(StreamKind.IMAGE, i, Image.HEIGHT)
        rows += field("Resolución", if (w.isEmpty() || h.isEmpty()) NO_VALUE else "${w}x${h}")
        rows += field("Aspect ratio", mi.get(StreamKind.IMAGE, i, Image.DISPLAY_ASPECT_RATIO_STRING))
        rows += field("Color space", mi.get(StreamKind.IMAGE, i, Image.COLOR_SPACE))
        rows += field("Chroma subsampling", mi.get(StreamKind.IMAGE, i, Image.CHROMA_SUBSAMPLING))
        rows += field("Bit depth", mi.get(StreamKind.IMAGE, i, Image.BIT_DEPTH))
        rows += field("Compresión", mi.get(StreamKind.IMAGE, i, Image.COMPRESSION_MODE_STRING))
        rows += field("HDR", mi.get(StreamKind.IMAGE, i, Image.HDR_FORMAT))
        rows += field("Tamaño stream", mi.get(StreamKind.IMAGE, i, Image.STREAM_SIZE_STRING))
        rows += field("Idioma", mi.get(StreamKind.IMAGE, i, Image.LANGUAGE_STRING))
    }
}

private fun addTextRows(mi: TwedMediaInfo, rows: MutableList<InfoRow>) {
    val count = mi.countStreams(StreamKind.TEXT)
    if (count == 0) {
        rows += InfoRow.Section("Text")
        rows += InfoRow.Field("Streams", "0 — sin streams de texto en este archivo")
        return
    }
    for (i in 0 until count) {
        rows += InfoRow.Section(if (count > 1) "Text #${i + 1}" else "Text")
        rows += field("ID", mi.get(StreamKind.TEXT, i, Text.ID))
        rows += field("Formato", mi.get(StreamKind.TEXT, i, Text.FORMAT_STRING))
        rows += field("Codec", mi.get(StreamKind.TEXT, i, Text.FORMAT))
        rows += field("Codec ID", mi.get(StreamKind.TEXT, i, Text.CODEC_ID))
        rows += field("Duración", durationOrNA(mi.get(StreamKind.TEXT, i, Text.DURATION)))
        rows += field("Idioma", mi.get(StreamKind.TEXT, i, Text.LANGUAGE_STRING))
        rows += field("Título", mi.get(StreamKind.TEXT, i, Text.TITLE))
        rows += field("Líneas", mi.get(StreamKind.TEXT, i, Text.LINES_COUNT))
        rows += field("Eventos", mi.get(StreamKind.TEXT, i, Text.EVENTS_TOTAL))
        rows += field("Tamaño stream", mi.get(StreamKind.TEXT, i, Text.STREAM_SIZE_STRING))
        rows += field("Default", mi.get(StreamKind.TEXT, i, Text.DEFAULT))
        rows += field("Forced", mi.get(StreamKind.TEXT, i, Text.FORCED))
    }
}

private fun addOtherRows(mi: TwedMediaInfo, rows: MutableList<InfoRow>) {
    val count = mi.countStreams(StreamKind.OTHER)
    if (count == 0) {
        rows += InfoRow.Section("Other")
        rows += InfoRow.Field("Streams", "0 — sin streams Other en este archivo")
        return
    }
    for (i in 0 until count) {
        rows += InfoRow.Section(if (count > 1) "Other #${i + 1}" else "Other")
        rows += field("ID", mi.get(StreamKind.OTHER, i, Other.ID))
        rows += field("Tipo", mi.get(StreamKind.OTHER, i, Other.TYPE))
        rows += field("Formato", mi.get(StreamKind.OTHER, i, Other.FORMAT_STRING))
        rows += field("Codec", mi.get(StreamKind.OTHER, i, Other.FORMAT))
        rows += field("Duración", durationOrNA(mi.get(StreamKind.OTHER, i, Other.DURATION)))
        rows += field("TimeCode 1er frame", mi.get(StreamKind.OTHER, i, Other.TIMECODE_FIRST_FRAME))
        rows += field("TimeCode fuente", mi.get(StreamKind.OTHER, i, Other.TIMECODE_SOURCE))
        rows += field("Idioma", mi.get(StreamKind.OTHER, i, Other.LANGUAGE_STRING))
        rows += field("Título", mi.get(StreamKind.OTHER, i, Other.TITLE))
    }
}

// ==============================================================================
// Utilidades de formato
// ==============================================================================

internal fun formatDuration(durationMs: String): String {
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

internal fun formatFileSize(bytes: Long): String {
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

internal fun formatBitrate(bitrate: String): String {
    val bps = bitrate.toLongOrNull() ?: return bitrate.ifEmpty { "N/A" }
    val kbps = bps / 1000
    return "$kbps kbps"
}