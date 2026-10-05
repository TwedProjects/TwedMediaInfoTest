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
import com.twedmediainfo.android.StreamKind
import com.twedmediainfo.android.TwedMediaInfo
import com.twedmediainfo.android.parameters.Audio
import com.twedmediainfo.android.parameters.Image
import com.twedmediainfo.android.parameters.Text
import com.twedmediainfo.android.parameters.Video
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
            // Versión de la biblioteca (valida que carga nativa funciona)
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
                // Pantalla principal con tabs
                MainScreen()
            }
        }
    }
}

private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ : MANAGE_EXTERNAL_STORAGE
        Environment.isExternalStorageManager()
    } else {
        // Android 10 y anteriores: READ_EXTERNAL_STORAGE
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
                // Android 11+: Abrir configuración de acceso a todos los archivos
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } else {
                // Android 10 y anteriores: Solicitar READ_EXTERNAL_STORAGE
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }) {
            Text("Conceder permiso")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            // Verificar si el permiso fue concedido después de regresar de configuración
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
fun AudioScreen() {
    MediaScreen(
        title = "Archivos de Audio",
        extensions = AUDIO_EXTENSIONS,
        analyzeFile = ::analyzeAudioFile
    )
}

@Composable
fun VideoScreen() {
    MediaScreen(
        title = "Archivos de Video",
        extensions = VIDEO_EXTENSIONS,
        analyzeFile = ::analyzeVideoFile
    )
}

@Composable
fun ImageScreen() {
    MediaScreen(
        title = "Archivos de Imagen",
        extensions = IMAGE_EXTENSIONS,
        analyzeFile = ::analyzeImageFile
    )
}

@Composable
fun TextScreen() {
    // Los subtítulos suelen estar embebidos en contenedores de audio/video
    MediaScreen(
        title = "Subtítulos (en contenedores A/V)",
        extensions = TEXT_CONTAINER_EXTENSIONS,
        analyzeFile = ::analyzeTextFile
    )
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

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "ogg", "opus", "flac", "m4a", "aac", "wav", "wma", "alac",
    "mka", "mid", "midi", "amr", "awb", "ac3", "eac3", "dts",
    "ape", "wv", "aiff", "aif", "caf", "dsf", "dff"
)

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "mov", "avi", "3gp", "3g2", "ts", "m2ts", "mts",
    "flv", "wmv", "mpg", "mpeg", "m4v", "vob", "ogv", "divx", "xvid",
    "rm", "rmvb", "asf", "m2v", "m4p"
)

private val IMAGE_EXTENSIONS = setOf(
    // Raster comunes
    "jpg", "jpeg", "png", "gif", "bmp", "webp",
    // Modernos / HDR
    "heic", "heif", "avif", "jxl",
    // TIFF
    "tiff", "tif",
    // RAW de cámaras
    "dng", "cr2", "cr3", "nef", "arw", "orf", "rw2", "raf", "pef", "srw",
    // Íconos y otros
    "ico", "cur", "pcx", "tga", "ppm", "pgm", "pbm", "pam"
)

