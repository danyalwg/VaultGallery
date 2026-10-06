package com.danyal.vaultgallery

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Build
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.ui.VaultBackground
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color as ComposeColor
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal enum class CreationType(val title: String) { GIF("GIF"), COLLAGE("Collage"), MOVIE("Movie") }
internal data class CreativeRequest(val type: CreationType, val media: List<GalleryMedia>)
internal enum class MovieRatio(val label: String, val widthScale: Int, val heightScale: Int) {
    SQUARE("1:1", 1, 1), LANDSCAPE("16:9", 16, 9), PORTRAIT("9:16", 9, 16)
}
internal enum class MovieTransition(val label: String) { NONE("None"), CROSSFADE("Crossfade") }
internal data class MovieOptions(
    val ratio: MovieRatio = MovieRatio.SQUARE,
    val height: Int = 720,
    val secondsPerImage: Int = 2,
    val maxVideoSeconds: Int = 8,
    val transition: MovieTransition = MovieTransition.CROSSFADE,
    val title: String = "",
    val backgroundAudioUri: Uri? = null,
    val backgroundAudioName: String = "",
    val voiceoverUri: Uri? = null,
    val voiceoverName: String = "",
    val fadeAudio: Boolean = true,
    val duckMusicForVoiceover: Boolean = true,
)

/** A movie source owns neither its bitmap nor URI. Callers keep them valid until encoding ends. */
internal data class CreativeMovieSource(
    val still: Bitmap? = null,
    val videoUri: Uri? = null,
    val durationMs: Long = 0L,
)
internal enum class CollageLayout(val label: String) {
    GRID("Grid"), FEATURE("Feature"), MOSAIC("Mosaic"), ROWS("Rows"), COLUMNS("Columns"), FREEFORM("Freeform")
}
internal enum class CollageRatio(val label: String, val width: Int, val height: Int) {
    PORTRAIT("Portrait", 2000, 2600),
    LANDSCAPE("Landscape", 2600, 2000),
    SQUARE("1:1", 2400, 2400),
    THREE_FOUR("3:4", 1800, 2400),
    NINE_SIXTEEN("9:16", 1350, 2400),
    FULL_SCREEN("Full screen", 1080, 2400),
    WALLPAPER("Wallpaper", 1440, 2960),
    WIDE("16:9", 2560, 1440),
    STORY("Story", 1080, 1920),
    TABLET("Tablet", 2560, 1600),
    CUSTOM("Custom", 0, 0),
}
internal data class CollageOptions(
    val layout: CollageLayout = CollageLayout.GRID,
    val ratio: CollageRatio = CollageRatio.SQUARE,
    val gap: Int = 12,
    val roundness: Int = 0,
    val featureFraction: Float = .6f,
    val background: Int = Color.BLACK,
    val gradientBackground: Boolean = false,
    val backgroundEnd: Int = 0xFF3F51D9.toInt(),
    val photoBackground: Boolean = false,
    val borderWidth: Int = 0,
    val borderColor: Int = Color.WHITE,
    val freeformPlacements: List<CollagePlacement> = emptyList(),
    val customWidth: Int = 2400,
    val customHeight: Int = 2400,
    val overlayText: String = "",
    val overlayTextColor: Int = Color.WHITE,
    val overlayTextSize: Int = 72,
    val sticker: String = "",
    val drawingEnabled: Boolean = false,
    val drawingColor: Int = Color.WHITE,
    val drawingWidth: Int = 12,
    val drawingStrokes: List<CollageStroke> = emptyList(),
) {
    val outputWidth: Int get() = if (ratio == CollageRatio.CUSTOM) customWidth.coerceIn(320, 8192) else ratio.width
    val outputHeight: Int get() = if (ratio == CollageRatio.CUSTOM) customHeight.coerceIn(320, 8192) else ratio.height
}

internal data class CollagePoint(val x: Float, val y: Float)
internal data class CollageStroke(val points: List<CollagePoint>, val color: Int, val width: Int)

internal data class CollagePlacement(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val rotation: Float = 0f,
)

internal data class CreativeOptions(
    val collage: CollageOptions = CollageOptions(),
    val gifSize: Int = 480,
    val gifDelayMs: Int = 750,
    val movie: MovieOptions = MovieOptions(),
)

private enum class CollagePanel(val label: String) { LAYOUT("Layout"), BACKGROUND("Background"), BORDER("Border"), RATIO("Ratio"), OVERLAYS("Overlays") }

/** Normalized rectangles shared by the on-screen preview and full-resolution renderer. */
internal fun collagePlacements(count: Int, options: CollageOptions): List<RectF> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(RectF(0f, 0f, 1f, 1f))
    return when (options.layout) {
        CollageLayout.GRID -> {
            val columns = ceil(sqrt(count * options.outputWidth.toDouble() / options.outputHeight)).toInt().coerceIn(1, count)
            val rows = ceil(count / columns.toDouble()).toInt()
            (0 until count).map { index ->
                val row = index / columns
                val rowCount = min(columns, count - row * columns)
                val col = index % columns
                RectF(col.toFloat() / rowCount, row.toFloat() / rows, (col + 1f) / rowCount, (row + 1f) / rows)
            }
        }
        CollageLayout.FEATURE -> {
            val first = options.featureFraction.coerceIn(.35f, .75f)
            if (options.outputWidth < options.outputHeight) {
                listOf(RectF(0f, 0f, 1f, first)) + (1 until count).map { index ->
                    val columns = min(count - 1, 2)
                    val rows = ceil((count - 1) / columns.toDouble()).toInt()
                    val row = (index - 1) / columns
                    val rowCount = min(columns, count - 1 - row * columns)
                    val col = (index - 1) % columns
                    RectF(col.toFloat() / rowCount, first + (1f - first) * row / rows, (col + 1f) / rowCount, first + (1f - first) * (row + 1) / rows)
                }
            } else {
                listOf(RectF(0f, 0f, first, 1f)) + (1 until count).map { index ->
                    RectF(first, (index - 1f) / (count - 1), 1f, index.toFloat() / (count - 1))
                }
            }
        }
        CollageLayout.ROWS -> (0 until count).map { RectF(0f, it.toFloat() / count, 1f, (it + 1f) / count) }
        CollageLayout.COLUMNS -> (0 until count).map { RectF(it.toFloat() / count, 0f, (it + 1f) / count, 1f) }
        CollageLayout.MOSAIC -> when (count) {
            2 -> listOf(RectF(0f, 0f, .62f, 1f), RectF(.62f, 0f, 1f, 1f))
            3 -> listOf(RectF(0f, 0f, .62f, 1f), RectF(.62f, 0f, 1f, .5f), RectF(.62f, .5f, 1f, 1f))
            else -> listOf(RectF(0f, 0f, .62f, .62f), RectF(.62f, 0f, 1f, .38f), RectF(.62f, .38f, 1f, .62f)) +
                (3 until count).map { index ->
                    val cells = count - 3
                    RectF((index - 3f) / cells, .62f, (index - 2f) / cells, 1f)
                }
        }
        CollageLayout.FREEFORM -> if (options.freeformPlacements.size == count) {
            options.freeformPlacements.map { RectF(it.left, it.top, it.right, it.bottom) }
        } else (0 until count).map { index ->
            val columns = if (count < 5) 2 else 3
            val rows = ceil(count / columns.toDouble()).toInt()
            val col = index % columns
            val row = index / columns
            val inset = .035f
            val tilt = if (index % 2 == 0) inset else -inset
            RectF(
                (col.toFloat() / columns + inset + tilt).coerceIn(0f, .9f),
                (row.toFloat() / rows + inset - tilt).coerceIn(0f, .9f),
                ((col + 1f) / columns - inset + tilt).coerceIn(.1f, 1f),
                ((row + 1f) / rows - inset - tilt).coerceIn(.1f, 1f),
            )
        }
    }
}

