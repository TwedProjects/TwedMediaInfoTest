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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
                MainScreen()
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

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Audio", "Video", "Image", "Text")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> AudioScreen()
            1 -> VideoScreen()
            2 -> ImageScreen()
            3 -> TextScreen()
        }
    }
}

@Composable
fun MediaScreen(
    title: String,
    extensions: Set<String>,
    analyzeFile: (File) -> Map<String, String>?
) {
    var directoryPath by remember {
        mutableStateOf("/storage/emulated/0/")
    }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileInfo by remember { mutableStateOf<Map<String, String>?>(null) }
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                DirectorySelector(
                    directoryPath = directoryPath,
                    onDirectoryPathChange = { directoryPath = it },
                    onLoadFiles = {
                        errorMessage = null
                        files = loadFilesFromDirectory(directoryPath, extensions) { error ->
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "$key:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(160.dp)
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

internal val AUDIO_EXTENSIONS = setOf(
    "mp3", "ogg", "opus", "flac", "m4a", "aac", "wav", "wma", "alac",
    "mka", "mid", "midi", "amr", "awb", "ac3", "eac3", "dts",
    "ape", "wv", "aiff", "aif", "caf", "dsf", "dff"
)

internal val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "mov", "avi", "3gp", "3g2", "ts", "m2ts", "mts",
    "flv", "wmv", "mpg", "mpeg", "m4v", "vob", "ogv", "divx", "xvid",
    "rm", "rmvb", "asf", "m2v", "m4p"
)

internal val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp",
    "heic", "heif", "avif", "jxl",
    "tiff", "tif",
    "dng", "cr2", "cr3", "nef", "arw", "orf", "rw2", "raf", "pef", "srw",
    "ico", "cur", "pcx", "tga", "ppm", "pgm", "pbm", "pam"
)

internal val TEXT_CONTAINER_EXTENSIONS = AUDIO_EXTENSIONS + VIDEO_EXTENSIONS

private fun loadFilesFromDirectory(
    path: String,
    extensions: Set<String>,
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
            file.isFile && file.extension.lowercase() in extensions
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

internal fun MutableMap<String, String>.putIfNotEmpty(key: String, value: String) {
    if (value.isNotEmpty()) put(key, value)
}

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

internal fun formatSampleRate(sampleRate: String): String {
    val hz = sampleRate.toLongOrNull() ?: return sampleRate.ifEmpty { "N/A" }
    return "$hz Hz"
}