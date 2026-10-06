@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import com.danyal.vaultgallery.core.GalleryLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull
import android.net.Uri
import android.media.MediaExtractor
import android.media.MediaFormat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import com.github.panpf.zoomimage.compose.rememberZoomState
import com.github.panpf.zoomimage.compose.zoom.zoom

// Measured from the attached Samsung device at its active 560 dpi density. Keeping these in dp
// preserves the same physical design proportions on phones with different pixel resolutions.
internal val SamsungViewerFilmstripArea = 68.dp
internal val SamsungViewerFilmstripNormal = 50.dp
internal val SamsungViewerFilmstripSelected = 60.dp
internal val SamsungViewerActionBarHeight = 56.dp
internal val SamsungViewerControllerSize = 30.dp
internal val SamsungViewerFilmstripNormalWidth = 48.dp
internal val SamsungViewerFilmstripSelectedWidth = 124.dp
internal val SamsungViewerFilmstripTimelineWidth = 220.dp
internal val SamsungViewerFilmstripFrameControlWidth = 340.dp

/** Static controls rendered on precomposed neighbouring video pages. Samsung keeps the adjacent
 * video page fully formed while it follows the finger, then attaches playback after it settles. */
@Composable
internal fun SamsungAdjacentVideoChrome(
    durationMs: Long,
    visible: Boolean,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = SamsungViewerActionBarHeight,
) {
    if (!visible) return
    Row(
        modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding + SamsungViewerFilmstripArea + 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(SamsungViewerControllerSize).clip(CircleShape).background(Color(0x9929292D)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Row(
            Modifier.height(SamsungViewerControllerSize).clip(RoundedCornerShape(18.dp))
                .background(Color(0xCC29292D)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("00:00 / ${samsungVideoTimeLabel(durationMs)}", color = Color.White)
        }
        Box(
            Modifier.size(SamsungViewerControllerSize).clip(CircleShape).background(Color(0x9929292D)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.VolumeOff, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

internal enum class GalleryAudioFocusBehavior { FOLLOW_ANDROID, CONTINUE_PLAYBACK }

internal fun galleryAudioFocusBehavior(context: android.content.Context): GalleryAudioFocusBehavior = runCatching {
    GalleryAudioFocusBehavior.valueOf(
        context.getSharedPreferences("gallery-settings", android.content.Context.MODE_PRIVATE)
            .getString("audio_focus_behavior", GalleryAudioFocusBehavior.CONTINUE_PLAYBACK.name)
            .orEmpty(),
    )
}.getOrDefault(GalleryAudioFocusBehavior.CONTINUE_PLAYBACK)

internal fun buildSamsungGalleryPlayer(context: android.content.Context): ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
        val audioFocusBehavior = galleryAudioFocusBehavior(context)
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            audioFocusBehavior == GalleryAudioFocusBehavior.FOLLOW_ANDROID,
        )
        // Keep playback running when Android changes the output route during a call, Bluetooth
        // hand-off, or headset disconnect. Volume/output routing remains under system control.
        setHandleAudioBecomingNoisy(audioFocusBehavior == GalleryAudioFocusBehavior.FOLLOW_ANDROID)
        repeatMode = Player.REPEAT_MODE_OFF
    }

internal fun samsungVideoTimeLabel(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

internal data class VideoTechnicalInfo(
    val videoCodec: String,
    val audioCodec: String?,
    val frameRate: String?,
    val colorSpace: String? = null,
    val hdr: String? = null,
    val bitrate: String? = null,
    val rotationDegrees: Int? = null,
)

internal fun readVideoTechnicalInfo(context: android.content.Context, uri: Uri): VideoTechnicalInfo {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, uri, null)
        var videoCodec = "Video"
        var audioCodec: String? = null
        var frameRate: String? = null
        var colorSpace: String? = null
        var hdr: String? = null
        var bitrate: String? = null
        var rotationDegrees: Int? = null
        repeat(extractor.trackCount) { track ->
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/")) {
                videoCodec = friendlyCodecName(mime)
                frameRate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) format.getInteger(MediaFormat.KEY_FRAME_RATE).toString() else null
                }.getOrNull()
                bitrate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) "%.1f Mbps".format(java.util.Locale.ROOT, format.getInteger(MediaFormat.KEY_BIT_RATE) / 1_000_000.0) else null
                }.getOrNull()
                rotationDegrees = runCatching {
                    if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else null
                }.getOrNull()
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    val standard = runCatching { if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) format.getInteger(MediaFormat.KEY_COLOR_STANDARD) else null }.getOrNull()
                    val transfer = runCatching { if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) format.getInteger(MediaFormat.KEY_COLOR_TRANSFER) else null }.getOrNull()
                    colorSpace = when (standard) {
                        MediaFormat.COLOR_STANDARD_BT2020 -> "BT.2020"
                        MediaFormat.COLOR_STANDARD_BT709 -> "BT.709"
                        MediaFormat.COLOR_STANDARD_BT601_NTSC, MediaFormat.COLOR_STANDARD_BT601_PAL -> "BT.601"
                        else -> null
                    }
                    hdr = when (transfer) {
                        MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 / PQ"
                        MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
                        else -> null
                    }
                }
            } else if (mime.startsWith("audio/")) {
                audioCodec = friendlyCodecName(mime)
            }
        }
        VideoTechnicalInfo(videoCodec, audioCodec, frameRate, colorSpace, hdr, bitrate, rotationDegrees)
    } finally {
        extractor.release()
    }
}