internal fun defaultFreeformPlacements(count: Int, options: CollageOptions): List<CollagePlacement> =
    collagePlacements(count, options.copy(freeformPlacements = emptyList())).mapIndexed { index, rect ->
        CollagePlacement(rect.left, rect.top, rect.right, rect.bottom, if (index % 2 == 0) -4f else 4f)
    }

internal fun transformCollagePlacement(
    placement: CollagePlacement,
    panX: Float,
    panY: Float,
    zoom: Float,
    rotation: Float,
): CollagePlacement {
    val oldWidth = placement.right - placement.left
    val oldHeight = placement.bottom - placement.top
    val width = (oldWidth * zoom).coerceIn(.12f, 1f)
    val height = (oldHeight * zoom).coerceIn(.12f, 1f)
    val centerX = ((placement.left + placement.right) / 2f + panX).coerceIn(width / 2f, 1f - width / 2f)
    val centerY = ((placement.top + placement.bottom) / 2f + panY).coerceIn(height / 2f, 1f - height / 2f)
    return CollagePlacement(
        centerX - width / 2f,
        centerY - height / 2f,
        centerX + width / 2f,
        centerY + height / 2f,
        (placement.rotation + rotation).coerceIn(-180f, 180f),
    )
}

@Composable
internal fun CollageDesignControls(
    options: CollageOptions,
    onChange: (CollageOptions) -> Unit,
    onShuffle: (() -> Unit)? = null,
) {
    var panel by remember { mutableStateOf(CollagePanel.LAYOUT) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        when (panel) {
            CollagePanel.LAYOUT -> {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    onShuffle?.let { OutlinedButton(onClick = it) { Text("Shuffle") } }
                    CollageLayout.entries.forEach { layout ->
                        CollagePresetButton(layout, options.layout == layout) { onChange(options.copy(layout = layout)) }
                    }
                }
                if (options.layout == CollageLayout.FEATURE) {
                    Text("Featured photo size: ${(options.featureFraction * 100).toInt()}%", color = VaultSecondary)
                    Slider(options.featureFraction, { onChange(options.copy(featureFraction = it)) }, valueRange = .35f.. .75f)
                }
            }
            CollagePanel.BACKGROUND -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(options.photoBackground, { onChange(options.copy(photoBackground = !options.photoBackground)) }, label = { Text("Photo") })
                    FilterChip(options.gradientBackground, { onChange(options.copy(gradientBackground = !options.gradientBackground, photoBackground = false)) }, label = { Text("Gradient") })
                    Text(if (options.gradientBackground) "End colour" else "Colour", color = VaultSecondary, modifier = Modifier.padding(vertical = 10.dp))
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Color.BLACK, 0xFFF44336.toInt(), 0xFFFF7A21.toInt(), 0xFFFFD83D.toInt(),
                        0xFF28C7C7.toInt(), 0xFF2196F3.toInt(), 0xFF3F51D9.toInt(), Color.WHITE,
                    ).forEach { color ->
                        val selected = if (options.gradientBackground) options.backgroundEnd == color else options.background == color
                        Box(
                            Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(ComposeColor(color))
                                .then(if (selected) Modifier.border(3.dp, VaultPrimary, androidx.compose.foundation.shape.CircleShape) else Modifier)
                                .clickable {
                                    onChange(if (options.gradientBackground) options.copy(backgroundEnd = color, photoBackground = false) else options.copy(background = color, photoBackground = false))
                                },
                        )
                    }
                }
            }
            CollagePanel.BORDER -> Column {
                Text("Roundness: ${options.roundness}%", color = VaultSecondary)
                Slider(options.roundness.toFloat(), { onChange(options.copy(roundness = it.toInt())) }, valueRange = 0f..100f)
                Text("Margins: ${options.gap} px", color = VaultSecondary)
                Slider(options.gap.toFloat(), { onChange(options.copy(gap = it.toInt())) }, valueRange = 0f..40f)
                Text("Border: ${options.borderWidth} px", color = VaultSecondary)
                Slider(options.borderWidth.toFloat(), { onChange(options.copy(borderWidth = it.toInt())) }, valueRange = 0f..24f)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Color.WHITE, Color.BLACK, 0xFFFFD83D.toInt(), 0xFF2196F3.toInt(), 0xFFF44336.toInt()).forEach { color ->
                        Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(ComposeColor(color))
                            .then(if (options.borderColor == color) Modifier.border(3.dp, VaultPrimary, androidx.compose.foundation.shape.CircleShape) else Modifier)
                            .clickable { onChange(options.copy(borderColor = color)) })
                    }
                }
            }
            CollagePanel.RATIO -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CollageRatio.entries.forEach { ratio ->
                        FilterChip(options.ratio == ratio, { onChange(options.copy(ratio = ratio)) }, label = { Text(ratio.label) })
                    }
                }
                if (options.ratio == CollageRatio.CUSTOM) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        options.customWidth.toString(),
                        { value -> value.filter(Char::isDigit).toIntOrNull()?.let { onChange(options.copy(customWidth = it.coerceIn(320, 8192))) } },
                        label = { Text("Width") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        options.customHeight.toString(),
                        { value -> value.filter(Char::isDigit).toIntOrNull()?.let { onChange(options.copy(customHeight = it.coerceIn(320, 8192))) } },
                        label = { Text("Height") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
            }
            CollagePanel.OVERLAYS -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    options.overlayText,
                    { onChange(options.copy(overlayText = it.take(100))) },
                    label = { Text("Caption") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("", "❤", "★", "☺", "✨", "📍", "🎉").forEach { sticker ->
                        FilterChip(options.sticker == sticker, { onChange(options.copy(sticker = sticker)) }, label = { Text(if (sticker.isBlank()) "None" else sticker) })
                    }
                }
                Text("Caption size: ${options.overlayTextSize}", color = VaultSecondary)
                Slider(options.overlayTextSize.toFloat(), { onChange(options.copy(overlayTextSize = it.toInt())) }, valueRange = 32f..160f)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(options.drawingEnabled, { onChange(options.copy(drawingEnabled = !options.drawingEnabled)) }, label = { Text(if (options.drawingEnabled) "Drawing on" else "Draw") })
                    TextButton(enabled = options.drawingStrokes.isNotEmpty(), onClick = { onChange(options.copy(drawingStrokes = emptyList())) }) { Text("Clear drawing") }
                }
                Text("Brush: ${options.drawingWidth}", color = VaultSecondary)
                Slider(options.drawingWidth.toFloat(), { onChange(options.copy(drawingWidth = it.toInt())) }, valueRange = 3f..40f)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Color.WHITE, Color.BLACK, 0xFFFFD83D.toInt(), 0xFF2196F3.toInt(), 0xFFF44336.toInt()).forEach { color ->
                        Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(ComposeColor(color))
                            .then(if (options.drawingColor == color) Modifier.border(3.dp, VaultPrimary, androidx.compose.foundation.shape.CircleShape) else Modifier)
                            .clickable { onChange(options.copy(drawingColor = color, overlayTextColor = color)) })
                    }
                }
                Text(if (options.drawingEnabled) "Draw directly on the collage preview." else "Turn on Draw to add a freehand layer.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CollagePanel.entries.forEach { destination ->
                TextButton(onClick = { panel = destination }) {
                    Text(destination.label, color = if (panel == destination) VaultPrimary else VaultSecondary)
                }
            }
        }
    }
}