// Los subtítulos están embebidos en contenedores de audio/video
private val TEXT_CONTAINER_EXTENSIONS = AUDIO_EXTENSIONS + VIDEO_EXTENSIONS

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

        // Información general
        result["Formato"] = mediaInfo.getGeneral("Format")
        result["Duración"] = formatDuration(mediaInfo.getGeneral("Duration"))
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral("FileSize").toLongOrNull() ?: 0L)
        result["Bitrate total"] = formatBitrate(mediaInfo.getGeneral("OverallBitRate"))

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
        try {
            mediaInfo.destroy()
        } catch (_: Throwable) {}
        null
    }
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

        // Información general
        result["Formato"] = mediaInfo.getGeneral("Format")
        result["Duración"] = formatDuration(mediaInfo.getGeneral("Duration"))
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral("FileSize").toLongOrNull() ?: 0L)
        result["Bitrate total"] = formatBitrate(mediaInfo.getGeneral("OverallBitRate"))

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
        try {
            mediaInfo.destroy()
        } catch (_: Throwable) {}
        null
    }
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

        // Información general del contenedor
        result["Formato"] = mediaInfo.getGeneral("Format")
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral("FileSize").toLongOrNull() ?: 0L)

        // Streams de imagen
        val imageCount = mediaInfo.countStreams(StreamKind.IMAGE)
        result["Streams de imagen"] = imageCount.toString()

        if (imageCount > 0) {
            // Formato
            result["Codec"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT)
            result["Codec (string)"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_STRING)
            result["Perfil"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_PROFILE)
            result["Compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.FORMAT_COMPRESSION)
            
            // Tipo (exclusivo de Image)
            val type = mediaInfo.get(StreamKind.IMAGE, 0, Image.TYPE)
            if (type.isNotEmpty()) result["Tipo"] = type
            
            // Dimensiones
            val width = mediaInfo.get(StreamKind.IMAGE, 0, Image.WIDTH)
            val height = mediaInfo.get(StreamKind.IMAGE, 0, Image.HEIGHT)
            result["Resolución"] = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x${height}" else ""
            result["Aspect ratio"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.DISPLAY_ASPECT_RATIO_STRING)
            result["Pixel aspect ratio"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.PIXEL_ASPECT_RATIO)
            
            // Color
            result["Color space"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOR_SPACE)
            result["Chroma subsampling"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.CHROMA_SUBSAMPLING)
            result["Bit depth"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.BIT_DEPTH)
            result["Rango de color"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOUR_RANGE)
            result["Primarios"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COLOUR_PRIMARIES)
            result["Transfer"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.TRANSFER_CHARACTERISTICS)
            result["Matrix coef."] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MATRIX_COEFFICIENTS)
            
            // HDR
            val hdrFormat = mediaInfo.get(StreamKind.IMAGE, 0, Image.HDR_FORMAT)
            if (hdrFormat.isNotEmpty()) {
                result["HDR format"] = hdrFormat
                result["MaxCLL"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MAX_CLL)
                result["MaxFALL"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MAX_FALL)
                result["Mastering primaries"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MASTERING_DISPLAY_COLOR_PRIMARIES)
                result["Mastering luminance"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.MASTERING_DISPLAY_LUMINANCE)
            }
            
            // Compresión
            result["Modo compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COMPRESSION_MODE)
            result["Ratio compresión"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.COMPRESSION_RATIO)
            
            // Tamaño del stream
            result["Tamaño stream"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.STREAM_SIZE_STRING)
            
            // Metadata
            result["Encoder"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.ENCODED_LIBRARY)
            result["Fecha"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.ENCODED_DATE)
            result["Idioma"] = mediaInfo.get(StreamKind.IMAGE, 0, Image.LANGUAGE)
            
            // Summary (exclusivo de Image)
            val summary = mediaInfo.get(StreamKind.IMAGE, 0, Image.SUMMARY)
            if (summary.isNotEmpty()) result["Resumen"] = summary
        }

        mediaInfo.close()
        mediaInfo.destroy()
        
        Log.i(TAG, "=== ✅ Image analysis complete for ${file.name} ===")
        result
    } catch (e: Throwable) {
        Log.e(TAG, "❌ Exception during image analysis", e)
        try {
            mediaInfo.destroy()
        } catch (_: Throwable) {}
        null
    }
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

        // Información general del contenedor
        result["Formato contenedor"] = mediaInfo.getGeneral("Format")
        result["Tamaño"] = formatFileSize(mediaInfo.getGeneral("FileSize").toLongOrNull() ?: 0L)
        result["Duración total"] = formatDuration(mediaInfo.getGeneral("Duration"))

        // Streams de texto
        val textCount = mediaInfo.countStreams(StreamKind.TEXT)
        result["Streams de subtítulos"] = textCount.toString()

        if (textCount == 0) {
            result["Estado"] = "⚠️ Este archivo no contiene streams de subtítulos"
        } else {
            // Mostrar información de cada stream de texto
            for (i in 0 until textCount) {
                val streamLabel = if (textCount > 1) "Subtítulo #${i + 1}" else "Subtítulo"
                
                // Formato
                val format = mediaInfo.get(StreamKind.TEXT, i, Text.FORMAT)
                val formatString = mediaInfo.get(StreamKind.TEXT, i, Text.FORMAT_STRING)
                if (format.isNotEmpty()) {
                    result["$streamLabel - Formato"] = if (formatString.isNotEmpty()) formatString else format
                }
                
                // Codec ID
                val codecId = mediaInfo.get(StreamKind.TEXT, i, Text.CODEC_ID)
                if (codecId.isNotEmpty()) {
                    result["$streamLabel - Codec ID"] = codecId
                }
                
                // Muxing mode
                val muxingMode = mediaInfo.get(StreamKind.TEXT, i, Text.MUXING_MODE)
                if (muxingMode.isNotEmpty()) {
                    result["$streamLabel - Muxing"] = muxingMode
                }
                
                // Idioma
                val language = mediaInfo.get(StreamKind.TEXT, i, Text.LANGUAGE_STRING)
                if (language.isNotEmpty()) {
                    result["$streamLabel - Idioma"] = language
                }
                
                // Título
                val title = mediaInfo.get(StreamKind.TEXT, i, Text.TITLE)
                if (title.isNotEmpty()) {
                    result["$streamLabel - Título"] = title
                }
                
                // Flags
                val isDefault = mediaInfo.get(StreamKind.TEXT, i, Text.DEFAULT)
                if (isDefault.isNotEmpty() && isDefault == "Yes") {
                    result["$streamLabel - Por defecto"] = "✓ Sí"
                }
                
                val isForced = mediaInfo.get(StreamKind.TEXT, i, Text.FORCED)
                if (isForced.isNotEmpty() && isForced == "Yes") {
                    result["$streamLabel - Forzado"] = "✓ Sí"
                }
                
                // Geometría (en caracteres)
                val width = mediaInfo.get(StreamKind.TEXT, i, Text.WIDTH)
                val height = mediaInfo.get(StreamKind.TEXT, i, Text.HEIGHT)
                if (width.isNotEmpty() && height.isNotEmpty()) {
                    result["$streamLabel - Dimensiones"] = "${width}x${height} caracteres"
                }
                
                // Métricas de subtítulos
                val linesCount = mediaInfo.get(StreamKind.TEXT, i, Text.LINES_COUNT)
                if (linesCount.isNotEmpty()) {
                    result["$streamLabel - Líneas totales"] = linesCount
                }
                
                val eventsTotal = mediaInfo.get(StreamKind.TEXT, i, Text.EVENTS_TOTAL)
                if (eventsTotal.isNotEmpty()) {
                    result["$streamLabel - Eventos"] = eventsTotal
                }
                
                // Duración del stream de texto
                val duration = mediaInfo.get(StreamKind.TEXT, i, Text.DURATION_STRING)
                if (duration.isNotEmpty()) {
                    result["$streamLabel - Duración"] = duration
                }
                
                // Tamaño del stream
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
        try {
            mediaInfo.destroy()
        } catch (_: Throwable) {}
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