@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.danyal.vaultgallery.ui.VaultTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VaultVideoPlayerActivity : ComponentActivity() {
    private var pipSourceRect: Rect? = null

    private fun pictureInPictureParams(): PictureInPictureParams {
        val width = pipSourceRect?.width()?.coerceAtLeast(1) ?: window.decorView.width.coerceAtLeast(1)
        val height = pipSourceRect?.height()?.coerceAtLeast(1) ?: window.decorView.height.coerceAtLeast(1)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(width, height))
            .apply {
                pipSourceRect?.let(::setSourceRectHint)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(true)
            }
            .build()
    }

    fun updatePictureInPictureSource(view: android.view.View) {
        val visibleBounds = Rect()
        if (!view.getGlobalVisibleRect(visibleBounds) || visibleBounds.isEmpty) return
        pipSourceRect = visibleBounds
        setPictureInPictureParams(pictureInPictureParams())
    }

    fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun applyImmersive(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsCompat.Type.systemBars().let {
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun openPictureInPicture() {
        enterPictureInPictureMode(pictureInPictureParams())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val fallbackUri = intent.data ?: intent.parcelableStream()
        val queue = intent.getStringArrayListExtra("queue_uris").orEmpty()
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            .ifEmpty { listOfNotNull(fallbackUri) }
        if (queue.isEmpty()) { finish(); return }
        val titles = intent.getStringArrayListExtra("queue_titles").orEmpty()
        val startIndex = intent.getIntExtra("queue_index", 0).coerceIn(queue.indices)
        setContent { VaultTheme { SamsungStandaloneVideoPlayer(queue, titles, startIndex, ::finish) } }
    }
}

@Composable
private fun SamsungStandaloneVideoPlayer(
    uris: List<Uri>,
    titles: List<String>,
    startIndex: Int,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? VaultVideoPlayerActivity
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(startIndex) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var playing by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var speed by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var speedMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var captionsEnabled by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var interactionEpoch by remember { mutableLongStateOf(0L) }
    val player = remember(uris, startIndex) {
        buildSamsungGalleryPlayer(context).apply {
            setMediaItems(uris.map(MediaItem::fromUri), startIndex, 0L)
            prepare()
            playWhenReady = true
        }
    }

    fun interacted() {
        chromeVisible = true
        interactionEpoch++
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex.coerceIn(uris.indices)
                interactionEpoch++
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = (player.duration.takeIf { it > 0L } ?: 1L).coerceAtLeast(1L)
            playing = player.isPlaying
            delay(32)
        }
    }
    LaunchedEffect(chromeVisible, playing, locked, interactionEpoch) {
        if (chromeVisible && playing && !locked) {
            delay(3_000)
            chromeVisible = false
        }
    }
    LaunchedEffect(chromeVisible, locked) { activity?.applyImmersive(!chromeVisible || locked) }
    DisposableEffect(Unit) { onDispose { activity?.applyImmersive(false) } }
    BackHandler(locked) { locked = false; interacted() }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(player, locked) {
            detectTapGestures(onTap = {
                if (locked) locked = false else chromeVisible = !chromeVisible
                interactionEpoch++
            })
        },
    ) {
        AndroidView(
            factory = { PlayerView(it).apply {
                this.player = player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    activity?.updatePictureInPictureSource(view)
                }
            } },
            update = { view -> view.player = player; view.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(chromeVisible && !locked, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x18000000))) {
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0x52000000))
                        .statusBarsPadding().height(72.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
                    Text(
                        titles.getOrNull(currentIndex)?.substringBeforeLast('.')
                            ?: uris[currentIndex].lastPathSegment.orEmpty().substringBeforeLast('.'),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    IconButton(onClick = {
                        interacted()
                        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_CAST_SETTINGS)) }
                            .onFailure { Toast.makeText(context, "Wireless display settings are unavailable", Toast.LENGTH_SHORT).show() }
                    }) { Icon(Icons.Outlined.Cast, "Smart View", tint = Color.White) }
                    IconButton(onClick = {
                        captionsEnabled = !captionsEnabled
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled).build()
                        interacted()
                    }) { Icon(Icons.Outlined.ClosedCaption, if (captionsEnabled) "Turn subtitles off" else "Turn subtitles on", tint = if (captionsEnabled) Color.White else Color.Gray) }
                    Box {
                        IconButton(onClick = { moreMenu = true; interacted() }) { Icon(Icons.Outlined.MoreVert, "More options", tint = Color.White) }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("Details") }, onClick = { moreMenu = false; details = true })
                            DropdownMenuItem(text = { Text("Share") }, onClick = {
                                moreMenu = false
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "video/*"; putExtra(Intent.EXTRA_STREAM, uris[currentIndex]); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Share video"))
                            })
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.CenterStart).offset(y = (-125).dp).padding(start = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StandaloneRoundAction(Icons.Outlined.PhotoCamera, "Capture") {
                        interacted()
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { runCatching { captureStandaloneFrame(context, uris[currentIndex], currentPosition, titles.getOrNull(currentIndex)) } }
                            Toast.makeText(context, if (result.isSuccess) "Frame saved to Pictures/Vault Gallery" else "Could not capture this frame", Toast.LENGTH_SHORT).show()
                        }
                    }
                    StandaloneRoundAction(Icons.Outlined.Gif, "Create GIF") {
                        interacted()
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { runCatching { createStandaloneGif(context, uris[currentIndex]) } }
                            Toast.makeText(context, if (result.isSuccess) "GIF saved to Vault Gallery Creations" else "Could not create GIF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 54.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = player.hasPreviousMediaItem(),
                        onClick = { player.seekToPreviousMediaItem(); player.play(); interacted() },
                        modifier = Modifier.size(64.dp),
                    ) { Icon(Icons.Outlined.SkipPrevious, "Previous video", tint = if (player.hasPreviousMediaItem()) Color.White else Color.DarkGray, modifier = Modifier.size(38.dp)) }
                    IconButton(onClick = {
                        if (player.isPlaying) player.pause() else {
                            if (player.playbackState == Player.STATE_ENDED || currentPosition >= duration - 50L) player.seekTo(0L)
                            player.play()
                        }
                        interacted()
                    }, modifier = Modifier.size(78.dp).clip(CircleShape).background(Color(0x99202024))) {
                        Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause" else if (currentPosition >= duration - 50L) "Watch again" else "Play", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    IconButton(
                        enabled = player.hasNextMediaItem(),
                        onClick = { player.seekToNextMediaItem(); player.play(); interacted() },
                        modifier = Modifier.size(64.dp),
                    ) { Icon(Icons.Outlined.SkipNext, "Next video", tint = if (player.hasNextMediaItem()) Color.White else Color.DarkGray, modifier = Modifier.size(38.dp)) }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(samsungVideoTimeLabel(currentPosition), color = Color.White)
                        Spacer(Modifier.weight(1f))
                        Text(samsungVideoTimeLabel(duration), color = Color.White)
                    }
                    SamsungStandaloneSeekBar(
                        player = player,
                        positionMs = currentPosition,
                        durationMs = duration,
                        onScrubbingChanged = { active -> scrubbing = active; interacted() },
                        onPositionChanged = { currentPosition = it },
                        modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 14.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StandaloneTool(Icons.Outlined.FitScreen, "Screen ratio", Modifier.weight(1f)) {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }; interacted()
                        }
                        StandaloneTool(Icons.Outlined.PictureInPictureAlt, "Pop-up player", Modifier.weight(1f)) { activity?.openPictureInPicture(); interacted() }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            StandaloneTool(Icons.Outlined.Speed, if (speed == 1f) "Speed 1.0x" else "Speed ${speed}x") { speedMenu = true; interacted() }
                            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
                                    DropdownMenuItem(text = { Text(if (option == 1f) "Normal" else "${option}x") }, onClick = {
                                        speed = option; player.setPlaybackSpeed(option); speedMenu = false; interacted()
                                    })
                                }
                            }
                        }
                        StandaloneTool(Icons.Outlined.ScreenRotation, "Rotate", Modifier.weight(1f)) { activity?.toggleOrientation(); interacted() }
                        StandaloneTool(Icons.Outlined.Lock, "Lock", Modifier.weight(1f)) { locked = true; chromeVisible = false }
                    }
                }
            }
        }

        if (locked) Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xAA202024)).clickable { locked = false; interacted() }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) { Text("Tap to unlock controls", color = Color.White) }
    }

    if (details) AlertDialog(
        onDismissRequest = { details = false },
        title = { Text(titles.getOrNull(currentIndex) ?: uris[currentIndex].lastPathSegment.orEmpty()) },
        text = { Text("${samsungVideoTimeLabel(duration)} video\n${uris[currentIndex]}") },
        confirmButton = { TextButton(onClick = { details = false }) { Text("Done") } },
    )
}