@Composable
private fun CollagePresetButton(layout: CollageLayout, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(66.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val stroke = if (selected) VaultPrimary else VaultSecondary
        Box(
            Modifier.size(54.dp).border(if (selected) 2.dp else 1.dp, stroke, RoundedCornerShape(8.dp)).padding(7.dp),
        ) {
            collagePlacements(4, CollageOptions(layout = layout)).forEach { cell ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = cell.left * size.width
                            translationY = cell.top * size.height
                            scaleX = (cell.right - cell.left).coerceAtLeast(.05f)
                            scaleY = (cell.bottom - cell.top).coerceAtLeast(.05f)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        }
                        .border(1.dp, stroke, RoundedCornerShape(2.dp)),
                )
            }
        }
        Text(layout.label, color = if (selected) VaultPrimary else VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
internal fun CreativeStudioScreen(request: CreativeRequest, onBack: () -> Unit, onCreated: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var creationJob by remember { mutableStateOf<Job?>(null) }
    val candidates = remember(request) { request.media.distinctBy { it.id } }
    var selectedIds by remember(request) { mutableStateOf(candidates.take(if (request.type == CreationType.COLLAGE) 4 else 8).map { it.id }) }
    var collageOptions by remember(request) { mutableStateOf(CollageOptions()) }
    var gifSize by remember(request) { androidx.compose.runtime.mutableIntStateOf(480) }
    var gifDelayMs by remember(request) { androidx.compose.runtime.mutableIntStateOf(750) }
    var movieOptions by remember(request) { mutableStateOf(MovieOptions()) }
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingVoiceover by remember { mutableStateOf(false) }
    var voiceoverStartedAt by remember { mutableStateOf(0L) }
    var voiceoverSeconds by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    fun startVoiceoverRecording() {
        if (recordingVoiceover) return
        val output = File(context.cacheDir, "voiceovers/voiceover-${System.currentTimeMillis()}.m4a").apply {
            parentFile?.mkdirs()
        }
        runCatching {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(160_000)
            recorder.setAudioSamplingRate(48_000)
            recorder.setOutputFile(output.absolutePath)
            recorder.prepare()
            recorder.start()
            voiceRecorder = recorder
            recordingVoiceover = true
            voiceoverStartedAt = android.os.SystemClock.elapsedRealtime()
            movieOptions = movieOptions.copy(voiceoverUri = Uri.fromFile(output), voiceoverName = output.name)
        }.onFailure {
            output.delete()
            status = it.message ?: "Microphone recording could not start"
        }
    }

    fun stopVoiceoverRecording(keep: Boolean = true) {
        val uri = movieOptions.voiceoverUri
        runCatching { voiceRecorder?.stop() }
        voiceRecorder?.release()
        voiceRecorder = null
        recordingVoiceover = false
        if (!keep) {
            uri?.path?.let(::File)?.takeIf { it.isFile && it.path.startsWith(context.cacheDir.path) }?.delete()
            movieOptions = movieOptions.copy(voiceoverUri = null, voiceoverName = "")
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceoverRecording() else status = "Microphone permission is required to record a voiceover"
    }
    LaunchedEffect(recordingVoiceover, voiceoverStartedAt) {
        while (recordingVoiceover) {
            voiceoverSeconds = ((android.os.SystemClock.elapsedRealtime() - voiceoverStartedAt) / 1_000L).toInt()
            kotlinx.coroutines.delay(250)
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (recordingVoiceover) stopVoiceoverRecording(keep = true) else voiceRecorder?.release() }
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment ?: "Audio track" }
            movieOptions = movieOptions.copy(backgroundAudioUri = uri, backgroundAudioName = name)
        }
    }
    val limit = if (request.type == CreationType.COLLAGE) 9 else 16
    val usable = remember(candidates, selectedIds, limit) { selectedIds.take(limit).mapNotNull { id -> candidates.firstOrNull { it.id == id } } }
    Column(Modifier.fillMaxSize().background(VaultBackground).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White) }
            Text("Create ${request.type.title}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (request.type == CreationType.COLLAGE && usable.isNotEmpty()) {
                LaunchedEffect(collageOptions.layout, usable.size) {
                    if (collageOptions.layout == CollageLayout.FREEFORM && collageOptions.freeformPlacements.size != usable.size) {
                        collageOptions = collageOptions.copy(freeformPlacements = defaultFreeformPlacements(usable.size, collageOptions))
                    }
                }
                val backgroundModifier = if (collageOptions.gradientBackground) {
                    Modifier.background(Brush.linearGradient(listOf(ComposeColor(collageOptions.background), ComposeColor(collageOptions.backgroundEnd))))
                } else Modifier.background(ComposeColor(collageOptions.background))
                BoxWithConstraints(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)).then(backgroundModifier)) {
                    if (collageOptions.photoBackground) {
                        AsyncImage(
                            usable.first().uri,
                            "Photo background",
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(Modifier.fillMaxSize().background(ComposeColor(0x55000000)))
                    }
                    val latestOptions by rememberUpdatedState(collageOptions)
                    val density = LocalDensity.current
                    val previewWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                    val previewHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                    val gapX = maxWidth * (collageOptions.gap.toFloat() / collageOptions.outputWidth)
                    val gapY = maxHeight * (collageOptions.gap.toFloat() / collageOptions.outputHeight)
                    collagePlacements(usable.size, collageOptions).forEachIndexed { index, rect ->
                        val itemWidth = (maxWidth * rect.width() - gapX * 2).coerceAtLeast(1.dp)
                        val itemHeight = (maxHeight * rect.height() - gapY * 2).coerceAtLeast(1.dp)
                        val corner = (minOf(itemWidth.value, itemHeight.value) *
                            collageOptions.roundness.coerceIn(0, 100) / 200f).dp
                        val freeformModifier = if (collageOptions.layout == CollageLayout.FREEFORM) Modifier
                            .graphicsLayer { rotationZ = collageOptions.freeformPlacements.getOrNull(index)?.rotation ?: 0f }
                            .pointerInput(index, previewWidthPx, previewHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    val active = latestOptions.freeformPlacements.getOrNull(index) ?: return@detectTransformGestures
                                    val changed = transformCollagePlacement(active, pan.x / previewWidthPx, pan.y / previewHeightPx, zoom, rotationChange)
                                    val placements = latestOptions.freeformPlacements.toMutableList().apply { this[index] = changed }
                                    collageOptions = latestOptions.copy(freeformPlacements = placements)
                                }
                            } else Modifier
                        val cellShape = RoundedCornerShape(corner)
                        AsyncImage(
                            usable[index].uri, usable[index].name,
                            Modifier.offset(maxWidth * rect.left + gapX, maxHeight * rect.top + gapY)
                                .width(itemWidth)
                                .height(itemHeight)
                                .then(freeformModifier)
                                .clip(cellShape)
                                .then(if (collageOptions.borderWidth > 0) Modifier.border(collageOptions.borderWidth.dp, ComposeColor(collageOptions.borderColor), cellShape) else Modifier),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    ComposeCanvas(
                        Modifier.fillMaxSize().pointerInput(collageOptions.drawingEnabled, previewWidthPx, previewHeightPx) {
                            if (collageOptions.drawingEnabled) {
                                detectDragGestures(
                                    onDragStart = { point ->
                                        val active = latestOptions
                                        val stroke = CollageStroke(
                                            points = listOf(CollagePoint((point.x / previewWidthPx).coerceIn(0f, 1f), (point.y / previewHeightPx).coerceIn(0f, 1f))),
                                            color = active.drawingColor,
                                            width = active.drawingWidth,
                                        )
                                        collageOptions = active.copy(drawingStrokes = active.drawingStrokes + stroke)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val active = latestOptions
                                        val last = active.drawingStrokes.lastOrNull() ?: return@detectDragGestures
                                        val point = CollagePoint(
                                            (change.position.x / previewWidthPx).coerceIn(0f, 1f),
                                            (change.position.y / previewHeightPx).coerceIn(0f, 1f),
                                        )
                                        collageOptions = active.copy(drawingStrokes = active.drawingStrokes.dropLast(1) + last.copy(points = last.points + point))
                                    },
                                )
                            }
                        },
                    ) {
                        collageOptions.drawingStrokes.forEach { stroke ->
                            if (stroke.points.size == 1) {
                                val point = stroke.points.first()
                                drawCircle(ComposeColor(stroke.color), stroke.width / 2f, androidx.compose.ui.geometry.Offset(point.x * size.width, point.y * size.height))
                            } else {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    val first = stroke.points.first()
                                    moveTo(first.x * size.width, first.y * size.height)
                                    stroke.points.drop(1).forEach { point -> lineTo(point.x * size.width, point.y * size.height) }
                                }
                                drawPath(path, ComposeColor(stroke.color), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.width.toFloat(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                    }
                    if (collageOptions.sticker.isNotBlank()) Text(
                        collageOptions.sticker,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    )
                    if (collageOptions.overlayText.isNotBlank()) Text(
                        collageOptions.overlayText,
                        color = ComposeColor(collageOptions.overlayTextColor),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.BottomCenter).background(ComposeColor(0x66000000), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    usable.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}: ${item.name.take(10)}", color = androidx.compose.ui.graphics.Color.White)
                            TextButton(enabled = index > 0, onClick = { selectedIds = selectedIds.toMutableList().apply { add(index - 1, removeAt(index)) } }) { Text("←") }
                            TextButton(enabled = index < usable.lastIndex, onClick = { selectedIds = selectedIds.toMutableList().apply { add(index + 1, removeAt(index)) } }) { Text("→") }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                working -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = VaultPrimary)
                    Text(status ?: "Creating ${request.type.title}…", color = VaultSecondary)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(.72f), color = VaultPrimary)
                    Text("${(progress * 100).toInt()}%", color = androidx.compose.ui.graphics.Color.White)
                    TextButton(onClick = { creationJob?.cancel(); working = false; status = "Creation cancelled"; progress = 0f }) { Text("Cancel") }
                }
                else -> LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(candidates, key = { it.id }) { item ->
                        val selected = item.id in selectedIds
                        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).clickable {
                            selectedIds = selectedIds.toMutableList().apply {
                                if (selected) remove(item.id) else if (size < limit) add(item.id)
                            }
                        }) {
                            AsyncImage(item.uri, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            if (selected) Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x55000000))) {
                                Text("${selectedIds.indexOf(item.id) + 1}", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.TopStart).padding(7.dp).background(VaultPrimary, RoundedCornerShape(18.dp)).padding(horizontal = 7.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (request.type == CreationType.COLLAGE) CollageDesignControls(
                collageOptions,
                onChange = { collageOptions = it },
                onShuffle = { selectedIds = selectedIds.shuffled() },
            )
            if (request.type == CreationType.COLLAGE && collageOptions.layout == CollageLayout.FREEFORM) {
                Text("Drag to move. Pinch to resize and rotate each photo.", color = VaultSecondary)
            }
            if (request.type == CreationType.GIF) {
                Text("Frame speed: ${"%.1f".format(gifDelayMs / 1000f)} s", color = VaultSecondary)
                Slider(gifDelayMs.toFloat(), { gifDelayMs = it.toInt() }, valueRange = 100f..2_000f)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(360, 480, 720).forEach { size -> FilterChip(gifSize == size, { gifSize = size }, label = { Text("${size}p") }) }
                }
            }
            if (request.type == CreationType.MOVIE) {
                Text("Each picture: ${movieOptions.secondsPerImage} s", color = VaultSecondary)
                Slider(movieOptions.secondsPerImage.toFloat(), { movieOptions = movieOptions.copy(secondsPerImage = it.toInt()) }, valueRange = 1f..5f, steps = 3)
                Text("Each video clip: up to ${movieOptions.maxVideoSeconds} s", color = VaultSecondary)
                Slider(movieOptions.maxVideoSeconds.toFloat(), { movieOptions = movieOptions.copy(maxVideoSeconds = it.toInt()) }, valueRange = 2f..30f, steps = 27)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MovieRatio.entries.forEach { ratio -> FilterChip(movieOptions.ratio == ratio, { movieOptions = movieOptions.copy(ratio = ratio) }, label = { Text(ratio.label) }) }
                    listOf(480, 720, 1080).forEach { height -> FilterChip(movieOptions.height == height, { movieOptions = movieOptions.copy(height = height) }, label = { Text("${height}p") }) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MovieTransition.entries.forEach { transition ->
                        FilterChip(movieOptions.transition == transition, { movieOptions = movieOptions.copy(transition = transition) }, label = { Text(transition.label) })
                    }
                }
                OutlinedTextField(
                    value = movieOptions.title,
                    onValueChange = { movieOptions = movieOptions.copy(title = it.take(80)) },
                    label = { Text("Title overlay (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { musicPicker.launch(arrayOf("audio/*")) }) {
                        Text(if (movieOptions.backgroundAudioUri == null) "Add music" else "Change music")
                    }
                    if (movieOptions.backgroundAudioUri != null) {
                        Text(
                            movieOptions.backgroundAudioName,
                            color = VaultSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        )
                        TextButton(onClick = { movieOptions = movieOptions.copy(backgroundAudioUri = null, backgroundAudioName = "") }) {
                            Text("Remove")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        if (recordingVoiceover) stopVoiceoverRecording(keep = true)
                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceoverRecording()
                        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }) {
                        Text(if (recordingVoiceover) "Stop (${voiceoverSeconds}s)" else if (movieOptions.voiceoverUri == null) "Record voiceover" else "Record again")
                    }
                    if (movieOptions.voiceoverUri != null && !recordingVoiceover) {
                        Text(movieOptions.voiceoverName, color = VaultSecondary, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        TextButton(onClick = { stopVoiceoverRecording(keep = false) }) { Text("Remove") }
                    }
                }
                if (movieOptions.backgroundAudioUri != null || movieOptions.voiceoverUri != null) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(movieOptions.fadeAudio, { movieOptions = movieOptions.copy(fadeAudio = !movieOptions.fadeAudio) }, label = { Text("Audio fade") })
                        FilterChip(movieOptions.duckMusicForVoiceover, { movieOptions = movieOptions.copy(duckMusicForVoiceover = !movieOptions.duckMusicForVoiceover) }, label = { Text("Duck music") })
                    }
                }
            }
            val explanation = when (request.type) {
                CreationType.GIF -> "Creates a looping GIF from photos, videos, or a mixed selection of up to 16 items."
                CreationType.COLLAGE -> "Photos are used directly; a selected video contributes its poster frame."
                CreationType.MOVIE -> "Creates an H.264 MP4 from selected photos and real video clips, in the numbered order."
            }
            Text(explanation, color = VaultSecondary)
            Text("${usable.size} item${if (usable.size == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
            Button(enabled = usable.isNotEmpty() && !working, onClick = {
                working = true
                status = "Loading pictures"
                progress = 0f
                creationJob = scope.launch {
                    val result = runCatching {
                        if (request.type == CreationType.MOVIE) {
                            status = "Rendering photos and video clips"
                            progress = .1f
                            return@runCatching withContext(Dispatchers.IO) {
                                createMixedMovie(context, usable, movieOptions) { completed, total ->
                                    progress = .1f + .85f * completed.toFloat() / total.coerceAtLeast(1).toFloat()
                                }
                            }
                        }
                            val bitmaps = mutableListOf<Bitmap>()
                            usable.forEachIndexed { index, item ->
                                val decoded = withContext(Dispatchers.IO) {
                                    if (request.type == CreationType.GIF && item.kind == MediaKind.VIDEO) {
                                        decodeVideoGifFrames(context, item.uri, (16 / usable.size.coerceAtLeast(1)).coerceIn(2, 8))
                                    } else if (item.kind == MediaKind.VIDEO) {
                                        listOfNotNull(decodeVideoPoster(context, item.uri, 1400))
                                    } else listOfNotNull(decodeCreativeBitmap(context, item.uri, if (request.type == CreationType.COLLAGE) 1400 else 720))
                                }
                                bitmaps += decoded
                                progress = ((index + 1f) / (usable.size + 1f) * .7f).coerceAtMost(.7f)
                            }
                            if (request.type == CreationType.GIF && bitmaps.size > 16) {
                                bitmaps.drop(16).forEach(Bitmap::recycle)
                                while (bitmaps.size > 16) bitmaps.removeAt(bitmaps.lastIndex)
                            }
                            require(bitmaps.isNotEmpty()) { "No readable pictures were selected" }
                            try {
                                status = "Rendering ${request.type.title.lowercase()}"
                                progress = .78f
                                withContext(Dispatchers.IO) {
                                    when (request.type) {
                                        CreationType.COLLAGE -> createCollage(context, bitmaps, collageOptions)
                                        CreationType.GIF -> createAnimatedGif(context, bitmaps, gifSize, gifDelayMs)
                                        CreationType.MOVIE -> error("Movie rendering should use mixed-media encoder")
                                    }
                                }
                            } finally { bitmaps.forEach { it.recycle() } }
                    }
                    working = false
                    progress = if (result.isSuccess) 1f else 0f
                    result.onSuccess(onCreated).onFailure { if (it is kotlinx.coroutines.CancellationException) status = "Creation cancelled" else status = it.message ?: "Creation failed" }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(when (request.type) { CreationType.GIF -> Icons.Outlined.AutoAwesome; CreationType.COLLAGE -> Icons.Outlined.GridView; CreationType.MOVIE -> Icons.Outlined.Movie }, null)
                Spacer(Modifier.size(8.dp)); Text("Create ${request.type.title}")
            }
            status?.takeIf { !working }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Extracts a short, evenly sampled clip for the viewer's Create GIF action. */
internal fun decodeVideoGifFrames(context: Context, uri: Uri, frameCount: Int = 16): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        val clipMs = durationMs.coerceAtMost(6_000L)
        val count = frameCount.coerceIn(2, 16)
        buildList {
            repeat(count) { index ->
                val timeUs = clipMs * 1_000L * index / (count - 1).coerceAtLeast(1)
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)?.let { add(it) }
            }
        }
    } finally {
        retriever.release()
    }
}

internal fun decodeVideoPoster(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val longest = max(frame.width, frame.height)
        if (longest <= maxDimension) frame else {
            val scale = maxDimension.toFloat() / longest
            Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt().coerceAtLeast(1), (frame.height * scale).toInt().coerceAtLeast(1), true)
                .also { frame.recycle() }
        }
    } finally {
        retriever.release()
    }
}

private fun decodeCreativeBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) sample *= 2
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }
}