private fun friendlyCodecName(mime: String): String = when (mime.lowercase()) {
    "video/avc" -> "H.264"
    "video/hevc" -> "H.265"
    "video/av01" -> "AV1"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/x-vnd.on2.vp8" -> "VP8"
    "audio/mp4a-latm", "audio/aac" -> "AAC"
    "audio/opus" -> "Opus"
    "audio/vorbis" -> "Vorbis"
    "audio/mpeg" -> "MP3"
    else -> mime.substringAfter('/').uppercase()
}

internal fun videoResolutionClass(width: Int, height: Int): String? = when {
    minOf(width, height) >= 2160 -> "UHD"
    minOf(width, height) >= 1080 -> "FHD"
    minOf(width, height) >= 720 -> "HD"
    else -> null
}

@Composable
internal fun SamsungVideoFrames(
    source: Uri,
    mediaKey: Any,
    durationHintMs: Long,
    frameCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val duration = durationHintMs.coerceAtLeast(1L)
    Row(modifier.clip(RoundedCornerShape(3.dp))) {
        repeat(frameCount) { frame ->
            val position = duration * frame / (frameCount - 1)
            val request = remember(source, mediaKey, position) {
                ImageRequest.Builder(context)
                    .data(source)
                    .videoFrameMillis(position)
                    .memoryCacheKey("viewer-filmstrip-$mediaKey-$position")
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/** The expanded current-video tile is both the frame strip and the seek bar. Position is updated
 * directly from the finger with low-latency sync-frame seeks, followed by one exact release seek.
 * This prevents queued GOP decodes from making the image trail behind the gesture. */
@Composable
internal fun SamsungVideoSeekOverlay(
    player: ExoPlayer,
    durationHintMs: Long,
    frameControlExpanded: Boolean,
    onOpenFrameControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(1L) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var pendingSeek by remember(player) { mutableLongStateOf(0L) }
    var lastSeekDispatchMs by remember(player) { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            withFrameNanos { }
            if (!scrubbing) position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: durationHintMs.coerceAtLeast(1L)
        }
    }
    DisposableEffect(player) {
        onDispose {
            if (!player.isReleased && player.isScrubbingModeEnabled) player.setScrubbingModeEnabled(false)
        }
    }
    Canvas(modifier.pointerInput(player, duration, frameControlExpanded) {
        fun seekAt(x: Float, final: Boolean = false) {
            val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
            position = (duration * fraction).toLong()
            pendingSeek = position
            val now = android.os.SystemClock.uptimeMillis()
            if (final || now - lastSeekDispatchMs >= 16L) {
                player.setSeekParameters(if (final) SeekParameters.EXACT else SeekParameters.CLOSEST_SYNC)
                player.seekTo(pendingSeek)
                lastSeekDispatchMs = now
            }
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!frameControlExpanded) {
                // Samsung reserves precise seeking for a deliberate hold. A normal drag remains
                // available to the surrounding media filmstrip until the long-press threshold.
                val opened = withTimeoutOrNull(450L) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: return@withTimeoutOrNull false
                        if (!change.pressed) return@withTimeoutOrNull false
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > 14.dp.toPx()) {
                            return@withTimeoutOrNull false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE") false
                } ?: true
                if (opened) {
                    down.consume()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenFrameControl()
                }
                return@awaitEachGesture
            }
            scrubbing = true
            player.setScrubbingModeEnabled(true)
            seekAt(down.position.x)
            down.consume()
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    seekAt(change.position.x)
                    change.consume()
                    if (!change.pressed) break
                }
            } finally {
                seekAt((pendingSeek.toFloat() / duration.toFloat()).coerceIn(0f, 1f) * size.width, final = true)
                scrubbing = false
                if (!player.isReleased && player.isScrubbingModeEnabled) player.setScrubbingModeEnabled(false)
            }
        }
    }) {
        val x = size.width * (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        drawRect(Color(0x18000000))
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, size.height),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

// Samsung's filmstrip uses fixed crop windows rather than fitting each media aspect ratio.
// Measured on the attached device: 109 px / 560 dpi = 31 dp normally and 221 px = 63 dp
// for the centred item. This keeps portrait videos from collapsing into tiny slivers.
internal fun samsungFilmstripWidth(
    width: Int,
    height: Int,
    selected: Boolean,
    timelineExpanded: Boolean = false,
    frameControlExpanded: Boolean = false,
): Dp = when {
    selected && frameControlExpanded -> SamsungViewerFilmstripFrameControlWidth
    selected && timelineExpanded -> SamsungViewerFilmstripTimelineWidth
    selected -> SamsungViewerFilmstripSelectedWidth
    else -> SamsungViewerFilmstripNormalWidth
}

@Composable
internal fun SamsungViewerAction(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(24.dp), tint = if (enabled) Color.White else Color(0x66FFFFFF))
    }
}