@Composable
private fun StandaloneRoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0x99202024))) {
            Icon(icon, label, tint = Color.White)
        }
        Text(label, color = Color.White)
    }
}

@Composable
private fun StandaloneTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier.clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White, maxLines = 1, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SamsungStandaloneSeekBar(
    player: ExoPlayer,
    positionMs: Long,
    durationMs: Long,
    onScrubbingChanged: (Boolean) -> Unit,
    onPositionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.pointerInput(player, durationMs) {
        var latestPosition = positionMs
        fun seek(x: Float, final: Boolean) {
            val position = (durationMs * (x / size.width.toFloat()).coerceIn(0f, 1f)).toLong()
            latestPosition = position
            onPositionChanged(position)
            // Exact decoding for every pointer pixel queues expensive GOP decodes and produces
            // the loading spinner that made the old scrubber feel disconnected. During the
            // gesture show the nearest immediately decodable frame; commit the exact timestamp
            // once, when the finger is released.
            player.setSeekParameters(if (final) SeekParameters.EXACT else SeekParameters.CLOSEST_SYNC)
            player.seekTo(position)
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val resume = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            player.setScrubbingModeEnabled(true)
            onScrubbingChanged(true)
            seek(down.position.x, final = false)
            down.consume()
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    seek(change.position.x, final = false)
                    change.consume()
                    if (!change.pressed) break
                }
            } finally {
                player.setSeekParameters(SeekParameters.EXACT)
                player.seekTo(latestPosition)
                player.setScrubbingModeEnabled(false)
                onScrubbingChanged(false)
                player.playWhenReady = resume
            }
        }
    }) {
        val centerY = size.height / 2f
        val fraction = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        drawLine(Color(0x88FFFFFF), androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(size.width, centerY), 2.dp.toPx())
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(size.width * fraction, centerY), 3.dp.toPx())
        drawCircle(Color.White, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * fraction, centerY))
    }
}