private fun createCollage(context: Context, sources: List<Bitmap>, options: CollageOptions): Uri {
    val output = renderCollageBitmap(sources, options)
    return try {
        insertCreation(context, "Collage_${System.currentTimeMillis()}.jpg", "image/jpeg", "Pictures/Vault Gallery Creations") { stream ->
            check(output.compress(Bitmap.CompressFormat.JPEG, 95, stream)) { "Could not encode collage" }
        }
    } finally { output.recycle() }
}

internal fun renderCollageBitmap(sources: List<Bitmap>, options: CollageOptions = CollageOptions()): Bitmap {
    val count = min(9, sources.size)
    require(count > 0) { "Select at least one image" }
    val output = Bitmap.createBitmap(options.outputWidth, options.outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output).apply {
        if (options.gradientBackground) {
            drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, output.width.toFloat(), output.height.toFloat(), options.background, options.backgroundEnd, Shader.TileMode.CLAMP)
            })
        } else drawColor(options.background)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    if (options.photoBackground) {
        drawCenterCrop(canvas, sources.first(), Rect(0, 0, output.width, output.height), paint)
        canvas.drawColor(0x55000000)
    }
    collagePlacements(count, options).forEachIndexed { index, area ->
        val inset = options.gap.coerceIn(0, 40)
        val target = Rect(
            (area.left * output.width).toInt() + inset,
            (area.top * output.height).toInt() + inset,
            (area.right * output.width).toInt() - inset,
            (area.bottom * output.height).toInt() - inset,
        )
        if (target.width() > 0 && target.height() > 0) {
            val corner = min(target.width(), target.height()) * (options.roundness.coerceIn(0, 100) / 200f)
            canvas.save()
            if (options.layout == CollageLayout.FREEFORM) {
                canvas.rotate(options.freeformPlacements.getOrNull(index)?.rotation ?: 0f, target.exactCenterX(), target.exactCenterY())
            }
            canvas.clipPath(Path().apply { addRoundRect(RectF(target), corner, corner, Path.Direction.CW) })
            drawCenterCrop(canvas, sources[index], target, paint)
            if (options.borderWidth > 0) {
                canvas.drawRoundRect(RectF(target), corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = options.borderColor
                    style = Paint.Style.STROKE
                    strokeWidth = (options.borderWidth * 2).toFloat()
                })
            }
            canvas.restore()
        }
    }
    options.drawingStrokes.forEach { stroke ->
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color
            style = Paint.Style.STROKE
            strokeWidth = stroke.width.toFloat() * output.width / 1000f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            canvas.drawCircle(point.x * output.width, point.y * output.height, strokePaint.strokeWidth / 2f, Paint(strokePaint).apply { style = Paint.Style.FILL })
        } else if (stroke.points.size > 1) {
            val path = Path().apply {
                val first = stroke.points.first()
                moveTo(first.x * output.width, first.y * output.height)
                stroke.points.drop(1).forEach { point -> lineTo(point.x * output.width, point.y * output.height) }
            }
            canvas.drawPath(path, strokePaint)
        }
    }
    if (options.sticker.isNotBlank()) {
        canvas.drawText(options.sticker.take(4), output.width * .88f, output.height * .15f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = output.width * .1f
            textAlign = Paint.Align.CENTER
        })
    }
    if (options.overlayText.isNotBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = options.overlayTextColor
            textSize = options.overlayTextSize.toFloat() * output.width / 1080f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val baseline = output.height * .91f
        val bounds = Rect()
        textPaint.getTextBounds(options.overlayText, 0, options.overlayText.length, bounds)
        canvas.drawRoundRect(
            output.width / 2f - bounds.width() / 2f - 28f,
            baseline - bounds.height() - 24f,
            output.width / 2f + bounds.width() / 2f + 28f,
            baseline + 24f,
            18f,
            18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 },
        )
        canvas.drawText(options.overlayText, output.width / 2f, baseline, textPaint)
    }
    return output
}