@Composable
internal fun GalleryVideoPlayer(
    player: ExoPlayer,
    bottomPadding: Int = 112,
    accent: Color = Color.White,
    inline: Boolean = false,
    onCaptureFrame: (() -> Unit)? = null,
    onControlsVisibilityChanged: (Boolean) -> Unit = {},
) {
    var currentPosition by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    var muted by remember(player) { mutableStateOf(false) }
    var controls by remember(player) { mutableStateOf(true) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var pendingSeek by remember(player) { mutableLongStateOf(0L) }
    var lastSeekDispatchMs by remember(player) { mutableLongStateOf(0L) }
    var speedMenu by remember { mutableStateOf(false) }
    var speed by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    LaunchedEffect(controls) { onControlsVisibilityChanged(controls) }
    LaunchedEffect(player) {
        while (true) {
            withFrameNanos { }
            if (!scrubbing) currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0 } ?: 0L
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        }
    }
    LaunchedEffect(controls, playing) {
        if (controls && playing) { delay(3_000); controls = false }
    }
    DisposableEffect(player) {
        onDispose { if (!player.isReleased && player.isScrubbingModeEnabled) player.setScrubbingModeEnabled(false) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(player) {
        detectTapGestures(
            onTap = { controls = !controls },
            onDoubleTap = { point ->
                val delta = if (point.x < size.width / 2f) -10_000L else 10_000L
                player.seekTo((player.currentPosition + delta).coerceIn(0L, duration.coerceAtLeast(0L)))
                controls = true
            },
        )
    }) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) } }, modifier = Modifier.fillMaxSize())
        AnimatedVisibility(controls, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x30000000))) {
                if (!inline) Row(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)) }, modifier = Modifier.size(58.dp)) {
                        Icon(Icons.Outlined.Replay10, "Back 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = {
                        if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
                        playing = player.isPlaying
                    }, modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xAA1E1E22))) {
                        Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    IconButton(onClick = { player.seekTo((player.currentPosition + 10_000L).coerceAtMost(duration.coerceAtLeast(0L))) }, modifier = Modifier.size(58.dp)) {
                        Icon(Icons.Outlined.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
                if (inline) {
                    Row(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = bottomPadding.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onCaptureFrame != null) IconButton(onClick = onCaptureFrame, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xAA29292D))) {
                            Icon(Icons.Outlined.PhotoCamera, "Capture video frame", tint = Color.White)
                        } else androidx.compose.foundation.layout.Spacer(Modifier.size(50.dp))
                        Row(
                            Modifier.weight(1f).padding(horizontal = 10.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xCC29292D)).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
                            }, modifier = Modifier.size(42.dp)) {
                                Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White)
                            }
                            Slider(
                                value = currentPosition.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
                                onValueChange = {
                                    if (!scrubbing) { scrubbing = true; player.setScrubbingModeEnabled(true); player.setSeekParameters(SeekParameters.CLOSEST_SYNC) }
                                    currentPosition = it.toLong(); pendingSeek = currentPosition
                                    val now = android.os.SystemClock.uptimeMillis()
                                    if (now - lastSeekDispatchMs >= 16L) { player.seekTo(pendingSeek); lastSeekDispatchMs = now }
                                },
                                onValueChangeFinished = { player.setSeekParameters(SeekParameters.EXACT); player.seekTo(pendingSeek); scrubbing = false; player.setScrubbingModeEnabled(false) },
                                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                                modifier = Modifier.weight(1f).height(32.dp),
                            )
                            Text("${GalleryLogic.durationLabel(currentPosition)} / ${GalleryLogic.durationLabel(duration)}", color = Color.White)
                        }
                        IconButton(onClick = { muted = !muted; player.volume = if (muted) 0f else 1f }, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xAA29292D))) {
                            Icon(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, if (muted) "Unmute" else "Mute", tint = accent)
                        }
                    }
                } else Column(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = bottomPadding.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xCC18181C)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Slider(
                        value = currentPosition.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
                        onValueChange = {
                            if (!scrubbing) { scrubbing = true; player.setScrubbingModeEnabled(true); player.setSeekParameters(SeekParameters.CLOSEST_SYNC) }
                            currentPosition = it.toLong(); pendingSeek = currentPosition
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastSeekDispatchMs >= 16L) { player.seekTo(pendingSeek); lastSeekDispatchMs = now }
                        },
                        onValueChangeFinished = { player.setSeekParameters(SeekParameters.EXACT); player.seekTo(pendingSeek); scrubbing = false; player.setScrubbingModeEnabled(false) },
                        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(GalleryLogic.durationLabel(currentPosition), color = Color.White)
                        Text(" / ${GalleryLogic.durationLabel(duration)}", color = Color.LightGray, modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { speedMenu = true }) { Icon(Icons.Outlined.Speed, "Playback speed", tint = accent) }
                            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f).forEach { option -> DropdownMenuItem(
                                    text = { Text(if (option == 1f) "Normal" else "${option}×") },
                                    onClick = { speed = option; player.setPlaybackSpeed(option); speedMenu = false },
                                ) }
                            }
                        }
                        Text(if (speed == 1f) "1×" else "${speed}×", color = Color.White)
                        IconButton(onClick = { muted = !muted; player.volume = if (muted) 0f else 1f }) {
                            Icon(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, if (muted) "Unmute" else "Mute", tint = accent)
                        }
                    }
                }
            }
        }
    }
}

