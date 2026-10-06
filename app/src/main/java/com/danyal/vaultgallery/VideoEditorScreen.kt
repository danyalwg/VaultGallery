@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.danyal.vaultgallery

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.danyal.vaultgallery.core.GalleryLogic
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.editor.EditProjectStore
import com.danyal.vaultgallery.editor.VideoClip
import com.danyal.vaultgallery.editor.VideoEditProject
import com.danyal.vaultgallery.editor.VideoProjectState
import com.danyal.vaultgallery.editor.VideoTrack
import com.danyal.vaultgallery.editor.VideoTrackKind
import com.danyal.vaultgallery.editor.SubtitleCue
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

internal enum class VideoEditorSaveMode { REPLACE, COPY }
private val VideoEditorYellow = Color(0xFFFFD60A)
private enum class VideoTool(val label: String) { TRIM("Trim"), SPEED("Speed"), LOOK("Filters"), TUNE("Tone"), FRAME("Crop"), CAPTIONS("Captions"), QUALITY("Size") }
internal enum class VideoLook(val label: String) {
    ORIGINAL("Original"), AUTO("Auto"), VIVID("Vivid"), PUNCH("Punch"), WARM("Warm"), COOL("Cool"),
    MONO("Mono"), INVERT("Negative"), FADE("Fade"), DRAMATIC("Dramatic"), SUNSET("Sunset"), NIGHT("Night")
}
internal enum class VideoFrame(val label: String, val ratio: Float?) { ORIGINAL("Free", null), SQUARE("1:1", 1f), FOUR_THREE("4:3", 4f / 3f), NINE_SIXTEEN("9:16", 9f / 16f), SIXTEEN_NINE("16:9", 16f / 9f) }

internal data class VideoCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    companion object { val Full = VideoCropRect(0f, 0f, 1f, 1f) }

    val isFull: Boolean get() = left <= .0001f && top <= .0001f && right >= .9999f && bottom >= .9999f
}

private data class VideoPreviewInfo(
    val bitmap: android.graphics.Bitmap?,
    val width: Int,
    val height: Int,
    val mimeType: String? = null,
    val bitrate: Long = 0L,
    val hdr: Boolean = false,
)

private enum class HdrExportMode(val label: String) { PRESERVE("Keep HDR"), SDR("Convert to SDR") }

internal data class VideoExportEstimate(val width: Int, val height: Int, val durationMs: Long, val bytes: Long)

internal fun estimateVideoExport(
    sourceWidth: Int,
    sourceHeight: Int,
    sourceBitrate: Long,
    trimStartMs: Float,
    trimEndMs: Float,
    speed: Float,
    crop: VideoCropRect,
    rotation: Int,
    outputHeight: Int,
    muted: Boolean,
): VideoExportEstimate {
    val rotatedWidth = if (rotation % 180 == 0) sourceWidth else sourceHeight
    val rotatedHeight = if (rotation % 180 == 0) sourceHeight else sourceWidth
    val croppedWidth = (rotatedWidth.coerceAtLeast(1) * (crop.right - crop.left).coerceIn(.01f, 1f)).roundToInt().coerceAtLeast(1)
    val croppedHeight = (rotatedHeight.coerceAtLeast(1) * (crop.bottom - crop.top).coerceIn(.01f, 1f)).roundToInt().coerceAtLeast(1)
    val targetHeight = (if (outputHeight > 0) outputHeight else croppedHeight).coerceAtLeast(1)
    val targetWidth = (croppedWidth.toFloat() / croppedHeight * targetHeight).roundToInt().coerceAtLeast(1).let { it + (it and 1) }
    val evenHeight = targetHeight + (targetHeight and 1)
    val duration = (((trimEndMs - trimStartMs).coerceAtLeast(0f) / speed.coerceAtLeast(.05f))).toLong()
    val sourcePixels = sourceWidth.toLong().coerceAtLeast(1L) * sourceHeight.toLong().coerceAtLeast(1L)
    val targetPixels = targetWidth.toLong() * evenHeight
    val baseBitrate = sourceBitrate.takeIf { it > 0L } ?: 12_000_000L
    val videoBitrate = (baseBitrate * targetPixels / sourcePixels).coerceIn(1_000_000L, 50_000_000L)
    val audioBitrate = if (muted) 0L else 192_000L
    val bytes = ((videoBitrate + audioBitrate) * (duration / 1000.0) / 8.0).toLong().coerceAtLeast(0L)
    return VideoExportEstimate(targetWidth, evenHeight, duration, bytes)
}

private data class VideoEditState(
    val trim: ClosedFloatingPointRange<Float>,
    val speed: Float,
    val muted: Boolean,
    val rotation: Int,
    val look: VideoLook,
    val frame: VideoFrame,
    val crop: VideoCropRect,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val outputHeight: Int,
    val hdrMode: HdrExportMode,
)