private fun createAnimatedGif(context: Context, sources: List<Bitmap>, size: Int = 480, delayMs: Int = 750): Uri = insertCreation(
    context, "Animation_${System.currentTimeMillis()}.gif", "image/gif", "Pictures/Vault Gallery Creations",
) { stream ->
    writeAnimatedGif(stream, sources, size, delayMs)
}

internal fun writeAnimatedGif(stream: OutputStream, sources: List<Bitmap>, size: Int = 480, delayMs: Int = 750) {
    val outputSize = size.coerceIn(240, 1080)
    val frames = sources.take(16).map { source ->
        Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888).also { target ->
            val canvas = Canvas(target).apply { drawColor(Color.BLACK) }
            drawCenterCrop(canvas, source, Rect(0, 0, outputSize, outputSize), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
    }
    try { SimpleGifEncoder(stream, outputSize, outputSize, (delayMs / 10).coerceIn(2, 500)).encode(frames) } finally { frames.forEach { it.recycle() } }
}

private fun createSlideshowMovie(context: Context, sources: List<Bitmap>, options: MovieOptions = MovieOptions()): Uri {
    val height = options.height.coerceIn(360, 1080)
    val width = ((height.toFloat() * options.ratio.widthScale / options.ratio.heightScale).toInt() / 2 * 2).coerceAtLeast(2)
    val fps = 15
    val secondsPerImage = options.secondsPerImage.coerceIn(1, 5)
    val uri = createPendingMedia(context, "Movie_${System.currentTimeMillis()}.mp4", "video/mp4", "Movies/Vault Gallery Creations", video = true)
    val descriptor = context.contentResolver.openFileDescriptor(uri, "rw") ?: error("Could not open movie destination")
    try {
        encodeSlideshowMovie(descriptor.fileDescriptor, sources, width, height, fps, secondsPerImage)
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    } catch (error: Throwable) {
        context.contentResolver.delete(uri, null, null)
        throw error
    } finally { descriptor.close() }
}

private suspend fun createMixedMovie(
    context: Context,
    media: List<GalleryMedia>,
    options: MovieOptions,
    onProgress: (Int, Int) -> Unit = { _, _ -> },
): Uri {
    val usable = media.take(32)
    require(usable.isNotEmpty()) { "No readable media was selected" }
    val plan = buildMovieComposition(usable, options)
    val uri = createPendingMedia(context, "Movie_${System.currentTimeMillis()}.mp4", "video/mp4", "Movies/Vault Gallery Creations", video = true)
    val output = File(context.cacheDir, "movie-compositions/${System.nanoTime()}.mp4")
    return try {
        onProgress(0, usable.size)
        exportMovieComposition(context, plan, output)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w")?.use { destination ->
                output.inputStream().use { source -> source.copyTo(destination, 1024 * 1024) }
            } ?: error("Could not open movie destination")
        }
        onProgress(usable.size, usable.size)
        context.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            put(MediaStore.MediaColumns.WIDTH, plan.width)
            put(MediaStore.MediaColumns.HEIGHT, plan.height)
            put(MediaStore.Video.VideoColumns.DURATION, plan.durationMs)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1_000L)
        }, null, null)
        uri
    } catch (error: Throwable) {
        context.contentResolver.delete(uri, null, null)
        throw error
    } finally {
        output.delete()
    }
}