private fun captureStandaloneFrame(context: android.content.Context, uri: Uri, positionMs: Long, title: String?): Uri {
    val retriever = MediaMetadataRetriever()
    val bitmap = try {
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: error("Frame unavailable")
    } finally { retriever.release() }
    return try {
        insertStandaloneMedia(context, "${title.orEmpty().substringBeforeLast('.').ifBlank { "Video" }}_${samsungVideoTimeLabel(positionMs).replace(':', '-')}.jpg", "image/jpeg", "Pictures/Vault Gallery") {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it))
        }
    } finally { bitmap.recycle() }
}

private fun createStandaloneGif(context: android.content.Context, uri: Uri): Uri {
    val frames = decodeVideoGifFrames(context, uri)
    check(frames.size >= 4) { "Not enough frames" }
    return try {
        insertStandaloneMedia(context, "Animation_${System.currentTimeMillis()}.gif", "image/gif", "Pictures/Vault Gallery Creations") {
            writeAnimatedGif(it, frames)
        }
    } finally { frames.forEach { if (!it.isRecycled) it.recycle() } }
}

private inline fun insertStandaloneMedia(
    context: android.content.Context,
    name: String,
    mime: String,
    relativePath: String,
    write: (java.io.OutputStream) -> Unit,
): Uri {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val collection = if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val output = context.contentResolver.insert(collection, values) ?: error("Could not create output")
    try {
        context.contentResolver.openOutputStream(output, "w")?.use(write) ?: error("Could not open output")
        if (Build.VERSION.SDK_INT >= 29) context.contentResolver.update(output, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return output
    } catch (error: Throwable) {
        context.contentResolver.delete(output, null, null)
        throw error
    }
}

private fun Intent.parcelableStream(): Uri? = if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
else @Suppress("DEPRECATION") getParcelableExtra(Intent.EXTRA_STREAM)