/** Samsung Gallery's viewer has one inline player state. The filmstrip remains the navigation
 * surface while this compact controller owns playback; opening a second "promoted" state made
 * playback, seeking and back navigation diverge from the reference app. */
@Composable
internal fun SamsungInlineVideoPlayer(
    player: ExoPlayer,
    mediaKey: Any,
    durationHintMs: Long,
    chromeVisible: Boolean,
    muted: Boolean,
    bottomPadding: Int,
    onChromeToggle: () -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    onCaptureFrame: (() -> Unit)? = null,
) {
    val videoZoomState = rememberZoomState()
    var currentPosition by remember(player, mediaKey) { mutableLongStateOf(0L) }
    var duration by remember(player, mediaKey) { mutableLongStateOf(durationHintMs.coerceAtLeast(0L)) }
    var playing by remember(player, mediaKey) { mutableStateOf(false) }
    LaunchedEffect(player, mediaKey) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = (player.duration.takeIf { it > 0 } ?: durationHintMs).coerceAtLeast(1L)
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            delay(16)
        }
    }
    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
        player.repeatMode = Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(player, mediaKey) {
        player.setSeekParameters(SeekParameters.EXACT)
        videoZoomState.zoomable.reset()
    }
    LaunchedEffect(videoZoomState) {
        snapshotFlow { videoZoomState.zoomable.transform.scaleX > videoZoomState.zoomable.minScale * 1.01f }
            .distinctUntilChanged()
            .collect(onZoomChanged)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply {
                this.player = player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            } },
            update = { it.player = player },
            // Use the same mature zoom engine as still images. It provides pinch, double-tap,
            // pan, damping and—critically—edge-aware gesture handoff to HorizontalPager.
            modifier = Modifier.fillMaxSize().zoom(
                zoomable = videoZoomState.zoomable,
                onTap = { onChromeToggle() },
            ),
        )
        // Media3's scrubbing decoder is the sole preview surface. A second Coil request for every
        // 80 ms bucket caused blank/reload flashes and let the preview fall behind the finger.
        // The caller now seeks this PlayerView directly while scrubbing mode is active.

        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 16.dp).padding(bottom = bottomPadding.dp + SamsungViewerFilmstripArea + 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier.size(SamsungViewerControllerSize).clip(CircleShape).background(Color(0x9929292D))
                        .then(if (onCaptureFrame != null) Modifier.clickable { onCaptureFrame() } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (onCaptureFrame != null) Icon(Icons.Outlined.PhotoCamera, "Capture video frame", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Row(
                    Modifier.height(SamsungViewerControllerSize).clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC29292D)).clickable {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED || currentPosition >= duration - 50L) player.seekTo(0)
                                player.play()
                            }
                        }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        if (playing) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "${samsungVideoTimeLabel(currentPosition.coerceAtMost(duration))} / ${samsungVideoTimeLabel(duration)}",
                        color = Color.White,
                    )
                }
                Box(
                    Modifier.size(SamsungViewerControllerSize).clip(CircleShape).background(Color(0x9929292D)).clickable {
                        val nextMuted = !muted
                        onMutedChange(nextMuted)
                        player.volume = if (nextMuted) 0f else 1f
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        if (muted) "Off, toggle sound" else "On, toggle sound",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