/** Copies an encoded video and a selected audio stream into one MP4. Audio loops at sample
 * boundaries and is clipped to the movie duration. Neither source is modified. */
internal fun muxMovieWithBackgroundAudio(
    context: Context,
    silentVideo: File,
    audioUri: Uri,
    destination: java.io.FileDescriptor,
    videoDurationMs: Long,
) {
    val videoExtractor = MediaExtractor()
    val audioExtractor = MediaExtractor()
    val audioDescriptor = context.contentResolver.openFileDescriptor(audioUri, "r")
        ?: error("The selected audio track could not be opened")
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    try {
        videoExtractor.setDataSource(silentVideo.absolutePath)
        audioExtractor.setDataSource(audioDescriptor.fileDescriptor)
        val videoSourceTrack = (0 until videoExtractor.trackCount).firstOrNull { index ->
            videoExtractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: error("The rendered movie did not contain a video track")
        val audioSourceTrack = (0 until audioExtractor.trackCount).firstOrNull { index ->
            audioExtractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("The selected file does not contain an audio track")
        val videoFormat = videoExtractor.getTrackFormat(videoSourceTrack)
        val audioFormat = audioExtractor.getTrackFormat(audioSourceTrack)
        muxer = MediaMuxer(destination, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val outputVideoTrack = muxer.addTrack(videoFormat)
        val outputAudioTrack = runCatching { muxer.addTrack(audioFormat) }
            .getOrElse { error("This audio format cannot be stored in an MP4 movie") }
        muxer.start()
        muxerStarted = true

        fun capacity(format: MediaFormat): Int = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceIn(64 * 1024, 16 * 1024 * 1024)
        } else 2 * 1024 * 1024

        val videoBuffer = ByteBuffer.allocateDirect(capacity(videoFormat))
        val info = MediaCodec.BufferInfo()
        videoExtractor.selectTrack(videoSourceTrack)
        while (true) {
            videoBuffer.clear()
            val size = videoExtractor.readSampleData(videoBuffer, 0)
            if (size < 0) break
            val flags = if (videoExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else 0
            info.set(0, size, videoExtractor.sampleTime.coerceAtLeast(0L), flags)
            muxer.writeSampleData(outputVideoTrack, videoBuffer, info)
            videoExtractor.advance()
        }

        val audioBuffer = ByteBuffer.allocateDirect(capacity(audioFormat))
        val targetDurationUs = videoDurationMs.coerceAtLeast(1L) * 1_000L
        val declaredLoopUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) audioFormat.getLong(MediaFormat.KEY_DURATION) else 0L
        audioExtractor.selectTrack(audioSourceTrack)
        var loopBaseUs = 0L
        while (loopBaseUs < targetDurationUs) {
            audioExtractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            var firstSampleUs = -1L
            var lastRelativeUs = -1L
            while (true) {
                audioBuffer.clear()
                val size = audioExtractor.readSampleData(audioBuffer, 0)
                if (size < 0) break
                val sourceTimeUs = audioExtractor.sampleTime
                if (sourceTimeUs < 0L) break
                if (firstSampleUs < 0L) firstSampleUs = sourceTimeUs
                val relativeUs = (sourceTimeUs - firstSampleUs).coerceAtLeast(0L)
                val outputTimeUs = loopBaseUs + relativeUs
                if (outputTimeUs >= targetDurationUs) break
                val flags = if (audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                info.set(0, size, outputTimeUs, flags)
                muxer.writeSampleData(outputAudioTrack, audioBuffer, info)
                lastRelativeUs = relativeUs
                if (!audioExtractor.advance()) break
            }
            val measuredLoopUs = (lastRelativeUs + 20_000L).coerceAtLeast(0L)
            val loopDurationUs = max(declaredLoopUs, measuredLoopUs)
            if (loopDurationUs <= 0L) error("The selected audio track has no readable samples")
            loopBaseUs += loopDurationUs
        }
    } finally {
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        videoExtractor.release()
        audioExtractor.release()
        audioDescriptor.close()
    }
}

/** Streaming mixed-media movie encoder. Video frames are decoded one at a time, so adding a clip
 * does not retain the whole clip in memory. Returns the exact encoded timeline duration. */
internal suspend fun encodeMixedMovie(
    context: Context,
    descriptor: java.io.FileDescriptor,
    sources: List<CreativeMovieSource>,
    width: Int = 720,
    height: Int = 720,
    fps: Int = 15,
    options: MovieOptions = MovieOptions(),
    onProgress: (Int, Int) -> Unit = { _, _ -> },
): Long {
    data class RuntimeSource(
        val source: CreativeMovieSource,
        val retriever: MediaMetadataRetriever?,
        val frameCount: Int,
        val durationUs: Long,
    )

    val runtime = sources.take(16).mapNotNull { source ->
        source.still?.let {
            return@mapNotNull RuntimeSource(source, null, fps * options.secondsPerImage.coerceIn(1, 5), 0L)
        }
        val uri = source.videoUri ?: return@mapNotNull null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val measuredMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: source.durationMs
            val durationMs = measuredMs.coerceAtLeast(1L).coerceAtMost(options.maxVideoSeconds.coerceIn(2, 30) * 1_000L)
            RuntimeSource(source, retriever, (durationMs * fps / 1_000L).toInt().coerceAtLeast(1), durationMs * 1_000L)
        } catch (_: Throwable) {
            retriever.release()
            null
        }
    }
    require(runtime.isNotEmpty()) { "No movie frames could be decoded" }
    val transitionFrames = if (options.transition == MovieTransition.CROSSFADE) (fps / 2).coerceAtLeast(1) else 0
    val totalFrames = runtime.sumOf { it.frameCount } + transitionFrames * (runtime.size - 1).coerceAtLeast(0)

    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        setInteger(MediaFormat.KEY_BIT_RATE, if (height >= 1080) 8_000_000 else 4_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }
    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val muxer = MediaMuxer(descriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var track = -1
    var muxerStarted = false
    var frameIndex = 0L

    fun render(source: RuntimeSource, index: Int): Bitmap {
        val raw = source.source.still ?: source.retriever?.getFrameAtTime(
            if (source.frameCount <= 1) 0L else source.durationUs * index.coerceIn(0, source.frameCount - 1) / source.frameCount,
            MediaMetadataRetriever.OPTION_CLOSEST,
        ) ?: error("A selected video frame could not be decoded")
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
            val canvas = Canvas(output).apply { drawColor(Color.BLACK) }
            drawCenterCrop(canvas, raw, Rect(0, 0, width, height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            val title = options.title.trim()
            if (title.isNotEmpty()) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = (width / 18f).coerceIn(26f, 72f)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(7f, 0f, 2f, Color.BLACK)
                }
                val baseline = height - textPaint.textSize * 1.15f
                canvas.drawText(title.take(80), width / 2f, baseline, textPaint)
            }
            if (raw !== source.source.still) raw.recycle()
        }
    }

    fun blend(from: Bitmap, to: Bitmap, amount: Float): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
        val canvas = Canvas(output)
        canvas.drawBitmap(from, 0f, 0f, null)
        canvas.drawBitmap(to, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = (amount.coerceIn(0f, 1f) * 255).toInt() })
    }

    fun queue(bitmap: Bitmap) {
        val yuv = bitmapToI420(bitmap)
        var queued = false
        while (!queued) {
            val inputIndex = codec.dequeueInputBuffer(20_000)
            if (inputIndex >= 0) {
                codec.getInputBuffer(inputIndex)!!.apply { clear(); put(yuv) }
                codec.queueInputBuffer(inputIndex, 0, yuv.size, frameIndex * 1_000_000L / fps, 0)
                frameIndex++
                queued = true
            }
            val state = drainEncoder(codec, muxer, track, muxerStarted)
            track = state.first
            muxerStarted = state.second
        }
        onProgress(frameIndex.toInt(), totalFrames)
    }

    var previousEnd: Bitmap? = null
    try {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        runtime.forEachIndexed { sourceIndex, source ->
            currentCoroutineContext().ensureActive()
            if (sourceIndex > 0 && transitionFrames > 0) {
                val first = render(source, 0)
                try {
                    repeat(transitionFrames) { transitionIndex ->
                        currentCoroutineContext().ensureActive()
                        val transition = blend(requireNotNull(previousEnd), first, (transitionIndex + 1f) / (transitionFrames + 1f))
                        try { queue(transition) } finally { transition.recycle() }
                    }
                } finally { first.recycle() }
            }
            var lastFrame: Bitmap? = null
            repeat(source.frameCount) { sourceFrame ->
                currentCoroutineContext().ensureActive()
                val frame = render(source, sourceFrame)
                try { queue(frame) } finally {
                    lastFrame?.recycle()
                    lastFrame = if (sourceFrame == source.frameCount - 1) frame.copy(Bitmap.Config.ARGB_8888, false) else null
                    frame.recycle()
                }
            }
            previousEnd?.recycle()
            previousEnd = lastFrame
        }
        var eosQueued = false
        while (!eosQueued) {
            currentCoroutineContext().ensureActive()
            val eos = codec.dequeueInputBuffer(20_000)
            if (eos >= 0) {
                codec.queueInputBuffer(eos, 0, 0, frameIndex * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                eosQueued = true
            } else {
                val state = drainEncoder(codec, muxer, track, muxerStarted)
                track = state.first
                muxerStarted = state.second
            }
        }
        var ended = false
        while (!ended) {
            currentCoroutineContext().ensureActive()
            val info = MediaCodec.BufferInfo()
            val index = codec.dequeueOutputBuffer(info, 20_000)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted -> {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                index >= 0 -> {
                    codec.getOutputBuffer(index)?.let { buffer ->
                        if (info.size > 0 && muxerStarted) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buffer, info)
                        }
                    }
                    ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(index, false)
                }
            }
        }
        return frameIndex * 1_000L / fps
    } finally {
        previousEnd?.recycle()
        runtime.forEach { it.retriever?.release() }
        runCatching { codec.stop() }
        codec.release()
        if (muxerStarted) runCatching { muxer.stop() }
        muxer.release()
    }
}