@Composable
internal fun VideoEditorScreen(
    title: String,
    source: Uri,
    durationMs: Long,
    accent: Color = VaultBlue,
    onBack: () -> Unit,
    projectSource: String = source.toString(),
    onExported: (File, ExportResult, VideoEditorSaveMode) -> Unit,
) {
    val context = LocalContext.current
    val projectStore = remember(context) { EditProjectStore(context) }
    var projectId by remember(projectSource) { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var projectLoaded by remember(projectSource) { mutableStateOf(false) }
    val deviceCapabilities = remember(context) { detectDeviceMediaCapabilities(context) }
    val safeDuration = durationMs.coerceAtLeast(1_000L)
    var tool by remember { mutableStateOf(VideoTool.TRIM) }
    var trim by remember(source) { mutableStateOf(0f..safeDuration.toFloat()) }
    var speed by remember { mutableFloatStateOf(1f) }
    var muted by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }
    var look by remember { mutableStateOf(VideoLook.ORIGINAL) }
    var frame by remember { mutableStateOf(VideoFrame.ORIGINAL) }
    var crop by remember(source) { mutableStateOf(VideoCropRect.Full) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var outputHeight by remember { mutableIntStateOf(0) }
    var hdrMode by remember { mutableStateOf(HdrExportMode.PRESERVE) }
    var subtitleCues by remember(projectSource) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var requestedSaveMode by remember { mutableStateOf(VideoEditorSaveMode.COPY) }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewPosition by remember { mutableFloatStateOf(0f) }
    var trimDragging by remember { mutableStateOf(false) }
    var previewScrubInProgress by remember { mutableStateOf(false) }
    var resumeAfterPreviewScrub by remember { mutableStateOf(false) }
    var discardPrompt by remember { mutableStateOf(false) }
    var undoStack by remember(source) { mutableStateOf<List<VideoEditState>>(emptyList()) }
    var redoStack by remember(source) { mutableStateOf<List<VideoEditState>>(emptyList()) }
    var gestureStartState by remember(source) { mutableStateOf<VideoEditState?>(null) }
    val player = remember(source) {
        buildSamsungGalleryPlayer(context).apply {
            setMediaItem(MediaItem.fromUri(source))
            prepare()
        }
    }

    fun editState() = VideoEditState(trim, speed, muted, rotation, look, frame, crop, brightness, contrast, saturation, outputHeight, hdrMode)

    fun restoreEdit(state: VideoEditState) {
        trim = state.trim
        speed = state.speed
        muted = state.muted
        rotation = state.rotation
        look = state.look
        frame = state.frame
        crop = state.crop
        brightness = state.brightness
        contrast = state.contrast
        saturation = state.saturation
        outputHeight = state.outputHeight
        hdrMode = state.hdrMode
        previewPosition = previewPosition.coerceIn(state.trim.start, state.trim.endInclusive)
        player.seekTo(previewPosition.toLong())
    }

    LaunchedEffect(projectSource) {
        projectStore.loadVideo(projectSource)?.let { project ->
            projectId = project.id
            subtitleCues = project.subtitles
            val saved = project.editorState
            val start = saved.trimStartMs.coerceIn(0L, safeDuration).toFloat()
            val end = saved.trimEndMs.takeIf { it > saved.trimStartMs }
                ?.coerceIn(saved.trimStartMs + 1L, safeDuration)?.toFloat() ?: safeDuration.toFloat()
            restoreEdit(
                VideoEditState(
                    trim = start..end,
                    speed = saved.speed,
                    muted = saved.muted,
                    rotation = saved.rotation,
                    look = VideoLook.entries.firstOrNull { it.label == saved.look } ?: VideoLook.ORIGINAL,
                    frame = VideoFrame.entries.firstOrNull { it.label == saved.frame } ?: VideoFrame.ORIGINAL,
                    crop = VideoCropRect(saved.cropLeft, saved.cropTop, saved.cropRight, saved.cropBottom),
                    brightness = saved.brightness,
                    contrast = saved.contrast,
                    saturation = saved.saturation,
                    outputHeight = saved.outputHeight,
                    hdrMode = if (saved.hdrMode == "sdr") HdrExportMode.SDR else HdrExportMode.PRESERVE,
                ),
            )
        }
        projectLoaded = true
    }

    LaunchedEffect(projectLoaded, trim, speed, muted, rotation, look, frame, crop, brightness, contrast, saturation, outputHeight, hdrMode, subtitleCues) {
        if (!projectLoaded) return@LaunchedEffect
        delay(400)
        projectStore.saveVideo(
            VideoEditProject(
                id = projectId,
                title = title,
                tracks = listOf(
                    VideoTrack(
                        id = "primary",
                        kind = VideoTrackKind.VIDEO,
                        clips = listOf(VideoClip("primary", projectSource, 0, safeDuration)),
                        muted = muted,
                    ),
                ),
                width = 0,
                height = 0,
                fps = 0,
                editorState = VideoProjectState(
                    trimStartMs = trim.start.toLong(),
                    trimEndMs = trim.endInclusive.toLong(),
                    speed = speed,
                    muted = muted,
                    rotation = rotation,
                    look = look.label,
                    frame = frame.label,
                    cropLeft = crop.left,
                    cropTop = crop.top,
                    cropRight = crop.right,
                    cropBottom = crop.bottom,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    outputHeight = outputHeight,
                    hdrMode = if (hdrMode == HdrExportMode.SDR) "sdr" else "preserve",
                ),
                subtitles = subtitleCues,
            ),
        )
    }

    fun recordEdit(before: VideoEditState) {
        if (before != editState()) {
            undoStack = (undoStack + before).takeLast(30)
            redoStack = emptyList()
        }
    }

    fun discreteEdit(change: () -> Unit) {
        val before = editState()
        change()
        recordEdit(before)
    }

    fun beginContinuousEdit() {
        if (gestureStartState == null) gestureStartState = editState()
    }

    fun finishContinuousEdit() {
        gestureStartState?.let(::recordEdit)
        gestureStartState = null
    }

    val previewInfo by produceState(initialValue = VideoPreviewInfo(null, 0, 0), source) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                android.media.MediaMetadataRetriever().let { retriever ->
                    try {
                        retriever.setDataSource(context, source)
                        val rawWidth = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        val rawHeight = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        val metadataRotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                        val displayWidth = if (metadataRotation % 180 == 0) rawWidth else rawHeight
                        val displayHeight = if (metadataRotation % 180 == 0) rawHeight else rawWidth
                        val previewWidth = if (displayWidth >= displayHeight) 480 else (480f * displayWidth / displayHeight.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                        val previewHeight = if (displayHeight >= displayWidth) 480 else (480f * displayHeight / displayWidth.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                        val frame = if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, previewWidth, previewHeight)
                        } else retriever.getFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        VideoPreviewInfo(
                            frame,
                            displayWidth,
                            displayHeight,
                            primaryVideoTrackMime(context, source)
                                ?: retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                                ?: context.contentResolver.getType(source),
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L,
                            videoIsHdr(context, source),
                        )
                    } finally { retriever.release() }
                }
            }.getOrElse { VideoPreviewInfo(null, 0, 0) }
        }
    }
    val previewFrame = previewInfo.bitmap
    val sourceAspect = remember(previewFrame, rotation) {
        val bitmap = previewFrame
        val raw = when {
            previewInfo.width > 0 && previewInfo.height > 0 -> previewInfo.width.toFloat() / previewInfo.height
            bitmap != null && bitmap.height > 0 -> bitmap.width.toFloat() / bitmap.height
            else -> 16f / 9f
        }
        if (rotation % 180 == 0) raw else 1f / raw
    }
    val sourceShortSide = remember(previewInfo, rotation, crop) {
        val width = if (rotation % 180 == 0) previewInfo.width else previewInfo.height
        val height = if (rotation % 180 == 0) previewInfo.height else previewInfo.width
        minOf((width * (crop.right - crop.left)).toInt(), (height * (crop.bottom - crop.top)).toInt())
    }
    val exportEstimate = remember(previewInfo, trim, speed, crop, rotation, outputHeight, muted) {
        estimateVideoExport(
            previewInfo.width,
            previewInfo.height,
            previewInfo.bitrate,
            trim.start,
            trim.endInclusive,
            speed,
            crop,
            rotation,
            outputHeight,
            muted,
        )
    }
    LaunchedEffect(sourceShortSide, outputHeight) {
        if (outputHeight > 0 && sourceShortSide > 0 && outputHeight > sourceShortSide) outputHeight = 0
    }
    val videoEffects = remember(rotation, look, crop, brightness, contrast, saturation, outputHeight) {
        buildVideoEffects(rotation, look, crop, brightness, contrast, saturation, outputHeight)
    }
    val previewVideoEffects = remember(rotation, look, crop, brightness, contrast, saturation, tool) {
        // Crop mode deliberately shows the complete source under the editable crop window.
        // Other tools preview the actual cropped result.
        buildVideoEffects(rotation, look, if (tool == VideoTool.FRAME) VideoCropRect.Full else crop, brightness, contrast, saturation, outputHeight = 0)
    }
    LaunchedEffect(speed, muted) {
        player.setPlaybackSpeed(speed)
        player.volume = if (muted) 0f else 1f
    }
    LaunchedEffect(player) {
        while (true) {
            // Synchronise the transport UI to the display instead of polling every 50 ms. This
            // lets a 90/120 Hz device move the playhead continuously with playback and scrubbing.
            withFrameNanos { }
            previewPlaying = player.isPlaying
            if (!trimDragging) previewPosition = player.currentPosition.toFloat().coerceIn(trim.start, trim.endInclusive)
        }
    }
    LaunchedEffect(previewVideoEffects) { player.setVideoEffects(previewVideoEffects) }
    LaunchedEffect(trim, player) {
        if (!trimDragging && (player.currentPosition < trim.start || player.currentPosition > trim.endInclusive + 50f)) player.seekTo(trim.start.toLong())
        while (true) {
            if (!trimDragging && player.isPlaying && player.currentPosition >= trim.endInclusive - 40f) player.seekTo(trim.start.toLong())
            delay(60)
        }
    }
    LaunchedEffect(exporting, transformer) {
        val active = transformer ?: return@LaunchedEffect
        val holder = ProgressHolder()
        while (exporting) {
            if (active.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) progress = holder.progress
            delay(180)
        }
    }
    DisposableEffect(player) {
        onDispose { player.release(); transformer?.cancel() }
    }

    fun exportVideo(mode: VideoEditorSaveMode) {
        if (exporting) return
        deviceCapabilities.videoExportIssue(previewInfo.mimeType ?: context.contentResolver.getType(source))?.let { issue ->
            error = issue
            return
        }
        requestedSaveMode = mode
        val output = File(context.cacheDir, "editor-cache/video-${System.nanoTime()}.mp4").apply { parentFile?.mkdirs(); delete() }
        val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(trim.start.toLong()).apply {
            if (trim.endInclusive < safeDuration - 50f) setEndPositionMs(trim.endInclusive.toLong())
        }.build()
        val clipped = MediaItem.Builder().setUri(source).setClippingConfiguration(clipping).build()
        val speedProvider = object : SpeedProvider {
            override fun getSpeed(timeUs: Long): Float = speed
            override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = androidx.media3.common.C.TIME_UNSET
        }
        val editedBuilder = EditedMediaItem.Builder(clipped)
            .setRemoveAudio(shouldRemoveVideoAudio(muted, speed))
            .setEffects(Effects(buildVideoAudioProcessors(muted, speed), videoEffects))
        if (speed != 1f) editedBuilder.setSpeed(speedProvider)
        val edited = editedBuilder.build()
        val active = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, result: ExportResult) {
                    exporting = false
                    progress = 100
                    onExported(output, result, requestedSaveMode)
                }

                override fun onError(composition: androidx.media3.transformer.Composition, result: ExportResult, exception: ExportException) {
                    exporting = false
                    output.delete()
                    android.util.Log.e("VaultVideoEditor", "Video export failed", exception)
                    error = "${exception.errorCodeName}: ${exception.message ?: exception.cause?.message ?: "The device could not export this edit."}"
                }
            }).build()
        transformer = active
        exporting = true
        progress = 0
        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence.withAudioAndVideoFrom(listOf(edited))),
        ).setHdrMode(
            if (hdrMode == HdrExportMode.SDR) Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
            else Composition.HDR_MODE_KEEP_HDR,
        ).build()
        active.start(composition, output.absolutePath)
    }

    val hasChanges = trim.start > 1f || trim.endInclusive < safeDuration - 1f || speed != 1f || muted ||
        rotation != 0 || look != VideoLook.ORIGINAL || !crop.isFull || brightness != 0f ||
        contrast != 0f || saturation != 0f || outputHeight != 0 || hdrMode != HdrExportMode.PRESERVE || subtitleCues.isNotEmpty()

    fun resetVideoEdit() {
        trim = 0f..safeDuration.toFloat(); speed = 1f; muted = false; rotation = 0
        look = VideoLook.ORIGINAL; frame = VideoFrame.ORIGINAL; crop = VideoCropRect.Full
        brightness = 0f; contrast = 0f; saturation = 0f; outputHeight = 0
        hdrMode = HdrExportMode.PRESERVE
        subtitleCues = emptyList()
        player.seekTo(0)
    }

    fun requestClose() {
        if (hasChanges) discardPrompt = true else onBack()
    }

    BackHandler(onBack = ::requestClose)

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::requestClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                TextButton(enabled = hasChanges && !exporting, colors = ButtonDefaults.textButtonColors(contentColor = Color.White), onClick = { discreteEdit(::resetVideoEdit) }) { Text("Revert") }
                IconButton(enabled = undoStack.isNotEmpty(), onClick = {
                    val previous = undoStack.lastOrNull() ?: return@IconButton
                    redoStack = (redoStack + editState()).takeLast(30)
                    undoStack = undoStack.dropLast(1)
                    restoreEdit(previous)
                }) { Icon(Icons.Outlined.Undo, "Undo", tint = if (undoStack.isNotEmpty()) Color.White else VaultSecondary) }
                IconButton(enabled = redoStack.isNotEmpty(), onClick = {
                    val next = redoStack.lastOrNull() ?: return@IconButton
                    undoStack = (undoStack + editState()).takeLast(30)
                    redoStack = redoStack.dropLast(1)
                    restoreEdit(next)
                }) { Icon(Icons.Outlined.Redo, "Redo", tint = if (redoStack.isNotEmpty()) Color.White else VaultSecondary) }
                IconButton(onClick = { discreteEdit { muted = !muted } }) {
                    Icon(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, if (muted) "Restore audio" else "Mute audio", tint = Color.White)
                }
                TextButton(enabled = hasChanges && !exporting, colors = ButtonDefaults.textButtonColors(contentColor = Color.White), onClick = { exportVideo(VideoEditorSaveMode.COPY) }) { Text("Save copy") }
                TextButton(enabled = hasChanges && !exporting, colors = ButtonDefaults.textButtonColors(contentColor = Color.White), onClick = { exportVideo(VideoEditorSaveMode.REPLACE) }) { Text("Save") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().heightIn(min = 176.dp, max = 300.dp).background(Color.Black).navigationBarsPadding().padding(vertical = 8.dp)) {
                when (tool) {
                    VideoTool.TRIM -> Column(Modifier.padding(horizontal = 18.dp)) {
                        Text("${GalleryLogic.durationLabel(trim.start.toLong())}  —  ${GalleryLogic.durationLabel(trim.endInclusive.toLong())}   (${GalleryLogic.durationLabel((trim.endInclusive - trim.start).toLong())})", color = Color.White)
                        SamsungVideoFrames(
                            source = source,
                            mediaKey = "editor-$title",
                            durationHintMs = safeDuration,
                            frameCount = 12,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        )
                        Slider(
                            value = previewPosition.coerceIn(trim.start, trim.endInclusive),
                            onValueChange = { position ->
                                if (!previewScrubInProgress) {
                                    resumeAfterPreviewScrub = player.playWhenReady && player.playbackState != Player.STATE_ENDED
                                    previewScrubInProgress = true
                                    // Sync-frame seeking is the low-latency interaction path. One
                                    // exact seek is issued at release so responsiveness never
                                    // trades away the final selected frame.
                                    player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                    player.setScrubbingModeEnabled(true)
                                }
                                previewPosition = position
                                player.seekTo(position.toLong())
                                player.playWhenReady = resumeAfterPreviewScrub
                            },
                            onValueChangeFinished = {
                                player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                                player.seekTo(previewPosition.toLong())
                                if (player.isScrubbingModeEnabled) player.setScrubbingModeEnabled(false)
                                player.playWhenReady = resumeAfterPreviewScrub
                                previewScrubInProgress = false
                            },
                            valueRange = trim,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x6645454A),
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                        )
                        RangeSlider(
                            value = trim,
                            onValueChange = { range ->
                                beginContinuousEdit()
                                if (!previewScrubInProgress) {
                                    resumeAfterPreviewScrub = player.playWhenReady && player.playbackState != Player.STATE_ENDED
                                    previewScrubInProgress = true
                                    player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                    player.setScrubbingModeEnabled(true)
                                }
                                val minimum = 250f.coerceAtMost(safeDuration.toFloat())
                                val previous = trim
                                val adjusted = if (range.endInclusive - range.start < minimum) {
                                    range.start..(range.start + minimum).coerceAtMost(safeDuration.toFloat())
                                } else range
                                trimDragging = true
                                trim = adjusted
                                val target = if (abs(adjusted.endInclusive - previous.endInclusive) > abs(adjusted.start - previous.start)) {
                                    adjusted.endInclusive
                                } else adjusted.start
                                previewPosition = target
                                player.seekTo(target.toLong())
                                player.playWhenReady = resumeAfterPreviewScrub
                            },
                            onValueChangeFinished = {
                                trimDragging = false
                                player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                                player.seekTo(previewPosition.toLong())
                                if (player.isScrubbingModeEnabled) player.setScrubbingModeEnabled(false)
                                player.playWhenReady = resumeAfterPreviewScrub
                                previewScrubInProgress = false
                                finishContinuousEdit()
                            },
                            valueRange = 0f..safeDuration.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = VideoEditorYellow, activeTrackColor = VideoEditorYellow, inactiveTrackColor = Color(0xFF45454A)),
                        )
                    }
                    VideoTool.SPEED -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 2f, 4f).forEach { value -> VideoChoice("${value}×", speed == value) { discreteEdit { speed = value } } }
                    }
                    VideoTool.LOOK -> LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VideoLook.entries) { value -> VideoLookPreview(previewFrame, value, look == value) { discreteEdit { look = value } } }
                    }
                    VideoTool.TUNE -> Column(Modifier.padding(horizontal = 14.dp)) {
                        VideoSlider("Brightness", brightness, -1f..1f, { beginContinuousEdit(); brightness = it }, ::finishContinuousEdit)
                        VideoSlider("Contrast", contrast, -1f..1f, { beginContinuousEdit(); contrast = it }, ::finishContinuousEdit)
                        VideoSlider("Saturation", saturation, -100f..100f, { beginContinuousEdit(); saturation = it }, ::finishContinuousEdit)
                    }
                    VideoTool.FRAME -> Column(Modifier.padding(horizontal = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            VideoFrame.entries.forEach { value -> VideoChoice(value.label, frame == value) {
                                discreteEdit {
                                    frame = value
                                    crop = value.ratio?.let { centeredVideoCrop(sourceAspect, it) } ?: VideoCropRect.Full
                                }
                            } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 90, 180, 270).forEach { value -> VideoChoice("$value°", rotation == value) {
                                discreteEdit {
                                    rotation = value
                                    frame = VideoFrame.ORIGINAL
                                    crop = VideoCropRect.Full
                                }
                            } }
                        }
                    }
                    VideoTool.CAPTIONS -> SubtitleEditorPanel(
                        cues = subtitleCues,
                        positionMs = previewPosition.toLong(),
                        durationMs = safeDuration,
                        onChange = { subtitleCues = it },
                    )
                    VideoTool.QUALITY -> Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Original", 2160 to "4K", 1080 to "1080p", 720 to "720p", 480 to "480p")
                                .filter { (height, _) -> height == 0 || sourceShortSide <= 0 || height <= sourceShortSide }
                                .forEach { (height, label) ->
                                VideoChoice(label, outputHeight == height) { discreteEdit { outputHeight = height } }
                            }
                        }
                        if (previewInfo.hdr) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HdrExportMode.entries.forEach { mode ->
                                VideoChoice(mode.label, hdrMode == mode) { discreteEdit { hdrMode = mode } }
                            }
                        }
                        Text(
                            "${exportEstimate.width} × ${exportEstimate.height}  ·  H.264  ·  ${if (previewInfo.hdr && hdrMode == HdrExportMode.PRESERVE) "HDR" else "SDR"}  ·  ${GalleryLogic.durationLabel(exportEstimate.durationMs)}  ·  about ${GalleryLogic.fileSizeLabel(exportEstimate.bytes)}",
                            color = VaultSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    VideoTool.entries.forEach { item ->
                        VideoToolButton(
                            icon = when (item) {
                                VideoTool.TRIM -> Icons.Outlined.Crop
                                VideoTool.SPEED -> Icons.Outlined.Speed
                                VideoTool.LOOK -> Icons.Outlined.AutoFixHigh
                                VideoTool.TUNE -> Icons.Outlined.Tune
                                VideoTool.FRAME -> Icons.Outlined.RotateRight
                                VideoTool.CAPTIONS -> Icons.Outlined.ClosedCaption
                                VideoTool.QUALITY -> Icons.Outlined.HighQuality
                            },
                            label = item.label,
                            selected = tool == item,
                            accent = VideoEditorYellow,
                            onClick = { tool = item },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(factory = { PlayerView(it).apply { useController = false; this.player = player } }, modifier = Modifier.fillMaxSize())
            if (tool == VideoTool.FRAME) {
                VideoCropOverlay(
                    crop = crop,
                    videoAspect = sourceAspect,
                    onGestureStart = ::beginContinuousEdit,
                    onCropChanged = { crop = it; frame = VideoFrame.ORIGINAL },
                    onGestureFinished = ::finishContinuousEdit,
                )
            } else {
                IconButton(
                    onClick = { if (player.isPlaying) player.pause() else player.play() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(56.dp).background(Color(0xB827272B), CircleShape),
                ) {
                    Icon(if (previewPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (previewPlaying) "Pause preview" else "Play preview", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            subtitleCues.firstOrNull { previewPosition.toLong() in it.startMs until it.endMs }?.let { cue ->
                Text(
                    cue.text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (exporting) Column(Modifier.align(Alignment.Center).fillMaxWidth(.78f).background(Color(0xDD19191C), MaterialTheme.shapes.large).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(progress = { progress / 100f }, color = accent)
                Text("Exporting $progress%", color = Color.White, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = accent)
                Text("You can cancel without changing the original.", color = VaultSecondary)
                TextButton(onClick = { transformer?.cancel(); exporting = false; progress = 0 }) { Text("Cancel export") }
            }
        }
    }

    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Video editor") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
    if (discardPrompt) AlertDialog(
        onDismissRequest = { discardPrompt = false },
        title = { Text("Save your changes or discard them?") },
        dismissButton = {
            Row {
                TextButton(onClick = { discardPrompt = false }) { Text("Cancel") }
                TextButton(onClick = { discardPrompt = false; onBack() }) { Text("Discard") }
            }
        },
        confirmButton = {
            TextButton(enabled = !exporting, onClick = { discardPrompt = false; exportVideo(VideoEditorSaveMode.COPY) }) { Text("Save copy") }
        },
    )
}

/**
 * MediaStore normally reports a container type such as video/mp4. Codec capability checks need
 * the elementary video-track type (for example video/avc), otherwise a capable device can be
 * rejected before Media3 is even allowed to inspect the file.
 */
private fun primaryVideoTrackMime(context: Context, source: Uri): String? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, source, null)
        (0 until extractor.trackCount).firstNotNullOfOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                ?.takeIf { it.startsWith("video/", ignoreCase = true) }
        }
    } catch (_: Exception) {
        null
    } finally {
        extractor.release()
    }
}

private fun videoIsHdr(context: Context, source: Uri): Boolean {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, source, null)
        (0 until extractor.trackCount).any { index ->
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (!mime.startsWith("video/")) false else {
                val transfer = if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                    format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                } else -1
                transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG
            }
        }
    } catch (_: Throwable) {
        false
    } finally {
        extractor.release()
    }
}