internal fun encodeSlideshowMovie(
    descriptor: java.io.FileDescriptor,
    sources: List<Bitmap>,
    width: Int = 720,
    height: Int = 720,
    fps: Int = 15,
    secondsPerImage: Int = 2,
) {
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        setInteger(MediaFormat.KEY_BIT_RATE, 3_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }
    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val muxer = MediaMuxer(descriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var track = -1
    var muxerStarted = false
    try {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        var frameIndex = 0L
        val framesPerImage = fps * secondsPerImage
        sources.take(16).forEach { source ->
            val square = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawCenterCrop(Canvas(square).apply { drawColor(Color.BLACK) }, source, Rect(0, 0, width, height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            val yuv = bitmapToI420(square)
            square.recycle()
            repeat(framesPerImage) {
                var queued = false
                while (!queued) {
                    val inputIndex = codec.dequeueInputBuffer(20_000)
                    if (inputIndex >= 0) {
                        codec.getInputBuffer(inputIndex)!!.apply { clear(); put(yuv) }
                        codec.queueInputBuffer(inputIndex, 0, yuv.size, frameIndex * 1_000_000L / fps, 0)
                        frameIndex++
                        queued = true
                    }
                    val state = drainEncoder(codec, muxer, track, muxerStarted)
                    track = state.first; muxerStarted = state.second
                }
            }
        }
        var eosQueued = false
        while (!eosQueued) {
            val eos = codec.dequeueInputBuffer(20_000)
            if (eos >= 0) {
                codec.queueInputBuffer(eos, 0, 0, frameIndex * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                eosQueued = true
            } else {
                val state = drainEncoder(codec, muxer, track, muxerStarted)
                track = state.first; muxerStarted = state.second
            }
        }
        var ended = false
        while (!ended) {
            val info = MediaCodec.BufferInfo()
            val index = codec.dequeueOutputBuffer(info, 20_000)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> if (!muxerStarted) { track = muxer.addTrack(codec.outputFormat); muxer.start(); muxerStarted = true }
                index >= 0 -> {
                    codec.getOutputBuffer(index)?.let { buffer ->
                        if (info.size > 0 && muxerStarted) { buffer.position(info.offset); buffer.limit(info.offset + info.size); muxer.writeSampleData(track, buffer, info) }
                    }
                    ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(index, false)
                }
            }
        }
    } finally {
        runCatching { codec.stop() }; codec.release()
        if (muxerStarted) runCatching { muxer.stop() }
        muxer.release()
    }
}

private fun drainEncoder(codec: MediaCodec, muxer: MediaMuxer, currentTrack: Int, started: Boolean): Pair<Int, Boolean> {
    var track = currentTrack
    var muxerStarted = started
    while (true) {
        val info = MediaCodec.BufferInfo()
        val index = codec.dequeueOutputBuffer(info, 0)
        when {
            index == MediaCodec.INFO_TRY_AGAIN_LATER -> return track to muxerStarted
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> if (!muxerStarted) { track = muxer.addTrack(codec.outputFormat); muxer.start(); muxerStarted = true }
            index >= 0 -> {
                codec.getOutputBuffer(index)?.let { buffer ->
                    if (info.size > 0 && muxerStarted) { buffer.position(info.offset); buffer.limit(info.offset + info.size); muxer.writeSampleData(track, buffer, info) }
                }
                codec.releaseOutputBuffer(index, false)
            }
        }
    }
}

private fun bitmapToI420(bitmap: Bitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val output = ByteArray(width * height * 3 / 2)
    var yIndex = 0
    var uIndex = width * height
    var vIndex = uIndex + width * height / 4
    for (y in 0 until height) for (x in 0 until width) {
        val color = pixels[y * width + x]
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        output[yIndex++] = ((66 * r + 129 * g + 25 * b + 128 shr 8) + 16).coerceIn(0, 255).toByte()
        if (y % 2 == 0 && x % 2 == 0) {
            output[uIndex++] = ((-38 * r - 74 * g + 112 * b + 128 shr 8) + 128).coerceIn(0, 255).toByte()
            output[vIndex++] = ((112 * r - 94 * g - 18 * b + 128 shr 8) + 128).coerceIn(0, 255).toByte()
        }
    }
    return output
}

private fun drawCenterCrop(canvas: Canvas, source: Bitmap, destination: Rect, paint: Paint) {
    val srcRatio = source.width.toFloat() / source.height
    val dstRatio = destination.width().toFloat() / destination.height()
    val src = if (srcRatio > dstRatio) {
        val width = (source.height * dstRatio).toInt(); Rect((source.width - width) / 2, 0, (source.width + width) / 2, source.height)
    } else {
        val height = (source.width / dstRatio).toInt(); Rect(0, (source.height - height) / 2, source.width, (source.height + height) / 2)
    }
    canvas.drawBitmap(source, src, destination, paint)
}

private fun insertCreation(context: Context, name: String, mime: String, path: String, writer: (OutputStream) -> Unit): Uri {
    val uri = createPendingMedia(context, name, mime, path, video = false)
    try {
        context.contentResolver.openOutputStream(uri, "w")?.use { writer(BufferedOutputStream(it)) } ?: error("Could not write creation")
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    } catch (error: Throwable) {
        context.contentResolver.delete(uri, null, null)
        throw error
    }
}

private fun createPendingMedia(context: Context, name: String, mime: String, path: String, video: Boolean): Uri {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime)
        put(MediaStore.MediaColumns.RELATIVE_PATH, path); put(MediaStore.MediaColumns.IS_PENDING, 1)
        put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000L)
    }
    val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    return context.contentResolver.insert(collection, values) ?: error("Could not create output")
}

internal class SimpleGifEncoder(private val output: OutputStream, private val width: Int, private val height: Int, private val delayCs: Int) {
    fun encode(frames: List<Bitmap>) {
        output.write("GIF89a".toByteArray(Charsets.US_ASCII)); writeShort(width); writeShort(height)
        output.write(0xF7); output.write(0); output.write(0)
        repeat(256) { index ->
            val r = (index shr 5) * 255 / 7; val g = (index shr 2 and 7) * 255 / 7; val b = (index and 3) * 255 / 3
            output.write(r); output.write(g); output.write(b)
        }
        output.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B)); output.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        output.write(byteArrayOf(3, 1, 0, 0, 0))
        frames.forEach { frame ->
            output.write(byteArrayOf(0x21, 0xF9.toByte(), 4, 0)); writeShort(delayCs); output.write(0); output.write(0)
            output.write(0x2C); writeShort(0); writeShort(0); writeShort(width); writeShort(height); output.write(0)
            output.write(8)
            val indexed = rgb332(frame)
            writeLzw(indexed)
        }
        output.write(0x3B); output.flush()
    }

    private fun rgb332(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(width * height); bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return ByteArray(pixels.size) { i ->
            val c = pixels[i]; ((Color.red(c) and 0xE0) or (Color.green(c) shr 3 and 0x1C) or (Color.blue(c) shr 6)).toByte()
        }
    }

    private fun writeLzw(indexed: ByteArray) {
        val packed = ArrayList<Byte>()
        var bitBuffer = 0
        var bitCount = 0
        fun code(value: Int) {
            bitBuffer = bitBuffer or (value shl bitCount); bitCount += 9
            while (bitCount >= 8) { packed += (bitBuffer and 0xFF).toByte(); bitBuffer = bitBuffer ushr 8; bitCount -= 8 }
        }
        var index = 0
        while (index < indexed.size) {
            code(256)
            val end = min(index + 250, indexed.size)
            while (index < end) code(indexed[index++].toInt() and 0xFF)
        }
        code(257)
        if (bitCount > 0) packed += (bitBuffer and 0xFF).toByte()
        var offset = 0
        while (offset < packed.size) {
            val count = min(255, packed.size - offset); output.write(count)
            repeat(count) { output.write(packed[offset++].toInt() and 0xFF) }
        }
        output.write(0)
    }

    private fun writeShort(value: Int) { output.write(value and 0xFF); output.write(value shr 8 and 0xFF) }
}