internal enum class VideoCropHandle { NONE, MOVE, LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

internal fun centeredVideoCrop(sourceAspect: Float, targetAspect: Float): VideoCropRect {
    if (sourceAspect <= 0f || targetAspect <= 0f) return VideoCropRect.Full
    return if (sourceAspect > targetAspect) {
        val width = (targetAspect / sourceAspect).coerceIn(.08f, 1f)
        VideoCropRect((1f - width) / 2f, 0f, (1f + width) / 2f, 1f)
    } else {
        val height = (sourceAspect / targetAspect).coerceIn(.08f, 1f)
        VideoCropRect(0f, (1f - height) / 2f, 1f, (1f + height) / 2f)
    }
}

private fun fittedVideoRect(width: Float, height: Float, aspect: Float): Rect {
    if (width <= 0f || height <= 0f || aspect <= 0f) return Rect.Zero
    val containerAspect = width / height
    return if (containerAspect > aspect) {
        val videoWidth = height * aspect
        Rect((width - videoWidth) / 2f, 0f, (width + videoWidth) / 2f, height)
    } else {
        val videoHeight = width / aspect
        Rect(0f, (height - videoHeight) / 2f, width, (height + videoHeight) / 2f)
    }
}

private fun cropRectOnScreen(crop: VideoCropRect, video: Rect) = Rect(
    video.left + crop.left * video.width,
    video.top + crop.top * video.height,
    video.left + crop.right * video.width,
    video.top + crop.bottom * video.height,
)

private fun detectCropHandle(position: Offset, crop: Rect, radius: Float): VideoCropHandle {
    fun close(x: Float, y: Float) = hypot(position.x - x, position.y - y) <= radius
    return when {
        close(crop.left, crop.top) -> VideoCropHandle.TOP_LEFT
        close(crop.right, crop.top) -> VideoCropHandle.TOP_RIGHT
        close(crop.left, crop.bottom) -> VideoCropHandle.BOTTOM_LEFT
        close(crop.right, crop.bottom) -> VideoCropHandle.BOTTOM_RIGHT
        position.y in crop.top..crop.bottom && abs(position.x - crop.left) <= radius -> VideoCropHandle.LEFT
        position.x in crop.left..crop.right && abs(position.y - crop.top) <= radius -> VideoCropHandle.TOP
        position.y in crop.top..crop.bottom && abs(position.x - crop.right) <= radius -> VideoCropHandle.RIGHT
        position.x in crop.left..crop.right && abs(position.y - crop.bottom) <= radius -> VideoCropHandle.BOTTOM
        crop.contains(position) -> VideoCropHandle.MOVE
        else -> VideoCropHandle.NONE
    }
}

internal fun resizeVideoCrop(start: VideoCropRect, handle: VideoCropHandle, dx: Float, dy: Float): VideoCropRect {
    val minimum = .08f
    var left = start.left
    var top = start.top
    var right = start.right
    var bottom = start.bottom
    when (handle) {
        VideoCropHandle.NONE -> Unit
        VideoCropHandle.MOVE -> {
            val width = right - left
            val height = bottom - top
            left = (left + dx).coerceIn(0f, 1f - width)
            top = (top + dy).coerceIn(0f, 1f - height)
            right = left + width
            bottom = top + height
        }
        VideoCropHandle.LEFT -> left = (left + dx).coerceIn(0f, right - minimum)
        VideoCropHandle.TOP -> top = (top + dy).coerceIn(0f, bottom - minimum)
        VideoCropHandle.RIGHT -> right = (right + dx).coerceIn(left + minimum, 1f)
        VideoCropHandle.BOTTOM -> bottom = (bottom + dy).coerceIn(top + minimum, 1f)
        VideoCropHandle.TOP_LEFT -> {
            left = (left + dx).coerceIn(0f, right - minimum)
            top = (top + dy).coerceIn(0f, bottom - minimum)
        }
        VideoCropHandle.TOP_RIGHT -> {
            right = (right + dx).coerceIn(left + minimum, 1f)
            top = (top + dy).coerceIn(0f, bottom - minimum)
        }
        VideoCropHandle.BOTTOM_LEFT -> {
            left = (left + dx).coerceIn(0f, right - minimum)
            bottom = (bottom + dy).coerceIn(top + minimum, 1f)
        }
        VideoCropHandle.BOTTOM_RIGHT -> {
            right = (right + dx).coerceIn(left + minimum, 1f)
            bottom = (bottom + dy).coerceIn(top + minimum, 1f)
        }
    }
    return VideoCropRect(left, top, right, bottom)
}

@Composable
private fun VideoCropOverlay(
    crop: VideoCropRect,
    videoAspect: Float,
    onGestureStart: () -> Unit,
    onCropChanged: (VideoCropRect) -> Unit,
    onGestureFinished: () -> Unit,
) {
    val latestCrop by rememberUpdatedState(crop)
    Canvas(
        Modifier.fillMaxSize().pointerInput(videoAspect) {
            var activeHandle = VideoCropHandle.NONE
            var initialCrop = VideoCropRect.Full
            var totalDrag = Offset.Zero
            var videoRect = Rect.Zero
            detectDragGestures(
                onDragStart = { position ->
                    videoRect = fittedVideoRect(size.width.toFloat(), size.height.toFloat(), videoAspect)
                    activeHandle = detectCropHandle(position, cropRectOnScreen(latestCrop, videoRect), 34.dp.toPx())
                    initialCrop = latestCrop
                    totalDrag = Offset.Zero
                    if (activeHandle != VideoCropHandle.NONE) onGestureStart()
                },
                onDragCancel = {
                    if (activeHandle != VideoCropHandle.NONE) onGestureFinished()
                    activeHandle = VideoCropHandle.NONE
                },
                onDragEnd = {
                    if (activeHandle != VideoCropHandle.NONE) onGestureFinished()
                    activeHandle = VideoCropHandle.NONE
                },
                onDrag = { change, dragAmount ->
                    if (activeHandle == VideoCropHandle.NONE || videoRect.width <= 0f || videoRect.height <= 0f) return@detectDragGestures
                    change.consume()
                    totalDrag += dragAmount
                    onCropChanged(
                        resizeVideoCrop(
                            initialCrop,
                            activeHandle,
                            totalDrag.x / videoRect.width,
                            totalDrag.y / videoRect.height,
                        ),
                    )
                },
            )
        },
    ) {
        val video = fittedVideoRect(size.width, size.height, videoAspect)
        val selected = cropRectOnScreen(crop, video)
        val shade = Color(0xA8000000)
        drawRect(shade, Offset(video.left, video.top), Size(video.width, (selected.top - video.top).coerceAtLeast(0f)))
        drawRect(shade, Offset(video.left, selected.bottom), Size(video.width, (video.bottom - selected.bottom).coerceAtLeast(0f)))
        drawRect(shade, Offset(video.left, selected.top), Size((selected.left - video.left).coerceAtLeast(0f), selected.height))
        drawRect(shade, Offset(selected.right, selected.top), Size((video.right - selected.right).coerceAtLeast(0f), selected.height))
        drawRect(Color.White, selected.topLeft, selected.size, style = Stroke(width = 2.dp.toPx()))
        val grid = Color.White.copy(alpha = .55f)
        for (part in 1..2) {
            val x = selected.left + selected.width * part / 3f
            val y = selected.top + selected.height * part / 3f
            drawLine(grid, Offset(x, selected.top), Offset(x, selected.bottom), 1.dp.toPx())
            drawLine(grid, Offset(selected.left, y), Offset(selected.right, y), 1.dp.toPx())
        }
        val handleRadius = 6.dp.toPx()
        listOf(
            selected.topLeft,
            Offset(selected.center.x, selected.top),
            Offset(selected.right, selected.top),
            Offset(selected.left, selected.center.y),
            Offset(selected.right, selected.center.y),
            Offset(selected.left, selected.bottom),
            Offset(selected.center.x, selected.bottom),
            Offset(selected.right, selected.bottom),
        ).forEach { center ->
            drawCircle(Color.Black.copy(alpha = .65f), handleRadius + 2.dp.toPx(), center)
            drawCircle(Color.White, handleRadius, center)
        }
    }
}

@Composable
private fun VideoToolButton(icon: ImageVector, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier.size(46.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = if (selected) accent else Color.White, modifier = Modifier.size(27.dp)) }
        Text(label, color = if (selected) accent else Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VideoChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(if (selected) Color.White else Color(0xFF242427), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun VideoLookPreview(source: android.graphics.Bitmap?, look: VideoLook, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(76.dp).border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0xFF55555C), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (source != null) Image(
                source.asImageBitmap(),
                look.label,
                Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Crop,
                colorFilter = videoLookColorFilter(look),
            )
        }
        Text(look.label, color = if (selected) VideoEditorYellow else Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

private fun videoLookColorFilter(look: VideoLook): ColorFilter? {
    val values = when (look) {
        VideoLook.ORIGINAL -> return null
        VideoLook.AUTO -> floatArrayOf(
            1.06f, 0f, 0f, 0f, 2f, 0f, 1.06f, 0f, 0f, 2f,
            0f, 0f, 1.06f, 0f, 2f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.VIVID -> floatArrayOf(
            1.16f, -.04f, -.04f, 0f, 0f, -.04f, 1.16f, -.04f, 0f, 0f,
            -.04f, -.04f, 1.16f, 0f, 0f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.MONO -> floatArrayOf(
            .213f, .715f, .072f, 0f, 0f, .213f, .715f, .072f, 0f, 0f,
            .213f, .715f, .072f, 0f, 0f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.INVERT -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.PUNCH -> floatArrayOf(
            1.18f, 0f, 0f, 0f, 1f, 0f, 1.18f, 0f, 0f, 1f,
            0f, 0f, 1.18f, 0f, 1f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.WARM -> floatArrayOf(
            1.08f, 0f, 0f, 0f, 10f, 0f, 1f, 0f, 0f, 2f,
            0f, 0f, .86f, 0f, -5f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.COOL -> floatArrayOf(
            .88f, 0f, 0f, 0f, -4f, 0f, 1f, 0f, 0f, 1f,
            0f, 0f, 1.12f, 0f, 8f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.FADE -> floatArrayOf(
            .84f, 0f, 0f, 0f, 22f, 0f, .84f, 0f, 0f, 22f,
            0f, 0f, .84f, 0f, 22f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.DRAMATIC -> floatArrayOf(
            1.26f, 0f, 0f, 0f, -24f, 0f, 1.26f, 0f, 0f, -24f,
            0f, 0f, 1.26f, 0f, -24f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.SUNSET -> floatArrayOf(
            1.13f, 0f, 0f, 0f, 10f, 0f, 1.02f, 0f, 0f, 3f,
            0f, 0f, .80f, 0f, -7f, 0f, 0f, 0f, 1f, 0f,
        )
        VideoLook.NIGHT -> floatArrayOf(
            .80f, 0f, 0f, 0f, -8f, 0f, .92f, 0f, 0f, -5f,
            0f, 0f, 1.10f, 0f, 5f, 0f, 0f, 0f, 1f, 0f,
        )
    }
    return ColorFilter.colorMatrix(ColorMatrix(values))
}

@Composable
private fun VideoSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label ${if (range.endInclusive > 2f) "%+.0f".format(value) else "%+.2f".format(value)}", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(.34f))
        Slider(
            value,
            onValue,
            onValueChangeFinished = onFinished,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = VideoEditorYellow, activeTrackColor = VideoEditorYellow, inactiveTrackColor = Color(0xFF45454A)),
        )
    }
}

internal fun buildVideoEffects(
    rotation: Int,
    look: VideoLook,
    crop: VideoCropRect,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    outputHeight: Int,
): List<Effect> = buildList {
    if (rotation != 0) add(ScaleAndRotateTransformation.Builder().setRotationDegrees(rotation.toFloat()).build())
    if (!crop.isFull) {
        add(Crop(
            crop.left * 2f - 1f,
            crop.right * 2f - 1f,
            1f - crop.bottom * 2f,
            1f - crop.top * 2f,
        ))
    }
    when (look) {
        VideoLook.ORIGINAL -> Unit
        VideoLook.AUTO -> { add(Contrast(.08f)); add(HslAdjustment.Builder().adjustSaturation(8f).adjustLightness(2f).build()) }
        VideoLook.VIVID -> { add(Contrast(.12f)); add(HslAdjustment.Builder().adjustSaturation(28f).build()) }
        VideoLook.MONO -> add(RgbFilter.createGrayscaleFilter())
        VideoLook.INVERT -> add(RgbFilter.createInvertedFilter())
        VideoLook.PUNCH -> { add(Contrast(.18f)); add(HslAdjustment.Builder().adjustSaturation(22f).build()) }
        VideoLook.WARM -> add(HslAdjustment.Builder().adjustHue(8f).adjustSaturation(8f).adjustLightness(3f).build())
        VideoLook.COOL -> add(HslAdjustment.Builder().adjustHue(-8f).adjustSaturation(5f).build())
        VideoLook.FADE -> { add(Contrast(-.14f)); add(Brightness(.06f)); add(HslAdjustment.Builder().adjustSaturation(-12f).build()) }
        VideoLook.DRAMATIC -> { add(Contrast(.28f)); add(HslAdjustment.Builder().adjustSaturation(-8f).adjustLightness(-3f).build()) }
        VideoLook.SUNSET -> { add(Contrast(.10f)); add(HslAdjustment.Builder().adjustHue(12f).adjustSaturation(18f).adjustLightness(2f).build()) }
        VideoLook.NIGHT -> { add(Brightness(-.08f)); add(Contrast(.18f)); add(HslAdjustment.Builder().adjustHue(-12f).adjustSaturation(10f).build()) }
    }
    if (brightness != 0f) add(Brightness(brightness))
    if (contrast != 0f) add(Contrast(contrast))
    if (saturation != 0f) add(HslAdjustment.Builder().adjustSaturation(saturation).build())
    if (outputHeight > 0) add(Presentation.createForHeight(outputHeight))
}

/** Media3 1.9 can stall when speed-changing a video after removing its audio track. Keep a
 * zero-gain track for that combination; ordinary mute still removes audio completely. */
internal fun shouldRemoveVideoAudio(muted: Boolean, speed: Float): Boolean = muted && speed == 1f

internal fun buildVideoAudioProcessors(muted: Boolean, speed: Float): List<AudioProcessor> =
    if (muted && speed != 1f) listOf(GainProcessor(DefaultGainProvider.Builder(0f).build())) else emptyList()

internal fun saveExportedVideoToGallery(context: Context, file: File, source: GalleryMedia): Uri {
    val resolver = context.contentResolver
    val baseName = source.name.substringBeforeLast('.').ifBlank { "Video" }
    val originalPath = source.relativePath.trimStart('/')
    val safeAlbum = source.bucketName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Edited" }
    val writablePath = if (originalPath.substringBefore('/') in setOf("DCIM", "Movies", "Pictures")) originalPath else "Movies/$safeAlbum"
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "${baseName}_edited_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Video.Media.RELATIVE_PATH, writablePath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create the edited video")
    try {
        resolver.openOutputStream(uri, "w")?.use { output -> FileInputStream(file).use { it.copyTo(output, 1024 * 1024) } } ?: error("Could not write the edited video")
        resolver.update(uri, exportedVideoMetadata(file, includePending = Build.VERSION.SDK_INT >= 29), null, null)
        return uri
    } catch (failure: Throwable) {
        resolver.delete(uri, null, null)
        throw failure
    }
}

internal fun overwriteExportedVideo(context: Context, file: File, source: GalleryMedia): Uri {
    val resolver = context.contentResolver
    resolver.openOutputStream(source.uri, "rwt")?.use { output ->
        FileInputStream(file).use { it.copyTo(output, 1024 * 1024) }
    } ?: error("Could not open the original video for editing")
    resolver.update(source.uri, exportedVideoMetadata(file, includePending = false), null, null)
    return source.uri
}

private fun exportedVideoMetadata(file: File, includePending: Boolean): ContentValues {
    var width = 0
    var height = 0
    var duration = 0L
    runCatching {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
        put(MediaStore.MediaColumns.SIZE, file.length())
        if (width > 0) put(MediaStore.MediaColumns.WIDTH, width)
        if (height > 0) put(MediaStore.MediaColumns.HEIGHT, height)
        if (duration > 0) put(MediaStore.Video.VideoColumns.DURATION, duration)
        if (includePending) put(MediaStore.MediaColumns.IS_PENDING, 0)
    }
}
