@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)
@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections as FilledCollections
import androidx.compose.material.icons.filled.Image as FilledImage
import androidx.compose.material.icons.filled.Menu as FilledMenu
import androidx.compose.material.icons.filled.Movie as FilledMovie
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.danyal.vaultgallery.editor.PhotoPreset
import com.danyal.vaultgallery.editor.PhotoPresetStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.danyal.vaultgallery.core.GalleryLogic
import com.danyal.vaultgallery.data.AlbumKind
import com.danyal.vaultgallery.data.GalleryAlbum
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.data.MediaStoreRepository
import com.danyal.vaultgallery.ui.VaultBackground
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSecure
import com.danyal.vaultgallery.ui.VaultSurface
import com.danyal.vaultgallery.ui.VaultTheme
import com.danyal.vaultgallery.ui.LocalVaultMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged

private fun mediaKindSummary(media: List<GalleryMedia>): String {
    val images = media.count { it.kind == MediaKind.IMAGE }
    val videos = media.count { it.kind == MediaKind.VIDEO }
    val imageLabel = if (images == 1) "image" else "images"
    val videoLabel = if (videos == 1) "video" else "videos"
    return "$images $imageLabel  $videos $videoLabel"
}

private const val DEFAULT_VIEWER_PROBE = "com.danyal.vaultgallery.DEFAULT_VIEWER_PROBE"

private data class DefaultViewerInfo(val packageName: String, val label: String)

private fun mediaTypeLabel(mimeType: String): String = if (mimeType.startsWith("video/")) "videos" else "photos"

private fun resolvedDefaultViewer(context: android.content.Context, mimeType: String): DefaultViewerInfo? {
    val probe = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(
            Uri.parse(if (mimeType.startsWith("video/")) "content://media/external/video/media/1" else "content://media/external/images/media/1"),
            mimeType,
        )
    }
    val resolved = context.packageManager.resolveActivity(probe, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) ?: return null
    val info = resolved.activityInfo ?: return null
    if (
        info.packageName == "android" ||
        info.packageName == "com.android.intentresolver" ||
        info.name.contains("ResolverActivity", ignoreCase = true) ||
        info.name.contains("ChooserActivity", ignoreCase = true)
    ) return null
    return DefaultViewerInfo(info.packageName, resolved.loadLabel(context.packageManager).toString())
}

private fun isMediaPickerIntent(intent: Intent?): Boolean = intent?.action in setOf(Intent.ACTION_PICK, Intent.ACTION_GET_CONTENT)

private fun requestedPickerKind(intent: Intent?): MediaKind? {
    val type = intent?.type.orEmpty()
    val data = intent?.dataString.orEmpty()
    return when {
        type.startsWith("image/") || type.endsWith("/image") || "/images/" in data -> MediaKind.IMAGE
        type.startsWith("video/") || type.endsWith("/video") || "/video/" in data -> MediaKind.VIDEO
        else -> null
    }
}

internal fun Modifier.pinchToResizeGrid(
    columns: androidx.compose.runtime.MutableIntState,
    minimum: Int,
    maximum: Int,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState? = null,
    semanticExtremes: Boolean = false,
    onChanged: (Int) -> Unit = {},
): Modifier = composed {
    val visualScale = remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var pivotX by remember { mutableFloatStateOf(.5f) }
    var pivotY by remember { mutableFloatStateOf(.5f) }
    val latestOnChanged by rememberUpdatedState(onChanged)

    graphicsLayer {
        scaleX = visualScale.floatValue
        scaleY = visualScale.floatValue
        transformOrigin = TransformOrigin(pivotX, pivotY)
    }.pointerInput(columns, minimum, maximum, gridState, semanticExtremes) {
        data class Anchor(val index: Int, val fractionY: Float, val screenY: Float, val oldHeight: Int)

        fun anchorAt(centroid: Offset): Anchor? {
            val layout = gridState?.layoutInfo ?: return null
            val mediaItems = layout.visibleItemsInfo.filter { it.size.width < layout.viewportSize.width * .85f }
            val item = mediaItems.firstOrNull { info ->
                centroid.x >= info.offset.x && centroid.x <= info.offset.x + info.size.width &&
                    centroid.y >= info.offset.y && centroid.y <= info.offset.y + info.size.height
            } ?: mediaItems.minByOrNull { info ->
                val dx = info.offset.x + info.size.width / 2f - centroid.x
                val dy = info.offset.y + info.size.height / 2f - centroid.y
                dx * dx + dy * dy
            } ?: return null
            return Anchor(
                item.index,
                ((centroid.y - item.offset.y) / item.size.height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                centroid.y,
                item.size.height,
            )
        }

        fun changeColumns(next: Int, centroid: Offset) {
            if (next == columns.intValue) return
            val previous = columns.intValue
            val anchor = anchorAt(centroid)
            columns.intValue = next
            latestOnChanged(next)
            if (gridState == null || anchor == null) return

            // Request the reflow and initial compensation together, so the media beneath the
            // fingers does not flash at its uncorrected row before the next frame.
            val estimatedHeight = anchor.oldHeight * previous.toFloat() / next.toFloat()
            val desiredTop = anchor.screenY - anchor.fractionY * estimatedHeight
            val scrollOffset = (gridState.layoutInfo.viewportStartOffset - desiredTop).toInt()
            gridState.requestScrollToItem(anchor.index, scrollOffset)
            scope.launch {
                withFrameNanos { }
                gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == anchor.index }?.let { actual ->
                    val actualPoint = actual.offset.y + actual.size.height * anchor.fractionY
                    gridState.scrollBy(actualPoint - anchor.screenY)
                }
            }
        }

        fun applyZoom(factor: Float, centroid: Offset) {
            settleJob?.cancel()
            var scale = (visualScale.floatValue * factor).coerceIn(.68f, 1.48f)
            if (semanticExtremes) {
                visualScale.floatValue = scale.coerceIn(.78f, 1.28f)
                return
            }
            var current = columns.intValue
            while (current > minimum) {
                val boundary = current.toFloat() / (current - 1).toFloat()
                if (scale < boundary) break
                scale /= boundary
                current--
                changeColumns(current, centroid)
            }
            while (current < maximum) {
                val boundary = current.toFloat() / (current + 1).toFloat()
                if (scale > boundary) break
                scale /= boundary
                current++
                changeColumns(current, centroid)
            }
            visualScale.floatValue = scale.coerceIn(.78f, 1.28f)
        }

        fun settle(centroid: Offset) {
            val current = columns.intValue
            var scale = visualScale.floatValue
            if (semanticExtremes) {
                when {
                    current == maximum && scale >= 1.12f -> changeColumns(minimum, centroid)
                    current == minimum && scale <= .88f -> changeColumns(maximum, centroid)
                }
                settleJob = scope.launch {
                    animate(
                        initialValue = visualScale.floatValue,
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1_350f),
                    ) { value, _ ->
                        visualScale.floatValue = value
                    }
                }
                return
            }
            when {
                scale > 1f && current > minimum -> {
                    val boundary = current.toFloat() / (current - 1).toFloat()
                    if (scale >= (1f + boundary) / 2f) {
                        scale /= boundary
                        visualScale.floatValue = scale
                        changeColumns(current - 1, centroid)
                    }
                }
                scale < 1f && current < maximum -> {
                    val boundary = current.toFloat() / (current + 1).toFloat()
                    if (scale <= (1f + boundary) / 2f) {
                        scale /= boundary
                        visualScale.floatValue = scale
                        changeColumns(current + 1, centroid)
                    }
                }
            }
            settleJob = scope.launch {
                // Only settle the small residual scale after the final span reflow. A quick,
                // critically damped settle avoids the old whole-grid presentation-slide zoom.
                animate(
                    initialValue = visualScale.floatValue,
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1_500f),
                ) { value, _ ->
                    visualScale.floatValue = value
                }
            }
        }

        awaitEachGesture {
            // This grid also owns a long-press slide selector. Observe multi-touch in the Initial
            // pass so the long-press recognizer cannot consume the two-finger stream first.
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var lastSpan = 0f
            var zooming = false
            var lastCentroid = Offset(size.width / 2f, size.height / 2f)
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size < 2) {
                    if (zooming) settle(lastCentroid)
                    break
                }
                val centroid = pressed.fold(Offset.Zero) { sum, change -> sum + change.position } / pressed.size.toFloat()
                val span = pressed.sumOf { change ->
                    kotlin.math.hypot((change.position.x - centroid.x).toDouble(), (change.position.y - centroid.y).toDouble())
                }.toFloat() / pressed.size
                if (lastSpan == 0f) {
                    // Samsung anchors the media under the initial two-finger midpoint. Moving
                    // the transform origin every frame made the whole grid wobble under unequal
                    // finger movement and felt like a slideshow zoom transition.
                    pivotX = (centroid.x / size.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    pivotY = (centroid.y / size.height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                }
                if (lastSpan > 0f && span > 0f) {
                    zooming = true
                    applyZoom(span / lastSpan, centroid)
                    event.changes.forEach { it.consume() }
                }
                lastSpan = span
                lastCentroid = centroid
            }
        }
    }
}

class MainGalleryActivity : ComponentActivity() {
    private val galleryViewModel by viewModels<GalleryViewModel>()
    private val launchIntentState = mutableStateOf<Intent?>(null)
    private var approvedMediaMutation: (() -> Unit)? = null
    private var declinedMediaMutation: (() -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val mediaActionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching { approvedMediaMutation?.invoke() }
                .onFailure { Toast.makeText(this, it.message ?: "Could not update media", Toast.LENGTH_LONG).show() }
            approvedMediaMutation = null
            declinedMediaMutation = null
            galleryViewModel.clearSelection()
            galleryViewModel.refresh()
        } else {
            approvedMediaMutation = null
            declinedMediaMutation?.invoke()
            declinedMediaMutation = null
        }
    }
    private val documentScannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val scan = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pages = scan?.pages?.map { it.imageUri }.orEmpty()
        scan?.pdf?.let { pdf ->
            persistScannedPdf(this, pdf.uri, pdf.pageCount) { saved ->
                saved.onFailure { Toast.makeText(this, it.message ?: "Scanned PDF could not be saved", Toast.LENGTH_LONG).show() }
            }
        }
        persistScannedPages(this, pages) { copied ->
            Toast.makeText(this, "$copied scanned page${if (copied == 1) "" else "s"} saved", Toast.LENGTH_SHORT).show()
            galleryViewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchIntentState.value = intent
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        applyFullScreenPreference()
        setContent {
            VaultTheme {
                PublicGalleryApp(
                    galleryViewModel,
                    this,
                    launchIntentState.value?.getStringExtra("collection"),
                    launchIntentState.value,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntentState.value = intent
    }

    override fun onResume() {
        super.onResume()
        applyFullScreenPreference()
        galleryViewModel.refresh()
    }

    fun applyFullScreenPreference(enabled: Boolean = getSharedPreferences("gallery-settings", 0).getBoolean("full_screen_scroll", false)) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun toggleViewerOrientation() {
        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun restoreViewerOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /** Returns one or more public MediaStore items to legacy ACTION_PICK/GET_CONTENT callers. */
    fun finishMediaPick(items: List<GalleryMedia>) {
        if (items.isEmpty()) return
        val resultType = when {
            items.all { it.mimeType.startsWith("image/") } -> "image/*"
            items.all { it.mimeType.startsWith("video/") } -> "video/*"
            else -> "*/*"
        }
        val result = Intent().apply {
            data = items.first().uri
            type = resultType
            clipData = android.content.ClipData.newUri(contentResolver, items.first().name, items.first().uri).also { clip ->
                items.drop(1).forEach { item ->
                    clip.addItem(android.content.ClipData.Item(item.uri))
                }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    /** Starts a normal implicit VIEW request so Android—not the app—owns the default choice. */
    fun requestDefaultViewer(mimeType: String) {
        if (resolvedDefaultViewer(this, mimeType)?.packageName == packageName) {
            Toast.makeText(this, "Vault Gallery is already the default for ${mediaTypeLabel(mimeType)}", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = if (mimeType.startsWith("video/")) firstPublicMediaUri(MediaKind.VIDEO) else defaultViewerProbeImage()
        if (uri == null) {
            Toast.makeText(this, "Add a video first, then choose Vault Gallery when you open it", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(DEFAULT_VIEWER_PROBE, true)
            },
        )
    }

    private fun firstPublicMediaUri(kind: MediaKind): Uri? {
        val collection = if (kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return runCatching {
            contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) null else Uri.withAppendedPath(collection, cursor.getLong(0).toString())
            }
        }.getOrNull()
    }

    private fun defaultViewerProbeImage(): Uri? = runCatching {
        val directory = java.io.File(cacheDir, "editor-cache").apply { mkdirs() }
        val file = java.io.File(directory, "choose-vault-gallery.jpg")
        if (!file.isFile) {
            val bitmap = android.graphics.Bitmap.createBitmap(320, 240, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.rgb(24, 24, 28))
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 27f
            }
            canvas.drawText("Choose Vault Gallery", bitmap.width / 2f, bitmap.height / 2f, paint)
            java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            bitmap.recycle()
        }
        androidx.core.content.FileProvider.getUriForFile(this, "$packageName.secure.share", file)
    }.getOrNull()

    fun launchDocumentScanner() {
        val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { sender -> documentScannerLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener { error -> Toast.makeText(this, error.message ?: "Document scanner is unavailable", Toast.LENGTH_LONG).show() }
    }

    fun deleteMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = MediaStore.createTrashRequest(contentResolver, uris, true)
            mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            Toast.makeText(this, "Delete approval requires Android 11 or later", Toast.LENGTH_LONG).show()
        }
    }

    fun restoreMedia(uris: List<Uri>) {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < 30) return
        val pending = MediaStore.createTrashRequest(contentResolver, uris, false)
        mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    }

    fun deletePermanently(uris: List<Uri>) {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < 30) return
        val pending = MediaStore.createDeleteRequest(contentResolver, uris)
        mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    }

    fun setFavourite(uris: List<Uri>, favourite: Boolean) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            approvedMediaMutation = {
                Toast.makeText(this, if (favourite) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
            }
            val pending = MediaStore.createFavoriteRequest(contentResolver, uris, favourite)
            mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            Toast.makeText(this, "System favourites require Android 11 or later", Toast.LENGTH_LONG).show()
        }
    }

    fun updateDateTime(uris: List<Uri>, timestampMs: Long) {
        requestWriteAccess(uris) {
            var updated = 0
            uris.forEach { uri ->
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.ImageColumns.DATE_TAKEN, timestampMs)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, timestampMs / 1000L)
                }
                if (contentResolver.update(uri, values, null, null) > 0) updated++
            }
            Toast.makeText(this, "Updated date and time for $updated items", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun saveEditedPhoto(media: GalleryMedia, bitmap: android.graphics.Bitmap, mode: EditorSaveMode, exportOptions: PhotoExportOptions? = null, onFinished: (Result<Uri>) -> Unit) {
        val write: () -> Unit = {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        if (mode == EditorSaveMode.COPY) saveEditedBitmap(this@MainGalleryActivity, bitmap, media, exportOptions)
                        else overwriteEditedBitmap(this@MainGalleryActivity, bitmap, media)
                    }
                }
                bitmap.recycle()
                onFinished(result)
            }
        }
        if (mode == EditorSaveMode.REPLACE && Build.VERSION.SDK_INT >= 30 && media.bucketId != Long.MIN_VALUE) {
            requestWriteAccess(listOf(media.uri), onDenied = {
                bitmap.recycle()
                onFinished(Result.failure(IllegalStateException("Save cancelled")))
            }, mutation = write)
        } else write()
    }

    internal fun saveEditedVideo(media: GalleryMedia, file: java.io.File, mode: VideoEditorSaveMode, onFinished: (Result<Uri>) -> Unit) {
        val write: () -> Unit = {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        if (mode == VideoEditorSaveMode.COPY) saveExportedVideoToGallery(this@MainGalleryActivity, file, media)
                        else overwriteExportedVideo(this@MainGalleryActivity, file, media)
                    }
                }
                file.delete()
                onFinished(result)
            }
        }
        if (mode == VideoEditorSaveMode.REPLACE && Build.VERSION.SDK_INT >= 30 && media.bucketId != Long.MIN_VALUE) {
            requestWriteAccess(listOf(media.uri), onDenied = {
                file.delete()
                onFinished(Result.failure(IllegalStateException("Save cancelled")))
            }, mutation = write)
        } else write()
    }

    fun updateLocation(items: List<GalleryMedia>, latitude: Double, longitude: Double) {
        val images = items.filter { it.kind == MediaKind.IMAGE }
        requestWriteAccess(images.map { it.uri }) {
            var updated = 0
            images.forEach { item ->
                runCatching {
                    contentResolver.openFileDescriptor(item.uri, "rw")?.use { descriptor ->
                        androidx.exifinterface.media.ExifInterface(descriptor.fileDescriptor).apply {
                            setLatLong(latitude, longitude)
                            saveAttributes()
                        }
                    } ?: error("Could not open ${item.name}")
                }.onSuccess { updated++ }
            }
            Toast.makeText(this, "Updated location for $updated images", Toast.LENGTH_SHORT).show()
        }
    }

    fun transferMediaToAlbum(items: List<GalleryMedia>, targetAlbum: String, move: Boolean) {
        if (items.isEmpty()) return
        val enqueue = {
            MediaTransferCoordinator.enqueuePublic(this, items, sanitizeAlbumName(targetAlbum), move)
            Toast.makeText(this, "${if (move) "Move" else "Copy"} started in the background", Toast.LENGTH_SHORT).show()
        }
        if (move) requestWriteAccess(items.map { it.uri }, mutation = enqueue) else enqueue()
    }

    internal fun transferMediaToSecure(
        uris: List<Uri>,
        move: Boolean,
        sourceKind: TransferSourceKind = TransferSourceKind.MEDIA,
        sourceFolderCount: Int = 0,
    ) {
        if (uris.isEmpty()) return
        val launch = { launchSecureImport(this, ArrayList(uris), move, sourceKind, sourceFolderCount) }
        if (move) requestWriteAccess(uris, mutation = launch) else launch()
    }

    private fun requestWriteAccess(uris: List<Uri>, onDenied: (() -> Unit)? = null, mutation: () -> Unit) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            approvedMediaMutation = mutation
            declinedMediaMutation = onDenied
            val pending = MediaStore.createWriteRequest(contentResolver, uris)
            mediaActionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else runCatching(mutation).onFailure { Toast.makeText(this, it.message ?: "Could not update media", Toast.LENGTH_LONG).show() }
    }
}

private enum class PublicTab { PICTURES, ALBUMS, STORIES, MENU }
private enum class AuxiliaryScreen { SEARCH, VIDEOS, RECENT, FAVOURITES, CLEAN_OUT, LOCATIONS, SHARED_ALBUMS }

@Composable
private fun PublicGalleryApp(
    viewModel: GalleryViewModel,
    activity: MainGalleryActivity,
    initialCollection: String?,
    launchIntent: Intent?,
) {
    val motion = LocalVaultMotion.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galleryPreferences = remember { activity.getSharedPreferences("gallery-settings", 0) }
    val mediaPickerLaunch = isMediaPickerIntent(launchIntent)
    val pickerAllowsMultiple = mediaPickerLaunch && launchIntent?.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) == true
    val pickerKind = requestedPickerKind(launchIntent)
    val rememberedTab = remember {
        if (mediaPickerLaunch) PublicTab.PICTURES else runCatching {
            PublicTab.valueOf(galleryPreferences.getString("resume_public_tab", PublicTab.PICTURES.name).orEmpty())
        }.getOrDefault(PublicTab.PICTURES).let { if (it == PublicTab.MENU) PublicTab.PICTURES else it }
    }
    var tab by remember { mutableStateOf(rememberedTab) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<GalleryMedia?>(null) }
    var viewerItems by remember { mutableStateOf<List<GalleryMedia>>(emptyList()) }
    var editor by remember { mutableStateOf<GalleryMedia?>(null) }
    var editorLaunchMode by remember { mutableStateOf(EditorLaunchMode.STANDARD) }
    val externalLaunch = launchIntent?.action in setOf(
        Intent.ACTION_VIEW,
        Intent.ACTION_EDIT,
        MediaStore.ACTION_REVIEW,
        "com.android.camera.action.REVIEW",
    ) || mediaPickerLaunch
    var handledLaunchUri by remember(launchIntent) { mutableStateOf<String?>(null) }
    var creativeRequest by remember { mutableStateOf<CreativeRequest?>(null) }
    var settings by remember { mutableStateOf(false) }
    var allAlbums by remember { mutableStateOf(false) }
    var albumFilter by remember { mutableStateOf<Set<Long>?>(null) }
    var virtualAlbumFilter by remember { mutableStateOf<Set<Long>?>(null) }
    var virtualAlbumTitle by remember { mutableStateOf<String?>(null) }
    var virtualAlbumId by remember { mutableStateOf<Long?>(null) }
    var smartAlbumVersion by remember { mutableIntStateOf(0) }
    var messagingSourceOpen by remember { mutableStateOf<MessagingSource?>(null) }
    var messagingChatOpen by remember { mutableStateOf<GalleryAlbum?>(null) }
    var messagingVersion by remember { mutableIntStateOf(0) }
    var messagingModes by remember {
        mutableStateOf(MessagingSource.entries.associateWith(activity::messagingOrganizationMode))
    }
    var resumeStateApplied by remember { mutableStateOf(initialCollection != null || mediaPickerLaunch) }
    var albumOpenedFromAll by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf<MediaKind?>(null) }
    var favouriteOnly by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var auxiliaryScreen by remember(initialCollection) {
        mutableStateOf(
            when (initialCollection) {
                "Videos" -> AuxiliaryScreen.VIDEOS
                "Recent" -> AuxiliaryScreen.RECENT
                "Favourites" -> AuxiliaryScreen.FAVOURITES
                else -> null
            },
        )
    }
    var storyItems by remember { mutableStateOf<List<GalleryMedia>?>(null) }
    var selectedAlbumIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var albumCoverVersion by remember { mutableIntStateOf(0) }
    var essentialAlbumIds by remember {
        mutableStateOf<Set<Long>>(galleryPreferences.getStringSet("essential_album_ids", emptySet()).orEmpty().mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() })
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }
    val contactsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { messagingVersion++ }

    LaunchedEffect(launchIntent) {
        if (isMediaPickerIntent(launchIntent)) {
            // MainGalleryActivity is singleTop so a picker request can arrive while a viewer,
            // editor, or nested collection from the previous task is still composed. A system
            // picker contract must always start in a clean, bounded selection surface.
            activity.restoreViewerOrientation()
            viewer = null
            viewerItems = emptyList()
            editor = null
            storyItems = null
            creativeRequest = null
            settings = false
            trashOpen = false
            auxiliaryScreen = null
            allAlbums = false
            albumFilter = null
            virtualAlbumFilter = null
            virtualAlbumTitle = null
            virtualAlbumId = null
            messagingSourceOpen = null
            messagingChatOpen = null
            menuOpen = false
            selectedAlbumIds = emptySet()
            tab = PublicTab.PICTURES
            viewModel.clearSelection()
        }
    }

    LaunchedEffect(Unit) {
        val handled = HashSet<java.util.UUID>()
        var seeded = false
        while (true) {
            val work = withContext(Dispatchers.IO) {
                WorkManager.getInstance(activity).getWorkInfosByTag(MediaTransferCoordinator.WORK_TAG).get()
            }
            if (!seeded) {
                // onResume already loaded current media. Historical WorkManager records are a
                // baseline, not newly completed transfers for this viewer session.
                handled.addAll(work.filter { it.state.isFinished }.map { it.id })
                seeded = true
            } else {
                val newlyFinished = work.filter { it.state.isFinished && handled.add(it.id) }
                if (newlyFinished.isNotEmpty()) viewModel.refresh()
            }
            delay(2_000)
        }
    }
    LaunchedEffect(state.media, viewer?.id) {
        viewer?.let { current -> state.media.firstOrNull { it.id == current.id }?.let { refreshed -> viewer = refreshed } }
        if (viewerItems.isNotEmpty()) {
            val refreshedById = state.media.associateBy(GalleryMedia::id)
            viewerItems = viewerItems.mapNotNull { refreshedById[it.id] }
        }
    }
    LaunchedEffect(state.loading, state.media, launchIntent) {
        val supplied = launchIntent?.data ?: return@LaunchedEffect
        if (state.loading || handledLaunchUri == supplied.toString()) return@LaunchedEffect
        val item = state.media.firstOrNull { it.uri == supplied }
            ?: withContext(Dispatchers.IO) { resolveExternalMedia(activity, supplied, launchIntent.type) }
            ?: return@LaunchedEffect
        handledLaunchUri = supplied.toString()
        val sameAlbum = state.media.filter { it.bucketId == item.bucketId }
            .ifEmpty { listOf(item) }
        viewerItems = if (sameAlbum.any { it.uri == item.uri }) sameAlbum else listOf(item)
        if (launchIntent.action == Intent.ACTION_EDIT) {
            editorLaunchMode = EditorLaunchMode.STANDARD
            editor = item
        } else viewer = item
    }
    BackHandler(menuOpen || creativeRequest != null || editor != null || viewer != null || storyItems != null || settings || trashOpen || auxiliaryScreen != null || allAlbums || messagingSourceOpen != null || messagingChatOpen != null || albumFilter != null || virtualAlbumFilter != null || state.selectedIds.isNotEmpty() || selectedAlbumIds.isNotEmpty()) {
        when {
            menuOpen -> menuOpen = false
            creativeRequest != null -> creativeRequest = null
            editor != null -> editor = null
            viewer != null -> { activity.restoreViewerOrientation(); viewer = null }
            storyItems != null -> storyItems = null
            settings -> settings = false
            trashOpen -> trashOpen = false
            auxiliaryScreen != null -> auxiliaryScreen = null
            state.selectedIds.isNotEmpty() -> viewModel.clearSelection()
            selectedAlbumIds.isNotEmpty() -> selectedAlbumIds = emptySet()
            messagingChatOpen != null -> messagingChatOpen = null
            messagingSourceOpen != null -> messagingSourceOpen = null
            virtualAlbumFilter != null -> {
                virtualAlbumFilter = null
                virtualAlbumTitle = null
                virtualAlbumId = null
                allAlbums = albumOpenedFromAll
                albumOpenedFromAll = false
            }
            albumFilter != null -> {
                albumFilter = null
                allAlbums = albumOpenedFromAll
                albumOpenedFromAll = false
            }
            allAlbums -> allAlbums = false
        }
    }

    if (creativeRequest != null) {
        CreativeStudioScreen(creativeRequest!!, onBack = { creativeRequest = null }, onCreated = {
            Toast.makeText(activity, "${creativeRequest!!.type.title} saved", Toast.LENGTH_SHORT).show()
            creativeRequest = null; viewModel.clearSelection(); viewModel.refresh()
        })
        return
    }
    if (editor != null) {
        if (editor!!.kind == MediaKind.VIDEO) {
            PublicVideoEditor(
                editor!!,
                onBack = { if (externalLaunch) activity.finish() else editor = null },
                onSaved = { editor = null; viewer = null; viewModel.refresh(); if (externalLaunch) activity.finish() },
            )
        } else {
            PublicPhotoEditor(
                editor!!,
                editorLaunchMode,
                onBack = { if (externalLaunch) activity.finish() else editor = null },
                onSaved = { editor = null; viewer = null; viewModel.refresh(); if (externalLaunch) activity.finish() },
            )
        }
        return
    }
    if (viewer != null) {
        PublicViewer(
            media = viewer!!,
            items = viewerItems.ifEmpty { state.media },
            onBack = {
                activity.restoreViewerOrientation()
                if (externalLaunch) activity.finish() else viewer = null
            },
            onNavigate = { viewer = it },
            onShare = { shareUris(activity, listOf(viewer!!.uri), viewer!!.mimeType) },
            onSecure = { move -> activity.transferMediaToSecure(listOf(viewer!!.uri), move) },
            onFavourite = { activity.setFavourite(listOf(viewer!!.uri), !viewer!!.isFavourite) },
            onEdit = { editorLaunchMode = EditorLaunchMode.STANDARD; editor = viewer },
            onAiAssist = { editorLaunchMode = EditorLaunchMode.AI_ASSIST; editor = viewer },
            onScanDocument = { editorLaunchMode = EditorLaunchMode.DOCUMENT_SCAN; editor = viewer },
            onFindSimilar = {
                val source = viewer ?: return@PublicViewer
                scope.launch {
                    Toast.makeText(activity, "Finding visually similar photos…", Toast.LENGTH_SHORT).show()
                    val matches = findVisuallySimilar(activity, source, state.media)
                    if (matches.isEmpty()) {
                        Toast.makeText(activity, "No close visual matches found", Toast.LENGTH_SHORT).show()
                    } else {
                        activity.restoreViewerOrientation()
                        viewer = null
                        tab = PublicTab.ALBUMS
                        virtualAlbumFilter = (listOf(source) + matches).mapTo(LinkedHashSet(), GalleryMedia::id)
                        virtualAlbumTitle = "Similar to ${source.name}"
                        virtualAlbumId = Long.MIN_VALUE + source.id
                        albumOpenedFromAll = false
                    }
                }
            },
            onCreateGif = { creativeRequest = CreativeRequest(CreationType.GIF, listOf(viewer!!)) },
            onMediaCreated = { viewModel.refresh() },
            onDelete = { activity.deleteMedia(listOf(viewer!!.uri)); activity.restoreViewerOrientation(); viewer = null },
        )
        return
    }
    if (storyItems != null) {
        StoryViewer(storyItems!!, onBack = { storyItems = null })
        return
    }
    if (settings) {
        GallerySettings(onBack = { settings = false }, onMessagingChanged = {
            messagingVersion++
            messagingModes = MessagingSource.entries.associateWith(activity::messagingOrganizationMode)
        })
        return
    }
    if (trashOpen) {
        PublicTrashScreen(
            state.trash,
            onBack = { trashOpen = false },
            onRestore = { activity.restoreMedia(listOf(it.uri)) },
            onDelete = { activity.deletePermanently(listOf(it.uri)) },
            onEmpty = { activity.deletePermanently(state.trash.map { it.uri }) },
        )
        return
    }

    if (auxiliaryScreen != null) {
        when (auxiliaryScreen!!) {
            AuxiliaryScreen.SEARCH -> GallerySearchScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewerItems = state.media; viewer = it })
            AuxiliaryScreen.VIDEOS -> SamsungCollectionScreen("Videos", state.media.filter { it.kind == MediaKind.VIDEO }, onBack = { auxiliaryScreen = null }, onOpen = { item, list -> viewerItems = list; viewer = item }, onCreate = { type, media -> creativeRequest = CreativeRequest(type, media) })
            AuxiliaryScreen.RECENT -> SamsungCollectionScreen("Recent", state.media, onBack = { auxiliaryScreen = null }, onOpen = { item, list -> viewerItems = list; viewer = item }, onCreate = { type, media -> creativeRequest = CreativeRequest(type, media) })
            AuxiliaryScreen.FAVOURITES -> SamsungCollectionScreen("Favourites", state.media.filter { it.isFavourite }, onBack = { auxiliaryScreen = null }, onOpen = { item, list -> viewerItems = list; viewer = item }, onCreate = { type, media -> creativeRequest = CreativeRequest(type, media) })
            AuxiliaryScreen.CLEAN_OUT -> CleanOutScreen(
                state.media,
                onBack = { auxiliaryScreen = null },
                onOpen = { viewerItems = state.media; viewer = it },
                onDelete = { activity.deleteMedia(it.map(GalleryMedia::uri)) },
            )
            AuxiliaryScreen.LOCATIONS -> LocationsScreen(state.media, onBack = { auxiliaryScreen = null }, onOpen = { viewerItems = state.media; viewer = it })
            AuxiliaryScreen.SHARED_ALBUMS -> SharedAlbumsScreen(onBack = { auxiliaryScreen = null })
        }
        return
    }

    val messagingCatalogs by produceState<List<MessagingCatalog>>(
        initialValue = emptyList(),
        state.media,
        messagingVersion,
    ) {
        value = withContext(Dispatchers.IO) { MessagingAlbumRepository(activity).load(state.media) }
    }
    val filtered = remember(state.media, query, albumFilter, virtualAlbumFilter, kindFilter, favouriteOnly, pickerKind) {
        state.media.filter { media ->
            (albumFilter == null || media.bucketId in albumFilter.orEmpty()) &&
                (virtualAlbumFilter == null || media.id in virtualAlbumFilter.orEmpty()) &&
                (kindFilter == null || media.kind == kindFilter) &&
                (pickerKind == null || media.kind == pickerKind) &&
                (!favouriteOnly || media.isFavourite) &&
                (query.isBlank() || media.name.contains(query, true) || media.bucketName.contains(query, true))
        }
    }
    val mergeAlbums = galleryPreferences.getBoolean("merge_albums", true)
    val albums = remember(state.media, mergeAlbums, albumCoverVersion, messagingCatalogs, messagingModes, smartAlbumVersion) {
        val messagingBucketIds = state.media.filter { media ->
            val source = media.messagingSource()
            source != null && messagingModes[source] == MessagingOrganizationMode.CONVERSATIONS
        }.mapTo(HashSet()) { it.bucketId }
        val physical = MediaStoreRepository(activity).albums(state.media, mergeAlbums).filterNot { album ->
            album.bucketIds.isNotEmpty() && album.bucketIds.all { it in messagingBucketIds }
        }.map { album ->
            val customUri = galleryPreferences.getString("album_cover_${album.bucketId}", null)
            val custom = customUri?.let { saved -> state.media.firstOrNull { it.uri.toString() == saved && it.bucketId in album.bucketIds } }
            if (custom == null) album else album.copy(cover = custom)
        }
        val groups = galleryPreferences.getStringSet("album_group_names", emptySet()).orEmpty().mapNotNull { name ->
            val ids = galleryPreferences.getStringSet("album_group_$name", emptySet()).orEmpty().mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() }
            val members = physical.filter { album -> album.bucketIds.any { it in ids } }
            val cover = members.maxByOrNull { it.cover.dateTakenMs }?.cover ?: return@mapNotNull null
            GalleryAlbum(Long.MIN_VALUE + name.hashCode().toLong(), name, cover, members.sumOf { it.count }, members.flatMapTo(LinkedHashSet()) { it.bucketIds })
        }
        val messagingRoots = messagingCatalogs
            .filter { messagingModes[it.source] == MessagingOrganizationMode.CONVERSATIONS }
            .mapNotNull(MessagingCatalog::rootAlbum)
        val smart = SmartAlbumRepository(activity).load().mapNotNull { rule ->
            val indexedMatches = if (rule.indexedText.isBlank()) emptySet() else runCatching {
                GallerySearchIndex(activity).use { it.searchPublicIds(rule.indexedText) }
            }.getOrDefault(emptySet())
            val members = state.media.filter { SmartAlbumMatcher.matches(it, rule, indexedMatches = indexedMatches) }
            val cover = members.maxByOrNull(GalleryMedia::dateTakenMs) ?: return@mapNotNull null
            GalleryAlbum(
                bucketId = -4_000_000_000L - rule.id.hashCode().toLong().let { kotlin.math.abs(it) },
                name = rule.name,
                cover = cover,
                count = members.size,
                bucketIds = emptySet(),
                mediaIds = members.mapTo(LinkedHashSet(), GalleryMedia::id),
                orderedMedia = members.sortedByDescending(GalleryMedia::dateTakenMs),
                kind = AlbumKind.SMART,
                subtitle = "Smart album • ${members.size}",
            )
        }
        (groups + smart + messagingRoots + physical).withNavigationAlbumsFirst()
    }

    LaunchedEffect(state.loading, state.hasAccess, albums, messagingCatalogs, messagingModes) {
        if (!resumeStateApplied && !state.loading && state.hasAccess && messagingCatalogs.size == MessagingSource.entries.size) {
            tab = rememberedTab
            when (galleryPreferences.getString("resume_destination", "tab")) {
                "all-albums" -> if (tab == PublicTab.ALBUMS) allAlbums = true
                "physical-album" -> {
                    val savedBuckets = galleryPreferences.getStringSet("resume_album_bucket_ids", emptySet()).orEmpty()
                        .mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() }
                    val album = albums.firstOrNull { it.kind == AlbumKind.PHYSICAL && it.bucketIds == savedBuckets }
                    if (album != null) {
                        tab = PublicTab.ALBUMS
                        albumFilter = album.bucketIds
                        albumOpenedFromAll = galleryPreferences.getBoolean("resume_album_from_all", false)
                    }
                }
                "virtual-album" -> {
                    val savedId = galleryPreferences.getLong("resume_virtual_album_id", 0L)
                    val album = albums.firstOrNull { it.bucketId == savedId && it.mediaIds != null }
                    if (album != null) {
                        tab = PublicTab.ALBUMS
                        virtualAlbumFilter = album.mediaIds
                        virtualAlbumTitle = album.name
                        virtualAlbumId = album.bucketId
                        albumOpenedFromAll = galleryPreferences.getBoolean("resume_album_from_all", false)
                    }
                }
                "messaging-source", "messaging-chat" -> {
                    val source = MessagingSource.entries.firstOrNull {
                        it.storageId == galleryPreferences.getString("resume_messaging_source", null)
                    }
                    if (source != null && messagingModes[source] == MessagingOrganizationMode.CONVERSATIONS) {
                        tab = PublicTab.ALBUMS
                        messagingSourceOpen = source
                        if (galleryPreferences.getString("resume_destination", "tab") == "messaging-chat") {
                            val chatId = galleryPreferences.getLong("resume_messaging_chat_id", 0L)
                            messagingChatOpen = messagingCatalogs.firstOrNull { it.source == source }
                                ?.albums?.firstOrNull { it.bucketId == chatId }
                        }
                    }
                }
            }
            resumeStateApplied = true
        }
    }
    LaunchedEffect(
        resumeStateApplied,
        tab,
        allAlbums,
        albumFilter,
        virtualAlbumFilter,
        virtualAlbumId,
        albumOpenedFromAll,
        messagingSourceOpen,
        messagingChatOpen,
    ) {
        if (!resumeStateApplied) return@LaunchedEffect
        val destination = when {
            messagingChatOpen != null -> "messaging-chat"
            messagingSourceOpen != null -> "messaging-source"
            virtualAlbumFilter != null -> "virtual-album"
            albumFilter != null -> "physical-album"
            allAlbums -> "all-albums"
            else -> "tab"
        }
        galleryPreferences.edit()
            .putString("resume_public_tab", tab.name)
            .putString("resume_destination", destination)
            .putStringSet("resume_album_bucket_ids", albumFilter.orEmpty().mapTo(LinkedHashSet()) { it.toString() })
            .putLong("resume_virtual_album_id", virtualAlbumId ?: 0L)
            .putBoolean("resume_album_from_all", albumOpenedFromAll)
            .putString("resume_messaging_source", messagingSourceOpen?.storageId)
            .putLong("resume_messaging_chat_id", messagingChatOpen?.bucketId ?: 0L)
            .apply()
    }
    val selectedMedia = state.media.filter { it.id in state.selectedIds }
    val writableAlbums = albums.filter { it.kind == AlbumKind.PHYSICAL }
    val allOperationAlbums = albums + messagingCatalogs.flatMap(MessagingCatalog::albums)
    val selectedUris = selectedMedia.map { it.uri }
    val allSelectedFavourite = selectedMedia.isNotEmpty() && selectedMedia.all { it.isFavourite }

    fun openItem(item: GalleryMedia, displayed: List<GalleryMedia>) {
        when {
            mediaPickerLaunch && pickerAllowsMultiple -> viewModel.toggleSelection(item.id)
            mediaPickerLaunch -> activity.finishMediaPick(listOf(item))
            state.selectedIds.isNotEmpty() -> viewModel.toggleSelection(item.id)
            item.kind == MediaKind.VIDEO && galleryPreferences.getBoolean("external_player", false) -> {
                if (!openExternalVideo(activity, item, displayed)) {
                    viewerItems = displayed
                    viewer = item
                }
            }
            else -> {
                viewerItems = displayed
                viewer = item
            }
        }
    }

    Scaffold(
        containerColor = VaultBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(Modifier.background(VaultBackground).navigationBarsPadding()) {
                AnimatedContent(
                    targetState = when { state.selectedIds.isNotEmpty() -> "media"; selectedAlbumIds.isNotEmpty() -> "albums"; else -> "navigation" },
                    transitionSpec = { fadeIn(tween(motion.duration(motion.fastMs))) togetherWith fadeOut(tween(motion.duration(motion.fastMs))) },
                    label = "gallery bottom controls",
                ) { bottomMode ->
                if (bottomMode == "media" && mediaPickerLaunch) {
                    PickerSelectionBar(
                        count = selectedMedia.size,
                        onCancel = { viewModel.clearSelection() },
                        onDone = { activity.finishMediaPick(selectedMedia) },
                    )
                } else if (bottomMode == "media") {
                    SelectionBar(
                        selectedMedia = selectedMedia,
                        availableAlbums = writableAlbums,
                        onShare = { shareUris(activity, selectedUris, "*/*") },
                        onSecure = { move -> activity.transferMediaToSecure(selectedUris, move) },
                        onCreate = { type -> creativeRequest = CreativeRequest(type, selectedMedia) },
                        favouriteLabel = if (allSelectedFavourite) "Unfavourite" else "Favourite",
                        onFavourite = { activity.setFavourite(selectedUris, !allSelectedFavourite) },
                        onEditDateTime = { timestamp -> activity.updateDateTime(selectedUris, timestamp) },
                        onEditLocation = { latitude, longitude -> activity.updateLocation(selectedMedia, latitude, longitude) },
                        onDelete = { activity.deleteMedia(selectedUris) },
                        onTransferStarted = viewModel::clearSelection,
                    )
                } else if (bottomMode == "albums") {
                    val selectedAlbums = allOperationAlbums.filter { it.bucketId in selectedAlbumIds }
                    val selectedBuckets = selectedAlbums.flatMapTo(LinkedHashSet()) { it.bucketIds }
                    val selectedVirtualMedia = selectedAlbums.flatMapTo(LinkedHashSet()) { it.mediaIds.orEmpty() }
                    val albumMedia = state.media.filter { it.bucketId in selectedBuckets || it.id in selectedVirtualMedia }
                    AlbumSelectionBar(
                        selectedAlbums = selectedAlbums,
                        selectedMedia = albumMedia,
                        availableAlbums = writableAlbums,
                        onShare = { shareUris(activity, albumMedia.map { it.uri }, "*/*") },
                        onSecure = { move ->
                            activity.transferMediaToSecure(
                                albumMedia.map { it.uri },
                                move,
                                TransferSourceKind.ALBUMS,
                                selectedAlbums.size,
                            )
                        },
                        essentialAlbumIds = essentialAlbumIds,
                        onEssentialChanged = { ids ->
                            essentialAlbumIds = ids
                            galleryPreferences.edit().putStringSet("essential_album_ids", ids.mapTo(LinkedHashSet()) { it.toString() }).apply()
                        },
                        onCoverChanged = { album, media ->
                            galleryPreferences.edit().putString("album_cover_${album.bucketId}", media.uri.toString()).apply()
                            albumCoverVersion++
                        },
                        onGroupCreated = { albumCoverVersion++ },
                        onDelete = { activity.deleteMedia(albumMedia.map { it.uri }); selectedAlbumIds = emptySet() },
                        onDone = { selectedAlbumIds = emptySet() },
                    )
                } else if (mediaPickerLaunch) {
                    PickerBottomNavigation(
                        tab = tab,
                        onTab = { next -> tab = next; allAlbums = false; albumFilter = null; virtualAlbumFilter = null },
                        onCancel = { activity.setResult(Activity.RESULT_CANCELED); activity.finish() },
                    )
                } else {
                    PublicBottomNavigation(if (menuOpen) PublicTab.MENU else tab) { next ->
                        if (next == PublicTab.MENU) menuOpen = true
                        else { menuOpen = false; tab = next; allAlbums = false; albumFilter = null; virtualAlbumFilter = null; virtualAlbumTitle = null; virtualAlbumId = null; albumOpenedFromAll = false; kindFilter = null; favouriteOnly = false; selectedAlbumIds = emptySet() }
                    }
                }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(VaultBackground)) {
            val destination = when {
                !state.hasAccess -> "permission"
                state.error != null -> "error"
                messagingChatOpen != null -> "messaging-chat-${messagingChatOpen!!.bucketId}"
                messagingSourceOpen != null -> "messaging-${messagingSourceOpen!!.storageId}"
                allAlbums -> "all-albums"
                virtualAlbumFilter != null -> "album-virtual-${virtualAlbumId ?: virtualAlbumFilter.hashCode()}"
                albumFilter != null -> "album-${albumFilter.hashCode()}"
                else -> "tab-${tab.name}"
            }
            AnimatedContent(
                targetState = destination,
                transitionSpec = { fadeIn(tween(motion.duration(motion.fastMs))) togetherWith fadeOut(tween(motion.duration(motion.fastMs))) },
                label = "gallery navigation",
                modifier = Modifier.fillMaxSize(),
            ) { shownDestination ->
            when {
                shownDestination == "permission" -> PermissionGate(
                    loading = state.loading,
                    onGrant = { permissionLauncher.launch(requiredMediaPermissions()) },
                )
                shownDestination == "error" -> ErrorState(state.error ?: "Unknown gallery error", viewModel::refresh)
                shownDestination == "all-albums" -> AllAlbumsScreen(
                    albums = albums,
                    selected = selectedAlbumIds,
                    onBack = { if (selectedAlbumIds.isNotEmpty()) selectedAlbumIds = emptySet() else allAlbums = false },
                    onOpen = { album ->
                        when (album.kind) {
                            AlbumKind.WHATSAPP_ROOT -> {
                                messagingSourceOpen = MessagingSource.WHATSAPP
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            AlbumKind.WHATSAPP_BUSINESS_ROOT -> {
                                messagingSourceOpen = MessagingSource.WHATSAPP_BUSINESS
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            AlbumKind.SMART -> {
                                albumFilter = null
                                virtualAlbumFilter = album.mediaIds.orEmpty()
                                virtualAlbumTitle = album.name
                                virtualAlbumId = album.bucketId
                                albumOpenedFromAll = true
                            }
                            else -> { albumFilter = album.bucketIds; albumOpenedFromAll = true }
                        }
                        allAlbums = false
                        selectedAlbumIds = emptySet()
                    },
                    onToggleSelection = { id -> if (!mediaPickerLaunch) selectedAlbumIds = GalleryLogic.toggleSelection(selectedAlbumIds, id) },
                )
                shownDestination.startsWith("messaging-chat-") -> {
                    val chat = messagingChatOpen
                    val chatItems = chat?.orderedMedia.orEmpty()
                    MediaGridScreen(
                        title = chat?.name ?: "Conversation",
                        media = chatItems,
                        selected = state.selectedIds,
                        query = query,
                        searchOpen = searchOpen,
                        onBack = { messagingChatOpen = null },
                        onSearch = { searchOpen = !searchOpen },
                        onQuery = { query = it },
                        onOpen = ::openItem,
                        onSelect = { id ->
                            val item = chatItems.firstOrNull { it.id == id }
                            if (mediaPickerLaunch && !pickerAllowsMultiple && item != null) activity.finishMediaPick(listOf(item))
                            else viewModel.toggleSelection(id)
                        },
                        onSelectionSet = { ids -> if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.setSelection(ids) },
                        onRefresh = viewModel::refresh,
                        onCreate = { type, items -> creativeRequest = CreativeRequest(type, items) },
                    )
                }
                shownDestination.startsWith("messaging-") -> {
                    val catalog = messagingCatalogs.firstOrNull { it.source == messagingSourceOpen }
                    MessagingAlbumsScreen(
                        catalog = catalog,
                        organizationMode = messagingSourceOpen?.let { messagingModes[it] }
                            ?: MessagingOrganizationMode.CONVERSATIONS,
                        selected = selectedAlbumIds,
                        onBack = { if (selectedAlbumIds.isNotEmpty()) selectedAlbumIds = emptySet() else messagingSourceOpen = null },
                        onOpen = { album -> messagingChatOpen = album; selectedAlbumIds = emptySet() },
                        onToggleSelection = { id -> if (!mediaPickerLaunch) selectedAlbumIds = GalleryLogic.toggleSelection(selectedAlbumIds, id) },
                        onOrganizationModeChanged = { source, mode ->
                            activity.setMessagingOrganizationMode(source, mode)
                            messagingModes = messagingModes + (source to mode)
                            if (mode == MessagingOrganizationMode.ORIGINAL_FOLDERS) {
                                messagingChatOpen = null
                                messagingSourceOpen = null
                                tab = PublicTab.ALBUMS
                            }
                        },
                        onSettings = { settings = true },
                    )
                }
                shownDestination.startsWith("album-") -> MediaGridScreen(
                    title = virtualAlbumTitle ?: albums.firstOrNull { it.bucketIds == albumFilter }?.name ?: "Album",
                    media = filtered,
                    selected = state.selectedIds,
                    query = query,
                    searchOpen = searchOpen,
                    onBack = {
                        albumFilter = null
                        virtualAlbumFilter = null
                        virtualAlbumTitle = null
                        virtualAlbumId = null
                        allAlbums = albumOpenedFromAll
                        albumOpenedFromAll = false
                    },
                    onSearch = { searchOpen = !searchOpen },
                    onQuery = { query = it },
                    onOpen = ::openItem,
                    onSelect = { id ->
                        val item = filtered.firstOrNull { it.id == id }
                        if (mediaPickerLaunch && !pickerAllowsMultiple && item != null) activity.finishMediaPick(listOf(item))
                        else viewModel.toggleSelection(id)
                    },
                    onSelectionSet = { ids -> if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.setSelection(ids) },
                    onRefresh = viewModel::refresh,
                    onCreate = { type, albumItems -> creativeRequest = CreativeRequest(type, albumItems) },
                )
                shownDestination == "tab-${PublicTab.PICTURES.name}" -> PicturesScreen(
                    media = filtered,
                    loading = state.loading,
                    selected = state.selectedIds,
                    query = query,
                    searchOpen = searchOpen,
                    onQuery = { query = it },
                    onSearch = { auxiliaryScreen = AuxiliaryScreen.SEARCH },
                    onOpen = ::openItem,
                    onSelect = { id ->
                        val item = filtered.firstOrNull { it.id == id }
                        if (mediaPickerLaunch && !pickerAllowsMultiple && item != null) activity.finishMediaPick(listOf(item))
                        else viewModel.toggleSelection(id)
                    },
                    onSelectionSet = { ids -> if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.setSelection(ids) },
                    onSelectAll = { if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.selectAll(filtered) },
                    onDuplicates = { auxiliaryScreen = AuxiliaryScreen.CLEAN_OUT },
                    onCreate = { type, items -> creativeRequest = CreativeRequest(type, items) },
                )
                shownDestination == "tab-${PublicTab.ALBUMS.name}" -> AlbumsScreen(
                    albums,
                    onViewAll = { allAlbums = true },
                    onOpen = { album ->
                        when (album.kind) {
                            AlbumKind.WHATSAPP_ROOT -> {
                                messagingSourceOpen = MessagingSource.WHATSAPP
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            AlbumKind.WHATSAPP_BUSINESS_ROOT -> {
                                messagingSourceOpen = MessagingSource.WHATSAPP_BUSINESS
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            AlbumKind.SMART -> {
                                albumFilter = null
                                virtualAlbumFilter = album.mediaIds.orEmpty()
                                virtualAlbumTitle = album.name
                                virtualAlbumId = album.bucketId
                                albumOpenedFromAll = false
                            }
                            else -> { albumFilter = album.bucketIds; albumOpenedFromAll = false }
                        }
                        selectedAlbumIds = emptySet()
                    },
                    onSettings = { settings = true },
                    onRefresh = viewModel::refresh,
                    onSmartAlbumsChanged = { smartAlbumVersion++ },
                    essentialAlbumIds = essentialAlbumIds,
                    onEssentialChanged = { ids ->
                        essentialAlbumIds = ids
                        galleryPreferences.edit().putStringSet("essential_album_ids", ids.mapTo(LinkedHashSet()) { it.toString() }).apply()
                    },
                    selected = selectedAlbumIds,
                    onToggleSelection = { id -> if (!mediaPickerLaunch) selectedAlbumIds = GalleryLogic.toggleSelection(selectedAlbumIds, id) },
                )
                shownDestination == "tab-${PublicTab.STORIES.name}" -> StoriesScreen(state.media) { storyItems = it }
                else -> PicturesScreen(
                    media = filtered, loading = state.loading, selected = state.selectedIds, query = query, searchOpen = searchOpen,
                    onQuery = { query = it }, onSearch = { auxiliaryScreen = AuxiliaryScreen.SEARCH },
                    onOpen = ::openItem,
                    onSelect = { id ->
                        val item = filtered.firstOrNull { it.id == id }
                        if (mediaPickerLaunch && !pickerAllowsMultiple && item != null) activity.finishMediaPick(listOf(item)) else viewModel.toggleSelection(id)
                    },
                    onSelectionSet = { ids -> if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.setSelection(ids) },
                    onSelectAll = { if (pickerAllowsMultiple || !mediaPickerLaunch) viewModel.selectAll(filtered) },
                    onDuplicates = { auxiliaryScreen = AuxiliaryScreen.CLEAN_OUT }, onCreate = { type, items -> creativeRequest = CreativeRequest(type, items) },
                )
            }
            }
        }
    }
    if (menuOpen) ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = VaultRaised, dragHandle = null) {
        PublicMenuSheet(
            onVideos = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.VIDEOS },
            onRecent = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.RECENT },
            onFavourites = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.FAVOURITES },
            onCleanOut = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.CLEAN_OUT },
            onLocations = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.LOCATIONS },
            onSharedAlbums = { menuOpen = false; auxiliaryScreen = AuxiliaryScreen.SHARED_ALBUMS },
            onSettings = { menuOpen = false; settings = true },
            onTrash = { menuOpen = false; trashOpen = true },
            onSecure = { menuOpen = false; launchSecureGallery(activity) },
        )
    }
}

private fun requiredMediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
private fun rememberPersistedGridState(
    preferences: android.content.SharedPreferences,
    key: String,
): LazyGridState {
    val state = rememberLazyGridState(
        initialFirstVisibleItemIndex = preferences.getInt("${key}_index", 0).coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = preferences.getInt("${key}_offset", 0).coerceAtLeast(0),
    )
    LaunchedEffect(state, key) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling) {
                preferences.edit()
                    .putInt("${key}_index", state.firstVisibleItemIndex)
                    .putInt("${key}_offset", state.firstVisibleItemScrollOffset)
                    .apply()
            }
        }
    }
    return state
}

@Composable
private fun PermissionGate(loading: Boolean, onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator() else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Outlined.PermMedia, null, Modifier.size(58.dp), tint = VaultBlue)
            Text("Choose the photos and videos Gallery can show", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("You can grant full or selected access. Gallery stays usable with only the items you choose.", color = VaultSecondary, textAlign = TextAlign.Center)
            Button(onClick = onGrant) { Text("Continue") }
        }
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Gallery could not load", style = MaterialTheme.typography.headlineMedium)
            Text(message, color = VaultSecondary)
            Button(onClick = retry) { Text("Retry") }
        }
    }
}

@Composable
internal fun GalleryToolbar(
    title: String? = null,
    back: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    beforeSearch: @Composable (() -> Unit)? = null,
    extra: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(88.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        if (title != null) Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis) else Spacer(Modifier.weight(1f))
        beforeSearch?.invoke()
        if (onSearch != null) IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search") }
        extra?.invoke()
    }
}

@Composable
private fun PicturesScreen(
    media: List<GalleryMedia>, loading: Boolean, selected: Set<Long>, query: String, searchOpen: Boolean,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (GalleryMedia, List<GalleryMedia>) -> Unit,
    onSelect: (Long) -> Unit, onSelectionSet: (Set<Long>) -> Unit, onSelectAll: () -> Unit,
    onDuplicates: () -> Unit,
    onCreate: (CreationType, List<GalleryMedia>) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var gridColumns by remember { mutableIntStateOf(preferences.getInt("grid_columns", 4).coerceIn(3, 12)) }
    var moreMenu by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(mediaSortFrom(preferences.getString("pictures_sort", null))) }
    val displayed = remember(media, sort) { media.sortedByPreference(sort) }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            CollectionSelectionHeader(
                items = displayed,
                selected = selected,
                keyOf = { it.id },
                dateOf = { it.dateTakenMs },
                onSelectionSet = onSelectionSet,
                onCancel = { onSelectionSet(emptySet()) },
            )
        } else {
            GalleryToolbar(
                onSearch = onSearch,
                extra = {
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("Select") }, onClick = { moreMenu = false; media.firstOrNull()?.let { onSelect(it.id) } })
                            DropdownMenuItem(text = { Text("Sort: ${sort.label}") }, onClick = { moreMenu = false; sortDialog = true })
                            DropdownMenuItem(text = { Text("Create") }, onClick = { moreMenu = false; createMenu = true })
                            DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { moreMenu = false; slideshow = true })
                            DropdownMenuItem(text = { Text("View duplicates") }, onClick = { moreMenu = false; onDuplicates() })
                        }
                    }
                },
            )
        }
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search names and albums") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (displayed.isEmpty()) EmptyGallery()
        else TimelineGrid(displayed, selected, gridColumns, sort == MediaSort.NEWEST || sort == MediaSort.OLDEST, {
            gridColumns = it
            preferences.edit().putInt("grid_columns", it).apply()
        }, { item -> onOpen(item, displayed) }, onSelect, onSelectionSet)
    }
    if (createMenu) CreateMediaSheet(onDismiss = { createMenu = false }) { type ->
        createMenu = false
        onCreate(type, displayed)
    }
    if (sortDialog) MediaSortDialog(sort, onSelect = { selectedSort -> sort = selectedSort; preferences.edit().putString("pictures_sort", selectedSort.name).apply(); sortDialog = false }, onDismiss = { sortDialog = false })
    if (slideshow && displayed.isNotEmpty()) StoryViewer(displayed, onBack = { slideshow = false })
}

@Composable
private fun EmptyGallery() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(54.dp), tint = VaultSecondary)
            Text("No pictures or videos", style = MaterialTheme.typography.titleLarge)
            Text("New media will appear here automatically.", color = VaultSecondary)
        }
    }
}

@Composable
private fun TimelineGrid(
    media: List<GalleryMedia>,
    selected: Set<Long>,
    gridColumns: Int,
    groupByDate: Boolean,
    onGridColumnsChanged: (Int) -> Unit,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val haptic = LocalHapticFeedback.current
    val format = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val grouped = remember(media, groupByDate) {
        if (groupByDate) media.groupBy { format.format(Date(it.dateTakenMs)) }.map { it.key to it.value }
        else listOf(null to media)
    }
    val orderedIds = remember(media) { media.map { it.id } }
    val gridState = rememberPersistedGridState(preferences, "pictures_scroll")
    val scope = rememberCoroutineScope()
    val latestSelected by rememberUpdatedState(selected)
    val latestSelectionSet by rememberUpdatedState(onSelectionSet)
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    var dragBase by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragSelect by remember { mutableStateOf(true) }
    val liveGridColumns = remember { mutableIntStateOf(gridColumns) }
    LaunchedEffect(gridColumns) { liveGridColumns.intValue = gridColumns }

    fun mediaAt(x: Float, y: Float): Long? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            info.key is Long && x >= info.offset.x && x <= info.offset.x + info.size.width &&
                y >= info.offset.y && y <= info.offset.y + info.size.height
        }?.key as? Long

    fun updateSlideSelection(current: Long) {
        val anchor = dragAnchor ?: return
        latestSelectionSet(GalleryLogic.slideSelection(orderedIds, dragBase, anchor, current, dragSelect))
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(liveGridColumns.intValue),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier.fillMaxSize()
            .semantics { contentDescription = "Pictures grid, ${liveGridColumns.intValue} columns" }
            .pinchToResizeGrid(
            columns = liveGridColumns,
            minimum = 3,
            maximum = 12,
            gridState = gridState,
            onChanged = onGridColumnsChanged,
        ).pointerInput(media) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                mediaAt(longPress.position.x, longPress.position.y)?.let { id ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragAnchor = id
                    dragBase = latestSelected
                    dragSelect = id !in latestSelected
                    updateSlideSelection(id)
                }
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                    when {
                        change.position.y < 88f -> scope.launch { gridState.scrollBy(-30f) }
                        change.position.y > viewportHeight - 88f -> scope.launch { gridState.scrollBy(30f) }
                    }
                    mediaAt(change.position.x, change.position.y)?.let(::updateSlideSelection)
                }
                dragAnchor = null
            }
        },
    ) {
        grouped.forEach { (date, items) ->
            if (date != null) item(span = { GridItemSpan(maxLineSpan) }, key = "date-$date") {
                Text(date, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 2.dp, vertical = 15.dp))
            }
            items(items, key = { it.id }) { item -> MediaTile(item, item.id in selected, onOpen, onSelect) }
        }
    }
}

@Composable
private fun MediaTile(item: GalleryMedia, selected: Boolean, onOpen: (GalleryMedia) -> Unit, onSelect: ((Long) -> Unit)?) {
    val context = LocalContext.current
    val thumbnail = remember(item.uri, item.kind) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .apply { if (item.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }
            .memoryCacheKey("gallery-thumb-${item.id}-${item.dateTakenMs}")
            .diskCacheKey("gallery-thumb-${item.id}-${item.dateTakenMs}")
            .build()
    }
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(1.dp)).background(VaultSurface)
            .then(if (onSelect != null) Modifier.semantics { onLongClick("Select") { onSelect(item.id); true } } else Modifier)
            .combinedClickable(onClick = { onOpen(item) }, onLongClick = { onSelect?.invoke(item.id) }),
    ) {
        GalleryThumbnail(item, thumbnail)
        if (item.kind == MediaKind.VIDEO) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(14.dp), tint = Color.White)
                Text(GalleryLogic.durationLabel(item.durationMs), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color(0x55000000)))
            Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(25.dp).clip(CircleShape).background(VaultPrimary), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.Black)
            }
        }
    }
}

@Composable
private fun GalleryThumbnail(item: GalleryMedia, fallback: ImageRequest) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, item.uri, item.dateTakenMs) {
        if (item.kind == MediaKind.VIDEO && Build.VERSION.SDK_INT >= 29) {
            value = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.loadThumbnail(item.uri, Size(640, 640), null) }.getOrNull()
            }
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else AsyncImage(fallback, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

@Composable
private fun AlbumsScreen(
    albums: List<GalleryAlbum>,
    onViewAll: () -> Unit,
    onOpen: (GalleryAlbum) -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSmartAlbumsChanged: () -> Unit,
    essentialAlbumIds: Set<Long>,
    onEssentialChanged: (Set<Long>) -> Unit,
    selected: Set<Long>,
    onToggleSelection: (Long) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val showEssential = preferences.getBoolean("essential", true)
    var showEducation by remember { mutableStateOf(showEssential && !preferences.getBoolean("albums_education_dismissed", false)) }
    var hiddenAlbumIds by remember { mutableStateOf(preferences.getStringSet("hidden_album_ids", emptySet()).orEmpty()) }
    var sortMode by remember { mutableStateOf(preferences.getString("album_sort", "Custom order").orEmpty()) }
    val visibleAlbums = remember(albums, hiddenAlbumIds, sortMode) {
        val shown = albums.filterNot { it.bucketId.toString() in hiddenAlbumIds }
        (when (sortMode) {
            "Name (A to Z)" -> shown.sortedBy { it.name.lowercase(Locale.getDefault()) }
            "Name (Z to A)" -> shown.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            "Items (most to fewest)" -> shown.sortedByDescending { it.count }
            "Items (fewest to most)" -> shown.sortedBy { it.count }
            else -> shown
        }).withNavigationAlbumsFirst()
    }
    var menu by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var createSmart by remember { mutableStateOf(false) }
    var manageSmart by remember { mutableStateOf(false) }
    var smartRules by remember(albums) { mutableStateOf(SmartAlbumRepository(context).load()) }
    var newAlbumName by remember { mutableStateOf("") }
    var hideAlbums by remember { mutableStateOf(false) }
    var chooseEssential by remember { mutableStateOf(false) }
    var chooseSort by remember { mutableStateOf(false) }
    var sortBeforeDialog by remember { mutableStateOf(sortMode) }
    val scope = rememberCoroutineScope()
    val createMediaPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(250)) { uris ->
        val albumName = newAlbumName.trim()
        if (uris.isNotEmpty() && albumName.isNotBlank()) scope.launch {
            val copied = withContext(Dispatchers.IO) { copyUrisToAlbum(context, uris, albumName) }
            Toast.makeText(context, if (copied == uris.size) "$albumName created with $copied items" else "Copied $copied of ${uris.size} items", Toast.LENGTH_LONG).show()
            newAlbumName = ""
            onRefresh()
        }
    }
    val essentialAlbums = if (essentialAlbumIds.isEmpty()) visibleAlbums.take(12) else visibleAlbums.filter { it.bucketId in essentialAlbumIds }
    val displayedAlbums = if (showEssential) essentialAlbums else visibleAlbums
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, displayedAlbums.size, {
            if (selected.size == displayedAlbums.size) selected.toList().forEach(onToggleSelection)
            else displayedAlbums.filterNot { it.bucketId in selected }.forEach { onToggleSelection(it.bucketId) }
        }) {
            selected.toList().forEach(onToggleSelection)
        } else GalleryToolbar(extra = {
            IconButton(onClick = { create = true }) { Icon(Icons.Outlined.Add, "Create") }
            IconButton(onClick = onViewAll) { Icon(Icons.Outlined.Search, "Search") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; visibleAlbums.firstOrNull()?.let { onToggleSelection(it.bucketId) } })
                    DropdownMenuItem(text = { Text("Create smart album") }, onClick = { menu = false; createSmart = true })
                    if (smartRules.isNotEmpty()) DropdownMenuItem(text = { Text("Manage smart albums") }, onClick = { menu = false; manageSmart = true })
                    DropdownMenuItem(text = { Text("Sort") }, onClick = { menu = false; sortBeforeDialog = sortMode; chooseSort = true })
                    DropdownMenuItem(text = { Text("Choose essential albums") }, onClick = { menu = false; chooseEssential = true })
                    DropdownMenuItem(text = { Text("Hide albums") }, onClick = { menu = false; hideAlbums = true })
                }
            }
        })
        if (showEducation) Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("To show all your albums on the Albums tab, turn off Select essential albums in Settings.", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { preferences.edit().putBoolean("albums_education_dismissed", true).apply(); showEducation = false }) { Text("Not now") }
                    TextButton(onClick = { preferences.edit().putBoolean("albums_education_dismissed", true).apply(); showEducation = false; onSettings() }) { Text("Settings") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (showEssential) "Essential albums" else "All albums", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text("View all") }
        }
        AlbumGrid(displayedAlbums, selected, onOpen, onToggleSelection, Modifier.weight(1f), "albums_home_scroll")
    }
    if (create) AlertDialog(
        onDismissRequest = { create = false },
        title = { Text("Create album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Name the album, then choose the pictures and videos to copy into it.")
                OutlinedTextField(newAlbumName, { newAlbumName = it.take(60) }, label = { Text("Album name") }, singleLine = true)
            }
        },
        dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = newAlbumName.trim().isNotBlank(), onClick = {
            create = false
            createMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }) { Text("Choose media") } },
    )
    if (createSmart) SmartAlbumEditorDialog(
        onDismiss = { createSmart = false },
        onSave = { rule ->
            SmartAlbumRepository(context).save(rule)
            smartRules = SmartAlbumRepository(context).load()
            createSmart = false
            onSmartAlbumsChanged()
            Toast.makeText(context, "${rule.name} updates automatically", Toast.LENGTH_SHORT).show()
        },
    )
    if (manageSmart) AlertDialog(
        onDismissRequest = { manageSmart = false },
        title = { Text("Smart albums") },
        text = {
            Column(Modifier.heightIn(max = currentWindowHeightDp() * .55f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                smartRules.forEach { rule ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(
                                    rule.mediaType.takeIf { it != SmartMediaType.ALL }?.name?.lowercase(),
                                    rule.folderContains.takeIf(String::isNotBlank)?.let { "folder: $it" },
                                    rule.indexedText.takeIf(String::isNotBlank)?.let { "text: $it" },
                                    rule.withinDays.takeIf { it > 0 }?.let { "last $it days" },
                                ).joinToString(" • ").ifBlank { "All media" },
                                color = VaultSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = {
                            SmartAlbumRepository(context).delete(rule.id)
                            smartRules = SmartAlbumRepository(context).load()
                            onSmartAlbumsChanged()
                            if (smartRules.isEmpty()) manageSmart = false
                        }) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { manageSmart = false }) { Text("Done") } },
    )
    if (hideAlbums) AlertDialog(
        onDismissRequest = { hideAlbums = false },
        title = { Text("Hide albums") },
        text = {
            Column(Modifier.heightIn(max = currentWindowHeightDp() * .55f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                albums.forEach { album ->
                    val hidden = album.bucketId.toString() in hiddenAlbumIds
                    Row(Modifier.fillMaxWidth().clickable {
                        hiddenAlbumIds = hiddenAlbumIds.toMutableSet().apply { if (hidden) remove(album.bucketId.toString()) else add(album.bucketId.toString()) }
                        preferences.edit().putStringSet("hidden_album_ids", hiddenAlbumIds).apply()
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(album.name, modifier = Modifier.weight(1f), maxLines = 1)
                        Switch(checked = hidden, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { hideAlbums = false }) { Text("Done") } },
    )
    if (chooseEssential) AlertDialog(
        onDismissRequest = { chooseEssential = false },
        title = { Text("Essential albums") },
        text = {
            Column(Modifier.heightIn(max = currentWindowHeightDp() * .58f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text("Choose exactly what appears on the Albums tab. You can change this anytime.", color = VaultSecondary, modifier = Modifier.padding(bottom = 12.dp))
                visibleAlbums.forEach { album ->
                    val checked = album.bucketId in essentialAlbumIds
                    Row(Modifier.fillMaxWidth().clickable {
                        onEssentialChanged(essentialAlbumIds.toMutableSet().apply { if (checked) remove(album.bucketId) else add(album.bucketId) })
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(album.name, modifier = Modifier.weight(1f), maxLines = 1)
                        Switch(checked = checked, onCheckedChange = null)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { onEssentialChanged(emptySet()) }) { Text("Use suggested") } },
        confirmButton = { TextButton(onClick = { chooseEssential = false }) { Text("Done") } },
    )
    if (chooseSort) AlertDialog(
        onDismissRequest = { sortMode = sortBeforeDialog; chooseSort = false },
        title = { Text("Sort albums") },
        text = { Column {
            listOf("Custom order", "Name (A to Z)", "Name (Z to A)", "Items (most to fewest)", "Items (fewest to most)").forEach { option ->
                TextButton(onClick = {
                    sortMode = option
                }, modifier = Modifier.fillMaxWidth()) { Text(if (sortMode == option) "✓  $option" else option) }
            }
        } },
        dismissButton = { TextButton(onClick = { sortMode = sortBeforeDialog; chooseSort = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { preferences.edit().putString("album_sort", sortMode).apply(); chooseSort = false }) { Text("Done") } },
    )
}

@Composable
private fun SmartAlbumEditorDialog(
    onDismiss: () -> Unit,
    onSave: (SmartAlbumRule) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf(SmartMediaType.ALL) }
    var folder by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var indexedText by remember { mutableStateOf("") }
    var favouritesOnly by remember { mutableStateOf(false) }
    var withinDays by remember { mutableStateOf("") }
    var minimumDuration by remember { mutableStateOf("") }
    var minimumSize by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create smart album") },
        text = {
            Column(
                Modifier.heightIn(max = currentWindowHeightDp() * .68f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("A live view of matching media. Files stay in their original folders.", color = VaultSecondary)
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Album name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Media type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartMediaType.entries.forEach { type ->
                        FilterChip(
                            selected = mediaType == type,
                            onClick = { mediaType = type },
                            label = { Text(when (type) { SmartMediaType.ALL -> "All"; SmartMediaType.PHOTOS -> "Photos"; SmartMediaType.VIDEOS -> "Videos" }) },
                        )
                    }
                }
                OutlinedTextField(folder, { folder = it.take(80) }, label = { Text("Folder contains") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(filename, { filename = it.take(80) }, label = { Text("Filename contains") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    indexedText,
                    { indexedText = it.take(120) },
                    label = { Text("Tag, camera or recognized text") },
                    supportingText = { Text("Matches the private on-device search index") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().clickable { favouritesOnly = !favouritesOnly }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Favourites only", Modifier.weight(1f))
                    Switch(favouritesOnly, null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(withinDays, { withinDays = it.filter(Char::isDigit).take(4) }, label = { Text("Within days") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(minimumSize, { minimumSize = it.filter(Char::isDigit).take(6) }, label = { Text("Min MB") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                if (mediaType != SmartMediaType.PHOTOS) {
                    OutlinedTextField(minimumDuration, { minimumDuration = it.filter(Char::isDigit).take(6) }, label = { Text("Minimum video seconds") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Text("Blank fields are ignored. All enabled rules must match.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotBlank(),
                onClick = {
                    onSave(
                        SmartAlbumRule(
                            name = name.trim(),
                            mediaType = mediaType,
                            folderContains = folder.trim(),
                            filenameContains = filename.trim(),
                            indexedText = indexedText.trim(),
                            favouritesOnly = favouritesOnly,
                            withinDays = withinDays.toIntOrNull()?.coerceIn(0, 3_650) ?: 0,
                            minimumDurationSeconds = minimumDuration.toIntOrNull()?.coerceIn(0, 86_400) ?: 0,
                            minimumSizeMb = minimumSize.toIntOrNull()?.coerceIn(0, 100_000) ?: 0,
                        ),
                    )
                },
            ) { Text("Create") }
        },
    )
}

@Composable
private fun AllAlbumsScreen(
    albums: List<GalleryAlbum>, selected: Set<Long>, onBack: () -> Unit, onOpen: (GalleryAlbum) -> Unit,
    onToggleSelection: (Long) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(preferences.getString("all_album_sort", "Name (A to Z)").orEmpty()) }
    val filtered = remember(albums, query, sortMode) {
        albums.filter { query.isBlank() || it.name.contains(query, true) }.let { matches ->
            when (sortMode) {
                "Name (Z to A)" -> matches.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
                "Items (most to fewest)" -> matches.sortedByDescending { it.count }
                "Items (fewest to most)" -> matches.sortedBy { it.count }
                "Newest" -> matches.sortedByDescending { it.cover.dateTakenMs }
                "Oldest" -> matches.sortedBy { it.cover.dateTakenMs }
                else -> matches.sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
        }.withNavigationAlbumsFirst()
    }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, filtered.size, {
            if (selected.size == filtered.size) selected.toList().forEach(onToggleSelection)
            else filtered.filterNot { it.bucketId in selected }.forEach { onToggleSelection(it.bucketId) }
        }) { selected.toList().forEach(onToggleSelection) }
        else GalleryToolbar("All albums", onBack, onSearch = { searchOpen = !searchOpen }, extra = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Album options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("Name (A to Z)", "Name (Z to A)", "Newest", "Oldest", "Items (most to fewest)", "Items (fewest to most)").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            trailingIcon = { if (sortMode == option) Icon(Icons.Outlined.Check, null) },
                            onClick = { sortMode = option; preferences.edit().putString("all_album_sort", option).apply(); menu = false },
                        )
                    }
                }
            }
        })
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(query, { query = it }, label = { Text("Search albums") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }
        AlbumGrid(filtered, selected, onOpen, onToggleSelection, Modifier.weight(1f), "albums_all_scroll")
    }
}

@Composable
private fun MessagingAlbumsScreen(
    catalog: MessagingCatalog?,
    organizationMode: MessagingOrganizationMode,
    selected: Set<Long>,
    onBack: () -> Unit,
    onOpen: (GalleryAlbum) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onOrganizationModeChanged: (MessagingSource, MessagingOrganizationMode) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val albums = catalog?.albums.orEmpty()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val sortKey = "messaging_sort_${catalog?.source?.storageId ?: "messages"}"
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var sortMode by remember(sortKey) { mutableStateOf(preferences.getString(sortKey, "Name (A to Z)") ?: "Name (A to Z)") }
    val filtered = remember(albums, query, sortMode) {
        albums.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }.let { matches ->
            when (sortMode) {
                "Name (Z to A)" -> matches.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
                "Newest" -> matches.sortedByDescending { it.cover.dateTakenMs }
                "Oldest" -> matches.sortedBy { it.cover.dateTakenMs }
                "Items (most to fewest)" -> matches.sortedByDescending { it.count }
                "Items (fewest to most)" -> matches.sortedBy { it.count }
                else -> matches.sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            SelectionHeader(
                selected.size,
                filtered.size,
                {
                    if (selected.size == filtered.size) selected.toList().forEach(onToggleSelection)
                    else filtered.filterNot { it.bucketId in selected }.forEach { onToggleSelection(it.bucketId) }
                },
                { selected.toList().forEach(onToggleSelection) },
            )
        } else {
            GalleryToolbar(catalog?.source?.title ?: "Messages", onBack, extra = {
                IconButton(onClick = { searchOpen = !searchOpen }) { Icon(Icons.Outlined.Search, "Search conversations") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Messaging album settings") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "WhatsApp album options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        Text("Organization", color = VaultSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        listOf(
                            "By conversation" to MessagingOrganizationMode.CONVERSATIONS,
                            "Original WhatsApp folders" to MessagingOrganizationMode.ORIGINAL_FOLDERS,
                        ).forEach { (label, mode) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                trailingIcon = { if (organizationMode == mode) Icon(Icons.Outlined.Check, null) },
                                onClick = {
                                    catalog?.source?.let { onOrganizationModeChanged(it, mode) }
                                    menuOpen = false
                                },
                            )
                        }
                        HorizontalDivider(color = Color(0xFF38383D))
                        Text("Sort conversations", color = VaultSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        listOf("Name (A to Z)", "Name (Z to A)", "Newest", "Oldest", "Items (most to fewest)", "Items (fewest to most)").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                trailingIcon = { if (sortMode == option) Icon(Icons.Outlined.Check, null) },
                                onClick = {
                                    sortMode = option
                                    preferences.edit().putString(sortKey, option).apply()
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            })
        }
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                query,
                { query = it },
                label = { Text("Search contacts and groups") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (catalog == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (catalog.indexed) "Organized by conversation" else "Chat organization needs setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                val status = when {
                    !catalog.indexed -> "Your existing files remain available under Unassigned. Add the backup key in Settings to organize them."
                    catalog.backupTimestampMs > 0L -> "Backup indexed ${SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(catalog.backupTimestampMs))}"
                    else -> "Local backup index loaded"
                }
                Text(status, color = VaultSecondary, style = MaterialTheme.typography.bodyMedium)
                if (catalog.unmatchedCount > 0) {
                    Text("${catalog.unmatchedCount} newer or unmatched visual items", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (albums.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.FolderShared, null, Modifier.size(54.dp), tint = VaultSecondary)
                    Text("No ${catalog.source.title} media found", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onSettings) { Text("Open setup") }
                }
            }
        } else {
            AlbumGrid(
                filtered,
                selected,
                onOpen,
                onToggleSelection,
                Modifier.weight(1f),
                "messaging_${catalog.source.storageId}_scroll",
            )
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<GalleryAlbum>,
    selected: Set<Long>,
    onOpen: (GalleryAlbum) -> Unit,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
    stateKey: String = "album_grid_scroll",
) {
    if (albums.isEmpty()) { EmptyGallery(); return }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val columns = remember {
        mutableIntStateOf(if (preferences.getInt("album_grid_columns", 3) == 1) 1 else 3)
    }
    val gridState = rememberPersistedGridState(preferences, stateKey)
    val listMode = columns.intValue == 1
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns.intValue),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize().pinchToResizeGrid(columns, 1, 3, gridState, semanticExtremes = true) {
            preferences.edit().putInt("album_grid_columns", it).apply()
        },
    ) {
        items(albums, key = { it.bucketId }) { album ->
            val coverRequest = remember(album.cover.uri, album.cover.dateTakenMs) {
                ImageRequest.Builder(context).data(album.cover.uri)
                    .apply { if (album.cover.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }
                    .memoryCacheKey("album-cover-${album.cover.id}-${album.cover.dateTakenMs}")
                    .build()
            }
            val interaction = Modifier.combinedClickable(
                onClick = { if (selected.isNotEmpty()) onToggleSelection(album.bucketId) else onOpen(album) },
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleSelection(album.bucketId) },
            )
            if (listMode) {
                Row(interaction.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(104.dp)) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) { GalleryThumbnail(album.cover, coverRequest) }
                        if (album.bucketId in selected) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0x55000000)))
                            Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(CircleShape).background(VaultBlue), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Check, "Selected", Modifier.size(19.dp), tint = Color.White)
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                        Text(
                            buildString {
                                append("${album.count} items")
                                album.subtitle?.let { append("  ·  $it") }
                            },
                            color = VaultSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            } else {
                Column(interaction) {
                    Box {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))) { GalleryThumbnail(album.cover, coverRequest) }
                        if (album.bucketId in selected) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0x55000000)))
                            Box(Modifier.align(Alignment.TopStart).padding(9.dp).size(28.dp).clip(CircleShape).background(VaultBlue), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Check, "Selected", Modifier.size(19.dp), tint = Color.White)
                            }
                        }
                    }
                    Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 7.dp))
                    Text(
                        album.subtitle?.let { "${album.count}  ·  $it" } ?: album.count.toString(),
                        color = VaultSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaGridScreen(
    title: String,
    media: List<GalleryMedia>,
    selected: Set<Long>,
    query: String,
    searchOpen: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onQuery: (String) -> Unit,
    onOpen: (GalleryMedia, List<GalleryMedia>) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
    onRefresh: () -> Unit,
    onCreate: (CreationType, List<GalleryMedia>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val sortKey = remember(title) { "album_sort_${title.hashCode()}" }
    var sort by remember(title) { mutableStateOf(mediaSortFrom(preferences.getString(sortKey, null))) }
    var sortDialog by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    val displayed = remember(media, sort) { media.sortedByPreference(sort) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(250)) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val copied = withContext(Dispatchers.IO) { copyUrisToAlbum(context, uris, title) }
            Toast.makeText(context, "Added $copied of ${uris.size} items", Toast.LENGTH_LONG).show()
            onRefresh()
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) CollectionSelectionHeader(
            items = displayed,
            selected = selected,
            keyOf = { it.id },
            dateOf = { it.dateTakenMs },
            onSelectionSet = onSelectionSet,
            onCancel = { onSelectionSet(emptySet()) },
        ) else GalleryToolbar(title, onBack, onSearch, beforeSearch = {
            IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Icon(Icons.Outlined.Add, "Add") }
        }, extra = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; displayed.firstOrNull()?.let { onSelect(it.id) } })
                    DropdownMenuItem(text = { Text("Select all") }, onClick = { menu = false; onSelectionSet(displayed.mapTo(LinkedHashSet()) { it.id }) })
                    DropdownMenuItem(text = { Text("Create") }, onClick = { menu = false; createDialog = true })
                    DropdownMenuItem(text = { Text("Add to Home screen") }, onClick = { menu = false; pinCollectionShortcut(context, title) })
                    DropdownMenuItem(text = { Text("Sort: ${sort.label}") }, onClick = { menu = false; sortDialog = true })
                    DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { menu = false; displayed.firstOrNull()?.let { onOpen(it, displayed) } })
                }
            }
        })
        Text(mediaKindSummary(displayed), color = VaultSecondary, modifier = Modifier.padding(horizontal = 62.dp).padding(top = 0.dp, bottom = 4.dp))
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search this album") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (displayed.isEmpty()) EmptyGallery() else SlideSelectableMediaGrid(
            displayed,
            selected,
            { onOpen(it, displayed) },
            onSelect,
            onSelectionSet,
            stateKey = "media_${title.hashCode()}_scroll",
        )
    }
    if (sortDialog) MediaSortDialog(sort, onSelect = { selectedSort -> sort = selectedSort; preferences.edit().putString(sortKey, selectedSort.name).apply(); sortDialog = false }, onDismiss = { sortDialog = false })
    if (createDialog) CreateMediaSheet(onDismiss = { createDialog = false }) { type ->
        createDialog = false
        onCreate(type, displayed)
    }
}

@Composable
private fun SlideSelectableMediaGrid(
    media: List<GalleryMedia>,
    selected: Set<Long>,
    onOpen: (GalleryMedia) -> Unit,
    onSelect: (Long) -> Unit,
    onSelectionSet: (Set<Long>) -> Unit,
    stateKey: String = "media_grid_scroll",
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    val gridColumns = remember { mutableIntStateOf(preferences.getInt("grid_columns", 4).coerceIn(3, 12)) }
    val gridState = rememberPersistedGridState(preferences, stateKey)
    val orderedIds = remember(media) { media.map { it.id } }
    val scope = rememberCoroutineScope()
    val latestSelected by rememberUpdatedState(selected)
    val latestSelectionSet by rememberUpdatedState(onSelectionSet)
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    var dragBase by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragSelect by remember { mutableStateOf(true) }

    fun mediaAt(x: Float, y: Float): Long? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            info.key is Long && x >= info.offset.x && x <= info.offset.x + info.size.width &&
                y >= info.offset.y && y <= info.offset.y + info.size.height
        }?.key as? Long

    fun updateSlideSelection(current: Long) {
        val anchor = dragAnchor ?: return
        latestSelectionSet(GalleryLogic.slideSelection(orderedIds, dragBase, anchor, current, dragSelect))
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns.intValue),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).pinchToResizeGrid(gridColumns, 3, 12, gridState) {
            preferences.edit().putInt("grid_columns", it).apply()
        }.pointerInput(media) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                mediaAt(longPress.position.x, longPress.position.y)?.let { id ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragAnchor = id
                    dragBase = latestSelected
                    dragSelect = id !in latestSelected
                    updateSlideSelection(id)
                }
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                    when {
                        change.position.y < 88f -> scope.launch { gridState.scrollBy(-30f) }
                        change.position.y > viewportHeight - 88f -> scope.launch { gridState.scrollBy(30f) }
                    }
                    mediaAt(change.position.x, change.position.y)?.let(::updateSlideSelection)
                }
                dragAnchor = null
            }
        },
    ) { items(media, key = { it.id }) { MediaTile(it, it.id in selected, onOpen, onSelect) } }
}

@Composable
private fun StoriesScreen(media: List<GalleryMedia>, onOpen: (List<GalleryMedia>) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var menu by remember { mutableStateOf(false) }
    var hideScreenshots by remember { mutableStateOf(preferences.getBoolean("stories_hide_screenshots", false)) }
    var autoStories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    val format = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val stories = remember(media, hideScreenshots, autoStories) {
        if (!autoStories) emptyList() else media.filter { it.kind == MediaKind.IMAGE && (!hideScreenshots || !it.bucketName.contains("screenshot", true)) }
            .groupBy { format.format(Date(it.dateTakenMs)) }
            .entries.map { it.key to it.value }.filter { it.second.size >= 2 }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Create story") }, onClick = {
                            menu = false
                            val recent = media.filter { it.kind == MediaKind.IMAGE }.take(30)
                            if (recent.isNotEmpty()) onOpen(recent) else Toast.makeText(context, "Add photos before creating a story", Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text(if (hideScreenshots) "Show screenshots in stories" else "Hide screenshots from stories") }, onClick = {
                            menu = false; hideScreenshots = !hideScreenshots; preferences.edit().putBoolean("stories_hide_screenshots", hideScreenshots).apply()
                        })
                    }
                }
            },
        )
        if (stories.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 42.dp)) {
                Text("No stories", style = MaterialTheme.typography.headlineMedium, color = VaultSecondary)
                Spacer(Modifier.height(14.dp))
                Text(if (autoStories) "Experience your adventures again in curated collections automatically made from your pictures and videos." else "Automatic stories are off. You can turn them back on here at any time.", textAlign = TextAlign.Center, color = VaultSecondary)
                if (!autoStories) Button(onClick = {
                    autoStories = true
                    preferences.edit().putBoolean("auto_stories", true).apply()
                }, modifier = Modifier.padding(top = 18.dp)) { Text("Turn on automatic stories") }
            }
        } else LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(stories, key = { it.first }) { (title, items) ->
                Column(Modifier.combinedClickable(onClick = { onOpen(items) }, onLongClick = { onOpen(items) })) {
                    AsyncImage(items.first().uri, title, Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("${items.size} photos", color = VaultSecondary)
                }
            }
        }
    }
}

@Composable
private fun StoryViewer(items: List<GalleryMedia>, onBack: () -> Unit) {
    var index by remember(items) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    LaunchedEffect(index, items, paused) {
        if (!paused) {
            delay(3_500)
            if (index < items.lastIndex) index++ else onBack()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(items[index].uri, items[index].name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Text("${index + 1} / ${items.size}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Row(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 48.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index > 0) index-- }, onLongClick = { paused = !paused }))
            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick = { if (index < items.lastIndex) index++ else onBack() }, onLongClick = { paused = !paused }))
        }
        if (paused) Icon(Icons.Outlined.Pause, "Story paused", Modifier.align(Alignment.Center).size(58.dp).clip(CircleShape).background(Color(0x99000000)).padding(12.dp))
    }
}

@Composable
private fun SamsungCollectionScreen(title: String, media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia, List<GalleryMedia>) -> Unit, onCreate: (CreationType, List<GalleryMedia>) -> Unit) {
    val context = LocalContext.current
    val preferences = remember(title) { context.getSharedPreferences("collection-settings", 0) }
    var menu by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var sort by remember(title) { mutableStateOf(mediaSortFrom(preferences.getString("sort_$title", null))) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var create by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    var transferMove by remember { mutableStateOf<Boolean?>(null) }
    val gridColumns = remember(title) { mutableIntStateOf(preferences.getInt("grid_columns_$title", 4).coerceIn(3, 12)) }
    val displayed = remember(media, sort) { media.sortedByPreference(sort) }
    val selectedMedia = remember(displayed, selected) { displayed.filter { it.id in selected } }
    val destinationAlbums = remember(media) { MediaStoreRepository(context).albums(media, mergeMatchingNames = true) }
    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) SelectionHeader(selected.size, media.size, { selected = media.mapTo(LinkedHashSet()) { it.id } }, { selected = emptySet() })
        else GalleryToolbar(
            title = title,
            back = onBack,
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Select") }, onClick = { menu = false; media.firstOrNull()?.let { selected = setOf(it.id) } })
                        DropdownMenuItem(text = { Text("Sort: ${sort.label}") }, onClick = { menu = false; sortDialog = true })
                        DropdownMenuItem(text = { Text("Create") }, onClick = { menu = false; create = true })
                        DropdownMenuItem(text = { Text("Add to Home screen") }, onClick = { menu = false; pinCollectionShortcut(context, title) })
                        DropdownMenuItem(text = { Text("Start slideshow") }, onClick = { menu = false; if (media.isNotEmpty()) slideshow = true else Toast.makeText(context, "This collection is empty", Toast.LENGTH_SHORT).show() })
                    }
                }
            },
        )
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(52.dp), tint = VaultSecondary)
                    Text(if (title == "Favourites") "No favourites" else "No pictures or videos", style = MaterialTheme.typography.titleLarge)
                    Text(if (title == "Favourites") "Tap the heart on your favourite shots so you can find them here fast." else "New media will appear here automatically.", color = VaultSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 36.dp))
                }
            }
        } else {
            Text(mediaKindSummary(media), color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.intValue),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).pinchToResizeGrid(gridColumns, 3, 12) {
                    preferences.edit().putInt("grid_columns_$title", it).apply()
                },
            ) { items(displayed, key = { it.id }) { item -> MediaTile(item, item.id in selected, { if (selected.isNotEmpty()) selected = GalleryLogic.toggleSelection(selected, it.id) else onOpen(it, displayed) }, { selected = GalleryLogic.toggleSelection(selected, it) }) } }
            if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth().navigationBarsPadding().height(82.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomAction(Icons.Outlined.ContentCopy, "Copy") { transferMove = false }
                BottomAction(Icons.Outlined.Collections, "Move") { transferMove = true }
                BottomAction(Icons.Outlined.Share, "Share") { (context as? Activity)?.let { shareUris(it, selectedMedia.map { mediaItem -> mediaItem.uri }, "*/*") } }
                Box {
                    BottomAction(Icons.Outlined.MoreVert, "More") { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Create") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }, onClick = { menu = false; create = true })
                        DropdownMenuItem(text = { Text("Copy to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { menu = false; (context as? MainGalleryActivity)?.transferMediaToSecure(selectedMedia.map { it.uri }, false); selected = emptySet() })
                        DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { menu = false; (context as? MainGalleryActivity)?.transferMediaToSecure(selectedMedia.map { it.uri }, true); selected = emptySet() })
                        DropdownMenuItem(text = { Text("Move to recycle bin") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; (context as? MainGalleryActivity)?.deleteMedia(selectedMedia.map { it.uri }); selected = emptySet() })
                    }
                }
            }
        }
    }
    transferMove?.let { move -> PublicAlbumDestinationPicker(
        move = move,
        albums = destinationAlbums,
        sourceAlbumNames = selectedMedia.mapTo(LinkedHashSet()) { it.bucketName },
        onDismiss = { transferMove = null },
        onSelect = { target ->
            transferMove = null
            (context as? MainGalleryActivity)?.transferMediaToAlbum(selectedMedia, target, move)
            selected = emptySet()
        },
    ) }
    if (sortDialog) MediaSortDialog(sort, onSelect = { value -> sort = value; preferences.edit().putString("sort_$title", value.name).apply(); sortDialog = false }, onDismiss = { sortDialog = false })
    if (slideshow) StoryViewer(displayed, onBack = { slideshow = false })
    if (create) CreateMediaSheet(onDismiss = { create = false }) { type ->
        val source = if (selectedMedia.isNotEmpty()) selectedMedia else displayed
        create = false
        onCreate(type, source)
    }
}

@Composable
private fun GallerySearchScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-search-settings", 0) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(mediaSortFrom(preferences.getString("result_sort", null))) }
    var settingsOpen by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<Pair<String, List<GalleryMedia>>?>(null) }
    var mediaTypeFilter by remember { mutableStateOf("All") }
    var favouriteFilter by remember { mutableStateOf(false) }
    var recentFilter by remember { mutableStateOf(false) }
    var largeFilter by remember { mutableStateOf(false) }
    var longVideoFilter by remember { mutableStateOf(false) }
    var folderFilter by remember { mutableStateOf<String?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    val gridColumns = remember { mutableIntStateOf(preferences.getInt("search_grid_columns", 4).coerceIn(3, 12)) }
    val indexedIds by produceState<Set<Long>>(initialValue = emptySet(), query) {
        value = if (query.isBlank()) emptySet() else withContext(Dispatchers.IO) {
            runCatching { GallerySearchIndex(context).use { it.searchPublicIds(query) } }.getOrDefault(emptySet())
        }
    }
    val results = remember(media, query, sort, indexedIds, mediaTypeFilter, favouriteFilter, recentFilter, largeFilter, longVideoFilter, folderFilter) {
        if (query.isBlank()) emptyList() else media.filter {
            (it.id in indexedIds || it.name.contains(query, true) || it.bucketName.contains(query, true) || it.mimeType.contains(query, true)) &&
                matchesGallerySearchFilters(it, mediaTypeFilter, favouriteFilter, recentFilter, largeFilter, longVideoFilter, folderFilter)
        }.sortedByPreference(sort)
    }
    if (settingsOpen) {
        SearchSettingsScreen(preferences, onBack = { settingsOpen = false })
        return
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            title = category?.first,
            back = { if (category != null) category = null else onBack() },
            extra = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Sort results: ${sort.label}") }, onClick = { menu = false; sortDialog = true })
                        DropdownMenuItem(text = { Text("Search settings") }, onClick = { menu = false; settingsOpen = true })
                    }
                }
            },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            lazyRowItems(listOf("All", "Photos", "Videos")) { type ->
                FilterChip(mediaTypeFilter == type, { mediaTypeFilter = type }, label = { Text(type) })
            }
            item { FilterChip(favouriteFilter, { favouriteFilter = !favouriteFilter }, label = { Text("Favourites") }) }
            item { FilterChip(recentFilter, { recentFilter = !recentFilter }, label = { Text("Last 30 days") }) }
            item { FilterChip(largeFilter, { largeFilter = !largeFilter }, label = { Text("Large files") }) }
            item { FilterChip(longVideoFilter, { longVideoFilter = !longVideoFilter }, label = { Text("Long videos") }) }
            item {
                Box {
                    FilterChip(folderFilter != null, { folderMenu = true }, label = { Text(folderFilter ?: "Folder") })
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(text = { Text("All folders") }, onClick = { folderFilter = null; folderMenu = false })
                        media.map(GalleryMedia::bucketName).distinct().sortedBy(String::lowercase).forEach { folder ->
                            DropdownMenuItem(text = { Text(folder) }, onClick = { folderFilter = folder; folderMenu = false })
                        }
                    }
                }
            }
        }
        if (category != null) {
            val categoryMedia = category!!.second.filter {
                matchesGallerySearchFilters(it, mediaTypeFilter, favouriteFilter, recentFilter, largeFilter, longVideoFilter, folderFilter)
            }.sortedByPreference(sort)
            if (categoryMedia.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No matching pictures or videos", color = VaultSecondary) }
            else LazyVerticalGrid(columns = GridCells.Fixed(gridColumns.intValue), modifier = Modifier.weight(1f).padding(horizontal = 8.dp).pinchToResizeGrid(gridColumns, 3, 12) { preferences.edit().putInt("search_grid_columns", it).apply() }, horizontalArrangement = Arrangement.spacedBy(1.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(categoryMedia, key = { it.id }) { MediaTile(it, false, onOpen, null) }
            }
        } else if (query.isBlank()) {
            Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val edited = media.filter { it.name.contains("edited", true) || it.bucketName.contains("edited", true) }
                SearchCategory("Activity", listOf("AI-edited" to edited.filter { it.name.contains("ai", true) }, "Edited" to edited)) { category = it }
                SearchCategory("Shot types", listOf(
                    "Video" to media.filter { it.kind == MediaKind.VIDEO },
                    "Scan" to media.filter { it.name.contains("scan", true) || it.bucketName.contains("scan", true) },
                    "Selfie" to media.filter { it.name.contains("selfie", true) || it.bucketName.contains("selfie", true) },
                    "Portrait" to media.filter { it.name.contains("portrait", true) || it.bucketName.contains("portrait", true) },
                )) { category = it }
                SearchCategory("Documents", listOf(
                    "Receipts" to media.filter { it.name.contains("receipt", true) },
                    "Screenshots" to media.filter { it.bucketName.contains("screenshot", true) },
                    "Text" to media.filter { it.name.contains("document", true) || it.name.contains("scan", true) },
                )) { category = it }
            }
        } else if (results.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No results for “$query”", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(gridColumns.intValue), modifier = Modifier.weight(1f).padding(horizontal = 8.dp).pinchToResizeGrid(gridColumns, 3, 12) { preferences.edit().putInt("search_grid_columns", it).apply() }) {
                items(results, key = { it.id }) { MediaTile(it, false, onOpen, null) }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
        )
    }
    if (sortDialog) MediaSortDialog(sort, onSelect = { value -> sort = value; preferences.edit().putString("result_sort", value.name).apply(); sortDialog = false }, onDismiss = { sortDialog = false })
}

private fun matchesGallerySearchFilters(
    item: GalleryMedia,
    mediaType: String,
    favouritesOnly: Boolean,
    recentOnly: Boolean,
    largeOnly: Boolean,
    longVideosOnly: Boolean,
    folder: String?,
    nowMs: Long = System.currentTimeMillis(),
): Boolean {
    if (mediaType == "Photos" && item.kind != MediaKind.IMAGE) return false
    if (mediaType == "Videos" && item.kind != MediaKind.VIDEO) return false
    if (favouritesOnly && !item.isFavourite) return false
    if (recentOnly && item.dateTakenMs < nowMs - 30L * 24 * 60 * 60 * 1_000) return false
    if (largeOnly && item.sizeBytes < if (item.kind == MediaKind.VIDEO) 250L * 1024 * 1024 else 15L * 1024 * 1024) return false
    if (longVideosOnly && (item.kind != MediaKind.VIDEO || item.durationMs < 60_000L)) return false
    if (folder != null && !item.bucketName.equals(folder, true)) return false
    return true
}

@Composable
private fun SearchCategory(title: String, values: List<Pair<String, List<GalleryMedia>>>, onOpen: (Pair<String, List<GalleryMedia>>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        values.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { value -> Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), modifier = Modifier.weight(1f).clickable { onOpen(value) }) { Text("${value.first} ${value.second.size}", Modifier.padding(18.dp)) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchSettingsScreen(preferences: android.content.SharedPreferences, onBack: () -> Unit) {
    var recent by remember { mutableStateOf(preferences.getBoolean("recent", true)) }
    var suggestions by remember { mutableStateOf(preferences.getBoolean("suggestions", true)) }
    val categories = listOf("Search shortcuts", "People", "Locations", "Documents", "Activity", "Shot types", "My tags")
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Search settings", onBack)
        SettingsGroup {
            SettingSwitch("Show recent searches", recent) { recent = it; preferences.edit().putBoolean("recent", it).apply() }
            SettingSwitch("Show suggestions", suggestions) { suggestions = it; preferences.edit().putBoolean("suggestions", it).apply() }
        }
        Text("Search categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(18.dp))
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            categories.forEach { category ->
                var checked by remember(category) { mutableStateOf(preferences.getBoolean("category_$category", category != "My tags")) }
                SettingSwitch(category, checked) { checked = it; preferences.edit().putBoolean("category_$category", it).apply() }
            }
        }
    }
}

@Composable
private fun CleanOutScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit, onDelete: (List<GalleryMedia>) -> Unit) {
    val context = LocalContext.current
    val duplicateGroups by produceState<List<ExactDuplicateGroup>?>(initialValue = null, media) {
        value = findExactDuplicateGroups(context, media)
    }
    val qualityReport by produceState<MediaQualityReport?>(initialValue = null, media) {
        value = analyzeMediaQuality(context, media)
    }
    val duplicates = duplicateGroups.orEmpty().flatMap(ExactDuplicateGroup::items)
    val recommendedKeeperIds = duplicateGroups.orEmpty().mapTo(hashSetOf()) { it.recommendedKeeper.id }
    val suggestedDuplicateRemovalIds = duplicateGroups.orEmpty().flatMapTo(linkedSetOf()) { it.suggestedRemovalIds }
    val bestBurstIds = qualityReport?.bestBurstIds.orEmpty()
    val reclaimableBytes = duplicateGroups.orEmpty().sumOf(ExactDuplicateGroup::reclaimableBytes)
    val columns = remember { mutableIntStateOf(4) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var reviewReason by remember { mutableStateOf<QualityReviewReason?>(null) }
    val visible = reviewReason?.let { qualityReport?.mediaFor(it).orEmpty() } ?: duplicates
    Column(Modifier.fillMaxSize()) {
        if (selected.isEmpty()) GalleryToolbar("Clean out", onBack) else Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${selected.size} selected", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { selected = emptySet() }) { Text("Cancel") }
            TextButton(onClick = { onDelete(visible.filter { it.id in selected }); selected = emptySet() }) { Text("Delete") }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                if (reviewReason == null) Button(onClick = { selected = emptySet() }) { Text("Exact duplicates") }
                else OutlinedButton(onClick = { reviewReason = null; selected = emptySet() }) { Text("Exact duplicates") }
            }
            lazyRowItems(QualityReviewReason.entries) { reason ->
                val count = qualityReport?.mediaFor(reason)?.size
                val label = if (count == null) reason.label else "${reason.label} · $count"
                if (reviewReason == reason) Button(onClick = {}) { Text(label) }
                else OutlinedButton(onClick = { reviewReason = reason; selected = emptySet() }) { Text(label) }
            }
        }
        val loading = if (reviewReason == null) duplicateGroups == null else qualityReport == null
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator()
                    Text(if (reviewReason == null) "Verifying duplicate contents…" else "Analyzing media on this device…", color = VaultSecondary)
                }
            }
        } else if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CleaningServices, null, Modifier.size(56.dp), tint = VaultSecondary)
                    Text(if (reviewReason == null) "No exact duplicates" else "Nothing suggested", style = MaterialTheme.typography.titleLarge)
                    Text("Clean Out only suggests items for review. Nothing is removed unless you select it and confirm deletion.", color = VaultSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
                }
            }
        } else {
            Text(reviewReason?.label ?: "Exact duplicates", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp))
            Text(
                if (reviewReason == null) {
                    "${duplicateGroups.orEmpty().size} groups · ${GalleryLogic.fileSizeLabel(reclaimableBytes)} can be recovered. Files are verified byte for byte."
                } else {
                    "${visible.size} review suggestion${if (visible.size == 1) "" else "s"} · local analysis only · never deleted automatically"
                },
                color = VaultSecondary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
            if (reviewReason == null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("KEEP marks the safest original/favourite copy.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selected = suggestedDuplicateRemovalIds }) { Text("Select suggested copies") }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(columns.intValue), modifier = Modifier.fillMaxSize().padding(8.dp).pinchToResizeGrid(columns, 3, 12)) { items(visible, key = { it.id }) { item ->
                Box {
                    MediaTile(
                        item,
                        item.id in selected,
                        onOpen = { if (selected.isEmpty()) onOpen(it) else selected = selected.toMutableSet().apply { if (!add(it.id)) remove(it.id) } },
                        onSelect = { id -> selected = selected.toMutableSet().apply { if (!add(id)) remove(id) } },
                    )
                    if (reviewReason == null && item.id in recommendedKeeperIds) Text(
                        "KEEP",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color(0xCC197A45), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                    if (reviewReason == QualityReviewReason.BURST && item.id in bestBurstIds) Text(
                        "BEST",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color(0xCC197A45), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            } }
        }
    }
}

@Composable
private fun LocationsScreen(media: List<GalleryMedia>, onBack: () -> Unit, onOpen: (GalleryMedia) -> Unit) {
    GalleryLocationsMapScreen(media, onBack, onOpen)
}

@Composable
private fun SharedAlbumsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(250)) { uris ->
        if (uris.isNotEmpty()) (context as? Activity)?.let { shareUris(it, uris, "*/*") }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Shared albums", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(38.dp)) {
                Icon(Icons.Outlined.FolderShared, null, Modifier.size(62.dp), tint = VaultSecondary)
                Text("Stay connected", style = MaterialTheme.typography.headlineMedium)
                Text("Create albums everyone can add to. On Pixel, sharing uses Android's system share sheet so you stay in control of the provider and recipients.", color = VaultSecondary, textAlign = TextAlign.Center)
                Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Text("Choose media to share") }
            }
        }
    }
}

@Composable
private fun PublicMenuSheet(
    onVideos: () -> Unit,
    onRecent: () -> Unit,
    onFavourites: () -> Unit,
    onCleanOut: () -> Unit,
    onLocations: () -> Unit,
    onSharedAlbums: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onSecure: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MenuAction(Icons.Outlined.VideoLibrary, "Videos", onVideos)
            MenuAction(Icons.Outlined.Favorite, "Favourites", onFavourites)
            MenuAction(Icons.Outlined.Today, "Recent", onRecent)
            MenuAction(Icons.Outlined.CleaningServices, "Clean out", onCleanOut)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MenuAction(Icons.Outlined.LocationOn, "Locations", onLocations)
            MenuAction(Icons.Outlined.FolderShared, "Shared\nalbums", onSharedAlbums)
            MenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
            MenuAction(Icons.Outlined.Settings, "Settings", onSettings)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A4A4F)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable(role = Role.Button, onClick = onSecure),
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x2238D3AA)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, "Secure Gallery", Modifier.size(25.dp), tint = VaultSecure)
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Secure Gallery", style = MaterialTheme.typography.titleMedium)
                    Text("Locked photos, videos and albums", color = VaultSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Text("›", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MenuScreen(
    onVideos: () -> Unit,
    onRecent: () -> Unit,
    onFavourites: () -> Unit,
    onCleanOut: () -> Unit,
    onLocations: () -> Unit,
    onSharedAlbums: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onSecure: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar()
        Spacer(Modifier.weight(1f))
        Card(
            colors = CardDefaults.cardColors(containerColor = VaultRaised), shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Outlined.VideoLibrary, "Videos", onVideos)
                    MenuAction(Icons.Outlined.Favorite, "Favourites", onFavourites)
                    MenuAction(Icons.Outlined.Today, "Recent", onRecent)
                    MenuAction(Icons.Outlined.CleaningServices, "Clean out", onCleanOut)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Outlined.LocationOn, "Locations", onLocations)
                    MenuAction(Icons.Outlined.FolderShared, "Shared\nalbums", onSharedAlbums)
                    MenuAction(Icons.Outlined.Delete, "Recycle bin", onTrash)
                    MenuAction(Icons.Outlined.Settings, "Settings", onSettings)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A4A4F)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onSecure),
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, "Secure Gallery", Modifier.size(30.dp), tint = VaultSecure)
                        Text("Open Secure Gallery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp).weight(1f))
                        Text("›", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(82.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { Icon(icon, label, Modifier.size(30.dp)); Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun PublicBottomNavigation(selected: PublicTab, onSelected: (PublicTab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).background(VaultBackground), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        NavItem(Icons.Outlined.Image, Icons.Filled.FilledImage, "Pictures", selected == PublicTab.PICTURES) { onSelected(PublicTab.PICTURES) }
        NavItem(Icons.Outlined.Collections, Icons.Filled.FilledCollections, "Albums", selected == PublicTab.ALBUMS) { onSelected(PublicTab.ALBUMS) }
        NavItem(Icons.Outlined.Movie, Icons.Filled.FilledMovie, "Stories", selected == PublicTab.STORIES) { onSelected(PublicTab.STORIES) }
        NavItem(Icons.Outlined.Menu, Icons.Filled.FilledMenu, "Menu", selected == PublicTab.MENU) { onSelected(PublicTab.MENU) }
    }
}

@Composable
private fun PickerBottomNavigation(tab: PublicTab, onTab: (PublicTab) -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(82.dp).background(VaultBackground),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavItem(Icons.Outlined.Image, Icons.Filled.FilledImage, "Pictures", tab == PublicTab.PICTURES) { onTab(PublicTab.PICTURES) }
        NavItem(Icons.Outlined.Collections, Icons.Filled.FilledCollections, "Albums", tab == PublicTab.ALBUMS) { onTab(PublicTab.ALBUMS) }
        TextButton(onClick = onCancel, modifier = Modifier.height(56.dp)) { Text("Cancel") }
    }
}

@Composable
private fun PickerSelectionBar(count: Int, onCancel: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(82.dp).background(VaultBackground).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count selected", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) { Text("Clear") }
        Button(enabled = count > 0, onClick = onDone) { Text("Done") }
    }
}

@Composable
internal fun SelectionHeader(selectedCount: Int, totalCount: Int, onSelectAll: () -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(92.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onSelectAll) { Text(if (selectedCount == totalCount) "All ✓" else "All") }
        Text("$selectedCount selected", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun AlbumSelectionBar(
    selectedAlbums: List<GalleryAlbum>,
    selectedMedia: List<GalleryMedia>,
    availableAlbums: List<GalleryAlbum>,
    onShare: () -> Unit,
    onSecure: (Boolean) -> Unit,
    essentialAlbumIds: Set<Long>,
    onEssentialChanged: (Set<Long>) -> Unit,
    onCoverChanged: (GalleryAlbum, GalleryMedia) -> Unit,
    onGroupCreated: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var more by remember { mutableStateOf(false) }
    var transferMove by remember { mutableStateOf<Boolean?>(null) }
    var coverAlbum by remember { mutableStateOf<GalleryAlbum?>(null) }
    var groupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    val virtualSelection = selectedAlbums.any { it.mediaIds != null }
    val selectedIds = selectedAlbums.mapTo(LinkedHashSet()) { it.bucketId }
    val allEssential = selectedIds.isNotEmpty() && selectedIds.all { it in essentialAlbumIds }
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        if (virtualSelection) BottomAction(Icons.Outlined.ContentCopy, "Copy") { transferMove = false }
        else BottomAction(Icons.Outlined.Add, "Group") { groupDialog = true }
        BottomAction(Icons.Outlined.Collections, "Move") { transferMove = true }
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        Box {
            BottomAction(Icons.Outlined.MoreVert, "More") { more = true }
            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                if (!virtualSelection) DropdownMenuItem(text = { Text(if (allEssential) "Remove from Essential albums" else "Add to Essential albums") }, onClick = {
                    more = false
                    onEssentialChanged(essentialAlbumIds.toMutableSet().apply { if (allEssential) removeAll(selectedIds) else addAll(selectedIds) })
                    Toast.makeText(context, if (allEssential) "Removed from Essential albums" else "Added to Essential albums", Toast.LENGTH_SHORT).show()
                })
                if (selectedAlbums.size == 1) DropdownMenuItem(text = { Text("Change album cover") }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = {
                    more = false; coverAlbum = selectedAlbums.first()
                })
                DropdownMenuItem(text = { Text("Copy selected albums to album") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { more = false; transferMove = false })
                DropdownMenuItem(text = { Text("Copy selected albums to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { more = false; onSecure(false) })
                DropdownMenuItem(text = { Text("Move selected albums to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { more = false; onSecure(true) })
                DropdownMenuItem(text = { Text("Move selected albums to Recycle bin") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { more = false; onDelete() })
                DropdownMenuItem(text = { Text("Finish selection") }, onClick = { more = false; onDone() })
            }
        }
    }
    transferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = availableAlbums,
            sourceAlbumNames = selectedAlbums.mapTo(LinkedHashSet()) { it.name },
            sourceKind = TransferSourceKind.ALBUMS,
            onDismiss = { transferMove = null },
            onSelect = { target ->
                transferMove = null
                (context as? MainGalleryActivity)?.transferMediaToAlbum(selectedMedia, target, move)
                onDone()
            },
        )
    }
    coverAlbum?.let { album ->
        val choices = selectedMedia.filter { item -> album.mediaIds?.contains(item.id) ?: (item.bucketId in album.bucketIds) }
        AlertDialog(
            onDismissRequest = { coverAlbum = null },
            title = { Text("Choose cover for ${album.name}") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = currentWindowHeightDp() * .55f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(choices, key = { it.id }) { item ->
                        val request = ImageRequest.Builder(context).data(item.uri).apply { if (item.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }.build()
                        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { onCoverChanged(album, item); coverAlbum = null }) { GalleryThumbnail(item, request) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { coverAlbum = null }) { Text("Cancel") } },
        )
    }
    if (groupDialog && !virtualSelection) AlertDialog(
        onDismissRequest = { groupDialog = false },
        title = { Text("Create album group") },
        text = { OutlinedTextField(groupName, { groupName = it.take(60) }, label = { Text("Group name") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { groupDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = groupName.isNotBlank(), onClick = {
            val name = groupName.trim()
            val prefs = context.getSharedPreferences("gallery-settings", 0)
            val names = prefs.getStringSet("album_group_names", emptySet()).orEmpty().toMutableSet().apply { add(name) }
            val ids = selectedAlbums.flatMapTo(LinkedHashSet()) { it.bucketIds }.mapTo(LinkedHashSet()) { it.toString() }
            prefs.edit().putStringSet("album_group_names", names).putStringSet("album_group_$name", ids).apply()
            groupDialog = false; groupName = ""; onGroupCreated(); onDone()
            Toast.makeText(context, "Album group created", Toast.LENGTH_SHORT).show()
        }) { Text("Create") } },
    )
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) VaultPrimary else VaultSecondary
    Column(
        Modifier.width(86.dp).clickable(role = Role.Tab, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(if (selected) selectedIcon else icon, label, Modifier.size(29.dp), tint = color)
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SelectionBar(
    selectedMedia: List<GalleryMedia>,
    availableAlbums: List<GalleryAlbum>,
    onShare: () -> Unit,
    onSecure: (Boolean) -> Unit,
    onCreate: (CreationType) -> Unit,
    favouriteLabel: String,
    onFavourite: () -> Unit,
    onEditDateTime: (Long) -> Unit,
    onEditLocation: (Double, Double) -> Unit,
    onDelete: () -> Unit,
    onTransferStarted: () -> Unit,
) {
    val context = LocalContext.current
    var createMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var tag by remember { mutableStateOf("") }
    var transferMove by remember { mutableStateOf<Boolean?>(null) }
    var dateDialog by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())) }
    var locationDialog by remember { mutableStateOf(false) }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var presetDialog by remember { mutableStateOf(false) }
    val presets by produceState<List<PhotoPreset>>(emptyList(), selectedMedia.size) {
        value = PhotoPresetStore(context).list()
    }
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        BottomAction(Icons.Outlined.Add, "Create", { createMenu = true })
        BottomAction(Icons.Outlined.Share, "Share", onShare)
        BottomAction(Icons.Outlined.Delete, "Delete", onDelete)
        Box {
            BottomAction(Icons.Outlined.MoreVert, "More", { moreMenu = true })
            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { moreMenu = false; copyUrisToClipboard(context, selectedMedia) })
                DropdownMenuItem(text = { Text("Copy to album") }, onClick = { moreMenu = false; transferMove = false })
                DropdownMenuItem(text = { Text("Move to album") }, onClick = { moreMenu = false; transferMove = true })
                DropdownMenuItem(text = { Text("Add to shared album") }, onClick = { moreMenu = false; (context as? Activity)?.let { shareUris(it, selectedMedia.map { item -> item.uri }, "*/*") } })
                DropdownMenuItem(text = { Text(if (favouriteLabel == "Unfavourite") "Remove from favourites" else "Add to favourites") }, leadingIcon = { Icon(Icons.Outlined.Favorite, null) }, onClick = { moreMenu = false; onFavourite() })
                DropdownMenuItem(text = { Text("Add tag") }, onClick = { moreMenu = false; tagDialog = true })
                DropdownMenuItem(text = { Text("Edit date and time") }, onClick = { moreMenu = false; dateDialog = true })
                DropdownMenuItem(text = { Text("Edit location") }, onClick = { moreMenu = false; locationDialog = true })
                if (selectedMedia.any { it.kind == MediaKind.IMAGE } && presets.isNotEmpty()) {
                    DropdownMenuItem(text = { Text("Apply photo preset to copies") }, leadingIcon = { Icon(Icons.Outlined.Tune, null) }, onClick = { moreMenu = false; presetDialog = true })
                }
                if (selectedMedia.size == 1 && selectedMedia.first().kind == MediaKind.IMAGE) DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                    moreMenu = false
                    setAsWallpaper(context, selectedMedia.first())
                })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Copy to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(false) })
                DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(true) })
            }
        }
    }
    if (createMenu) CreateMediaSheet(onDismiss = { createMenu = false }) { type ->
        createMenu = false
        onCreate(type)
    }
    if (tagDialog) AlertDialog(
        onDismissRequest = { tagDialog = false },
        title = { Text("Add tag") },
        text = { OutlinedTextField(tag, { tag = it.take(40) }, label = { Text("Tag") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { tagDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = tag.isNotBlank(), onClick = {
            GallerySearchIndex(context).use { index ->
                selectedMedia.forEach { item ->
                    index.setTags(item.id, tag.trim())
                }
            }
            Toast.makeText(context, "Tagged ${selectedMedia.size} items", Toast.LENGTH_SHORT).show()
            tag = ""; tagDialog = false
        }) { Text("Add") } },
    )
    if (presetDialog) AlertDialog(
        onDismissRequest = { presetDialog = false },
        title = { Text("Batch photo preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A new edited copy will be created for each selected photo. Originals stay unchanged. Work continues in the background.", color = VaultSecondary)
                presets.forEach { preset ->
                    Button(onClick = {
                        BatchPresetCoordinator.enqueue(context, selectedMedia.filter { it.kind == MediaKind.IMAGE }, preset)
                        presetDialog = false
                        onTransferStarted()
                        Toast.makeText(context, "Applying ${preset.name} in the background", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(preset.name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { presetDialog = false }) { Text("Cancel") } },
    )
    transferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = availableAlbums,
            sourceAlbumNames = selectedMedia.mapTo(LinkedHashSet()) { it.bucketName },
            onDismiss = { transferMove = null },
            onSelect = { target ->
                transferMove = null
                (context as? MainGalleryActivity)?.transferMediaToAlbum(selectedMedia, target, move)
                onTransferStarted()
            },
        )
    }
    if (dateDialog) AlertDialog(
        onDismissRequest = { dateDialog = false },
        title = { Text("Edit date and time") },
        text = { OutlinedTextField(dateText, { dateText = it.take(16) }, label = { Text("yyyy-MM-dd HH:mm") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { dateDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply { isLenient = false }.parse(dateText)?.time }.getOrNull()
            if (parsed == null) Toast.makeText(context, "Enter a valid date and time", Toast.LENGTH_SHORT).show()
            else { dateDialog = false; onEditDateTime(parsed) }
        }) { Text("Save") } },
    )
    if (locationDialog) AlertDialog(
        onDismissRequest = { locationDialog = false },
        title = { Text("Edit location") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Location is written to the image's standard GPS metadata. Videos are left unchanged.", color = VaultSecondary)
            OutlinedTextField(latitudeText, { latitudeText = it.take(12) }, label = { Text("Latitude (-90 to 90)") }, singleLine = true)
            OutlinedTextField(longitudeText, { longitudeText = it.take(13) }, label = { Text("Longitude (-180 to 180)") }, singleLine = true)
        } },
        dismissButton = { TextButton(onClick = { locationDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            val latitude = latitudeText.toDoubleOrNull(); val longitude = longitudeText.toDoubleOrNull()
            if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) Toast.makeText(context, "Enter valid coordinates", Toast.LENGTH_SHORT).show()
            else { locationDialog = false; onEditLocation(latitude, longitude) }
        }) { Text("Save") } },
    )
}

@Composable
internal fun PublicAlbumDestinationPicker(
    move: Boolean,
    albums: List<GalleryAlbum>,
    sourceAlbumNames: Set<String>,
    sourceKind: TransferSourceKind = TransferSourceKind.MEDIA,
    sourceFolderCount: Int = sourceAlbumNames.size,
    allowOriginalFolders: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var createDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var pendingMerge by remember { mutableStateOf<String?>(null) }
    val destinations = remember(albums, query, move, sourceAlbumNames, sourceKind) {
        albums.distinctBy { it.name.lowercase(Locale.ROOT) }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .filterNot { shouldExcludeTransferDestination(it.name, sourceAlbumNames, move, sourceKind) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = VaultRaised, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (sourceKind == TransferSourceKind.ALBUMS) "${if (move) "Move" else "Copy"} album folder${if (sourceFolderCount == 1) "" else "s"}"
                        else if (move) "Move to album" else "Copy to album",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        if (sourceKind == TransferSourceKind.ALBUMS) "Choose a destination folder. Existing contents will be merged safely."
                        else "Choose exactly where ${if (move) "the originals" else "the copies"} should go",
                        color = VaultSecondary,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search albums") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = maxOf(280.dp, currentWindowHeightDp() * .58f)),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                if (allowOriginalFolders) item(key = "keep-original-folders") {
                    Column(
                        Modifier.fillMaxWidth().clickable { onSelect("") },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Folder, "Keep original folders", Modifier.size(54.dp), tint = VaultBlue) }
                        Text("Original folders", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text("Create or merge", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item(key = "create-album") {
                    Column(
                        Modifier.fillMaxWidth().clickable { createDialog = true },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Add, "Create album", Modifier.size(46.dp), tint = VaultPrimary) }
                        Text(if (sourceKind == TransferSourceKind.ALBUMS) "New destination" else "Create album", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text("New folder", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(destinations, key = { "destination-${it.bucketId}" }) { album ->
                    Column(Modifier.fillMaxWidth().clickable {
                        if (sourceKind == TransferSourceKind.ALBUMS) pendingMerge = album.name else onSelect(album.name)
                    }) {
                        if (sourceKind == TransferSourceKind.ALBUMS) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(VaultSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Folder, album.name, Modifier.size(64.dp), tint = VaultPrimary)
                                Text("${album.count}", modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape).background(VaultRaised).padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(album.cover.uri).apply {
                                    if (album.cover.kind == MediaKind.VIDEO) videoFrameMillis(1_000)
                                }.build(),
                                contentDescription = album.name,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text(if (sourceKind == TransferSourceKind.ALBUMS) "Destination folder" else "${album.count} item${if (album.count == 1) "" else "s"}", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (createDialog) AlertDialog(
        onDismissRequest = { createDialog = false },
        title = { Text(if (sourceKind == TransferSourceKind.ALBUMS) "Create destination folder" else "Create new album") },
        text = {
            OutlinedTextField(
                value = newAlbumName,
                onValueChange = { newAlbumName = it.take(60) },
                label = { Text("Album name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = newAlbumName.isNotBlank(), onClick = {
                createDialog = false
                onSelect(sanitizeAlbumName(newAlbumName))
            }) { Text("Create") }
        },
    )
    pendingMerge?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingMerge = null },
            title = { Text("${if (move) "Move" else "Copy"} into $target?") },
            text = {
                Text(
                    "$sourceFolderCount selected album folder${if (sourceFolderCount == 1) "" else "s"} will be ${if (move) "moved" else "copied"} into “$target”. " +
                        if (move) "The source folder${if (sourceFolderCount == 1) "" else "s"} will disappear after every item is moved successfully."
                        else "The source folders will remain unchanged.",
                )
            },
            dismissButton = { TextButton(onClick = { pendingMerge = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = { pendingMerge = null; onSelect(target) }) { Text(if (move) "Move and merge" else "Copy and merge") } },
        )
    }
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) = BottomAction(icon, label, VaultPrimary, onClick)

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.width(78.dp).clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, label, Modifier.size(28.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GallerySettings(onBack: () -> Unit, onMessagingChanged: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("gallery-settings", 0) }
    var fullScreen by remember { mutableStateOf(preferences.getBoolean("full_screen_scroll", false)) }
    var externalPlayer by remember { mutableStateOf(preferences.getBoolean("external_player", false)) }
    var followAndroidAudioFocus by remember {
        mutableStateOf(galleryAudioFocusBehavior(context) == GalleryAudioFocusBehavior.FOLLOW_ANDROID)
    }
    var essential by remember { mutableStateOf(preferences.getBoolean("essential", true)) }
    var stories by remember { mutableStateOf(preferences.getBoolean("auto_stories", true)) }
    var mergeAlbums by remember { mutableStateOf(preferences.getBoolean("merge_albums", true)) }
    var conflictPolicy by remember { mutableStateOf(transferConflictPolicy(context)) }
    var conflictPolicyMenu by remember { mutableStateOf(false) }
    var secureLauncher by remember { mutableStateOf(isSecureLauncherVisible(context)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var diagnosticsOpen by remember { mutableStateOf(false) }
    var transferHistoryOpen by remember { mutableStateOf(false) }
    var defaultSetupMime by remember { mutableStateOf<String?>(null) }
    var messagingSetup by remember { mutableStateOf<MessagingSource?>(null) }
    var messagingStatusVersion by remember { mutableIntStateOf(0) }
    var messagingModes by remember {
        mutableStateOf(MessagingSource.entries.associateWith(context::messagingOrganizationMode))
    }
    val messagingRepository = remember { MessagingAlbumRepository(context) }
    val messagingStatus = remember { context.getSharedPreferences("messaging-index-status", 0) }
    if (diagnosticsOpen) {
        DeviceDiagnosticsScreen(onBack = { diagnosticsOpen = false })
        return
    }
    if (transferHistoryOpen) {
        TransferHistoryScreen(onBack = { transferHistoryOpen = false })
        return
    }
    LaunchedEffect(Unit) {
        var previous = ""
        while (true) {
            val current = MessagingSource.entries.joinToString { source ->
                "${messagingStatus.getString("${source.storageId}.status", "idle")}:${messagingStatus.getLong("${source.storageId}.updated", 0L)}"
            }
            if (current != previous) {
                previous = current
                messagingStatusVersion++
                onMessagingChanged()
            }
            delay(1_500)
        }
    }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Gallery settings", onBack)
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsHeader("System integration")
                SettingsGroup {
                    val photoDefault = resolvedDefaultViewer(context, "image/jpeg")
                    val videoDefault = resolvedDefaultViewer(context, "video/mp4")
                    SettingLink("Open photos — ${photoDefault?.label ?: "Ask every time"}") {
                        if (photoDefault != null && photoDefault.packageName != context.packageName) defaultSetupMime = "image/jpeg"
                        else (context as? MainGalleryActivity)?.requestDefaultViewer("image/jpeg")
                    }
                    SettingLink("Open videos — ${videoDefault?.label ?: "Ask every time"}") {
                        if (videoDefault != null && videoDefault.packageName != context.packageName) defaultSetupMime = "video/mp4"
                        else (context as? MainGalleryActivity)?.requestDefaultViewer("video/mp4")
                    }
                    SettingLink("System text and accessibility — On") {
                        dialog = "Vault Gallery inherits the device font, font size, bold-text adjustment, display scaling, language, right-to-left layout direction and animation scale. Android has no phone-wide photo sorting preference, so Gallery remembers sorting separately for Pictures, albums, searches and each folder."
                    }
                }
                SettingsHeader("Viewing")
                SettingsGroup {
                    SettingSwitch("Full screen scrolling", fullScreen) {
                        fullScreen = it
                        preferences.edit().putBoolean("full_screen_scroll", it).apply()
                        (context as? MainGalleryActivity)?.applyFullScreenPreference(it)
                    }
                    SettingSwitch("Open in Video player", externalPlayer) { externalPlayer = it; preferences.edit().putBoolean("external_player", it).apply() }
                    SettingSwitch(
                        "Pause for calls and other audio",
                        "When off, Gallery keeps playing through transient audio-focus changes. Android or the device may still control the final audio route.",
                        followAndroidAudioFocus,
                    ) {
                        followAndroidAudioFocus = it
                        preferences.edit().putString(
                            "audio_focus_behavior",
                            if (it) GalleryAudioFocusBehavior.FOLLOW_ANDROID.name else GalleryAudioFocusBehavior.CONTINUE_PLAYBACK.name,
                        ).apply()
                    }
                }
                SettingsHeader("Stories")
                SettingsGroup {
                    SettingSwitch("Auto create stories", stories) { stories = it; preferences.edit().putBoolean("auto_stories", it).apply() }
                }
                SettingsHeader("Albums")
                SettingsGroup {
                    SettingSwitch("Select essential albums", "Show only the albums you select on the Albums tab instead of showing them all.", essential) { essential = it; preferences.edit().putBoolean("essential", it).apply() }
                    SettingSwitch("Merge albums", "Albums with the same name will be shown as a single album.", mergeAlbums) { mergeAlbums = it; preferences.edit().putBoolean("merge_albums", it).apply() }
                }
                SettingsHeader("Messaging albums")
                SettingsGroup {
                    MessagingSetupRow(
                        source = MessagingSource.WHATSAPP,
                        mode = messagingModes[MessagingSource.WHATSAPP] ?: MessagingOrganizationMode.CONVERSATIONS,
                        indexed = messagingRepository.indexFile(MessagingSource.WHATSAPP).isFile,
                        status = messagingStatus.getString("${MessagingSource.WHATSAPP.storageId}.status", "idle").orEmpty(),
                        error = messagingStatus.getString("${MessagingSource.WHATSAPP.storageId}.error", null),
                        version = messagingStatusVersion,
                        onClick = { messagingSetup = MessagingSource.WHATSAPP },
                    )
                    MessagingSetupRow(
                        source = MessagingSource.WHATSAPP_BUSINESS,
                        mode = messagingModes[MessagingSource.WHATSAPP_BUSINESS] ?: MessagingOrganizationMode.CONVERSATIONS,
                        indexed = messagingRepository.indexFile(MessagingSource.WHATSAPP_BUSINESS).isFile,
                        status = messagingStatus.getString("${MessagingSource.WHATSAPP_BUSINESS.storageId}.status", "idle").orEmpty(),
                        error = messagingStatus.getString("${MessagingSource.WHATSAPP_BUSINESS.storageId}.error", null),
                        version = messagingStatusVersion,
                        onClick = { messagingSetup = MessagingSource.WHATSAPP_BUSINESS },
                    )
                }
                SettingsHeader("Transfers")
                SettingsGroup {
                    Box {
                        SettingLink("Matching file names — ${conflictPolicy.label}") { conflictPolicyMenu = true }
                        DropdownMenu(expanded = conflictPolicyMenu, onDismissRequest = { conflictPolicyMenu = false }) {
                            TransferConflictPolicy.entries.forEach { policy ->
                                DropdownMenuItem(
                                    text = { Text(policy.label) },
                                    onClick = {
                                        conflictPolicy = policy
                                        preferences.edit().putString("transfer_conflict_policy", policy.name).apply()
                                        conflictPolicyMenu = false
                                    },
                                )
                            }
                        }
                    }
                    SettingLink("Transfer history") { transferHistoryOpen = true }
                }
                SettingsHeader("Privacy")
                SettingsGroup {
                    SettingLink("Privacy Policy") { dialog = "Vault Gallery processes media locally and does not upload it by default. Secure Gallery exports plaintext only after your explicit action." }
                    SettingLink("Permissions") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                    }
                }
                SettingsHeader("Secure Gallery")
                SettingsGroup {
                    SettingSwitch("Show Secure Gallery icon", "Keep a separate launcher entry for direct authenticated vault access.", secureLauncher) {
                        secureLauncher = it; setSecureLauncherVisible(context, it)
                    }
                    SettingLink("Secure Gallery settings") { (context as? Activity)?.let(::launchSecureGallery) }
                }
                SettingsGroup {
                    SettingLink("Device and media diagnostics") { diagnosticsOpen = true }
                    SettingLink("Open-source licenses") { dialog = "Open-source engines\n\nPhotoEditor — MIT\nMedia3 — Apache 2.0\nCoil — Apache 2.0\nZoomImage — Apache 2.0\nTink — Apache 2.0\nSQLCipher — BSD-style\nOpenCV — Apache 2.0\nargon2kt — Apache 2.0\n\nFull notices and source links are included inside the app package." }
                    SettingLink("About Gallery") { dialog = "Vault Gallery ${BuildConfig.VERSION_NAME}\nA local-first Pixel gallery with slide selection, system trash, favourites, search, stories, photo/video editing, and a biometric Secure Gallery with optional encryption." }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }
    dialog?.let { message -> AlertDialog(onDismissRequest = { dialog = null }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } }, text = { Text(message) }) }
    defaultSetupMime?.let { mimeType ->
        val current = resolvedDefaultViewer(context, mimeType)
        AlertDialog(
            onDismissRequest = { defaultSetupMime = null },
            title = { Text("Make Vault Gallery the default for ${mediaTypeLabel(mimeType)}") },
            text = {
                Text(
                    "Android currently sends ${mediaTypeLabel(mimeType)} to ${current?.label ?: "another app"}. " +
                        "Open that app's system settings, clear its default preferences, return here, then tap the row again and choose Vault Gallery with Always. Android requires this confirmation and does not let any gallery change it silently.",
                )
            },
            dismissButton = { TextButton(onClick = { defaultSetupMime = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    current?.packageName?.let { packageName ->
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
                    }
                    defaultSetupMime = null
                }) { Text("Open app settings") }
            },
        )
    }
    messagingSetup?.let { source ->
        var keyText by remember(source) { mutableStateOf("") }
        val hasStoredKey = remember(source, messagingStatusVersion) { MessagingBackupKeyStore.load(context, source) != null }
        val validNewKey = keyText.filterNot(Char::isWhitespace).matches(Regex("[0-9a-fA-F]{64}"))
        AlertDialog(
            onDismissRequest = { messagingSetup = null },
            title = { Text("${source.title} albums") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Album organization", style = MaterialTheme.typography.titleMedium)
                    listOf(
                        "By conversation" to MessagingOrganizationMode.CONVERSATIONS,
                        "Original WhatsApp folders" to MessagingOrganizationMode.ORIGINAL_FOLDERS,
                    ).forEach { (label, mode) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                context.setMessagingOrganizationMode(source, mode)
                                messagingModes = messagingModes + (source to mode)
                                onMessagingChanged()
                            }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, Modifier.weight(1f))
                            if (messagingModes[source] == mode) Icon(Icons.Outlined.Check, "Selected", tint = VaultBlue)
                        }
                    }
                    Text("Vault Gallery decrypts the latest local Crypt15 backup on this phone, creates a private conversation index, and immediately deletes the temporary database. Original media is never moved.")
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it.filterNot(Char::isWhitespace).take(64) },
                        label = { Text(if (hasStoredKey) "New 64-character key (optional)" else "64-character backup key") },
                        supportingText = { Text(if (hasStoredKey && keyText.isBlank()) "A protected key is already stored" else "Processed only on this device") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (messagingRepository.indexFile(source).isFile || hasStoredKey) {
                        TextButton(onClick = {
                            MessagingBackupKeyStore.forget(context, source)
                            messagingRepository.indexFile(source).delete()
                            context.getSharedPreferences("messaging-index-status", 0).edit()
                                .remove("${source.storageId}.status")
                                .remove("${source.storageId}.error")
                                .apply()
                            messagingSetup = null
                            onMessagingChanged()
                        }) { Text("Remove organization and forget key", color = VaultSecure) }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { messagingSetup = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(enabled = hasStoredKey || validNewKey, onClick = {
                    if (validNewKey) MessagingBackupKeyStore.save(context, source, keyText)
                    context.getSharedPreferences("messaging-index-status", 0).edit()
                        .putString("${source.storageId}.status", "running")
                        .remove("${source.storageId}.error")
                        .apply()
                    MessagingIndexCoordinator.enqueue(context, source)
                    messagingSetup = null
                    messagingStatusVersion++
                    Toast.makeText(context, "${source.title} indexing started in the background", Toast.LENGTH_LONG).show()
                }) { Text("Index latest backup") }
            },
        )
    }
}

@Composable
private fun MessagingSetupRow(
    source: MessagingSource,
    mode: MessagingOrganizationMode,
    indexed: Boolean,
    status: String,
    error: String?,
    version: Int,
    onClick: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val refreshKey = version
    val summary = if (mode == MessagingOrganizationMode.ORIGINAL_FOLDERS) {
        "Using original WhatsApp media folders"
    } else when (status) {
        "running" -> "Indexing in the background…"
        "ready" -> "Ready — refresh whenever WhatsApp creates a newer backup"
        "error" -> error ?: "Indexing failed"
        else -> if (indexed) "Conversation albums are ready" else "Not set up"
    }
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(source.title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, color = if (status == "error") VaultSecure else VaultSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.graphicsLayer { rotationZ = 180f }, tint = VaultSecondary)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable private fun SettingsHeader(text: String) { Text(text, color = VaultSecondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 18.dp, top = 12.dp)) }

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = if (checked) "On" else "Off"
            },
        )
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, color = VaultSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = if (checked) "On" else "Off"
            },
        )
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(color = Color(0xFF38383D), modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun PublicViewer(
    media: GalleryMedia,
    items: List<GalleryMedia>,
    onBack: () -> Unit,
    onNavigate: (GalleryMedia) -> Unit,
    onShare: () -> Unit,
    onSecure: (Boolean) -> Unit,
    onFavourite: () -> Unit,
    onEdit: () -> Unit,
    onAiAssist: () -> Unit,
    onScanDocument: () -> Unit,
    onFindSimilar: () -> Unit,
    onCreateGif: () -> Unit,
    onMediaCreated: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deletePrompt by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var transferMove by remember { mutableStateOf<Boolean?>(null) }
    val destinationAlbums = remember(items) { MediaStoreRepository(context).albums(items, mergeMatchingNames = true) }
    var dragY by remember(media.id) { mutableFloatStateOf(0f) }
    var infoGestureEnabled by remember(media.id) { mutableStateOf(true) }
    var viewerHeight by remember { mutableFloatStateOf(1f) }
    // Chrome visibility and sound belong to the viewer session, not an individual page.
    var chromeVisible by remember { mutableStateOf(true) }
    var videoMuted by remember { mutableStateOf(true) }
    var stripTargetId by remember { mutableStateOf<Long?>(null) }
    var stripTargetPositionMs by remember { mutableLongStateOf(0L) }
    var stripTargetPlaying by remember { mutableStateOf(false) }
    var lastStripSeekDispatchMs by remember(media.id) { mutableLongStateOf(0L) }
    var stripScrubInProgress by remember(media.id) { mutableStateOf(false) }
    var resumeAfterStripScrub by remember(media.id) { mutableStateOf(false) }
    var recognizedDocument by remember(media.id) { mutableStateOf<RecognizedDocument?>(null) }
    var showRecognizedText by remember(media.id) { mutableStateOf(false) }
    var motionPhotoPlaying by remember(media.id) { mutableStateOf(false) }
    val motionPhoto by produceState<ExtractedMotionPhoto?>(initialValue = null, media.id, media.uri, media.kind) {
        value = if (media.kind == MediaKind.IMAGE) withContext(Dispatchers.IO) {
            runCatching { extractMotionPhoto(context, media.uri, "public-${media.id}-${media.dateTakenMs}") }.getOrNull()
        } else null
    }
    val inlinePlayer = remember(context) { buildSamsungGalleryPlayer(context) }
    val index = items.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = index, pageCount = { items.size })
    val latestOnNavigate by rememberUpdatedState(onNavigate)
    val latestMediaId by rememberUpdatedState(media.id)
    DisposableEffect(inlinePlayer) { onDispose { inlinePlayer.release() } }
    DisposableEffect(motionPhoto?.file) {
        val file = motionPhoto?.file
        onDispose { file?.delete() }
    }
    LaunchedEffect(media.id, media.uri, media.kind) {
        delay(300)
        recognizedDocument = if (media.kind == MediaKind.IMAGE) {
            runCatching { recognizeDocumentText(context, media.uri, "public:${media.id}:${media.dateTakenMs}") }
                .getOrNull()?.takeIf(RecognizedDocument::hasReliableText)
        } else null
    }
    LaunchedEffect(chromeVisible) {
        (context as? MainGalleryActivity)?.applyFullScreenPreference(!chromeVisible)
    }
    DisposableEffect(Unit) {
        onDispose { (context as? MainGalleryActivity)?.applyFullScreenPreference() }
    }
    LaunchedEffect(media.id, media.uri, motionPhoto?.file, motionPhotoPlaying) {
        val playable = when {
            media.kind == MediaKind.VIDEO -> media.uri
            motionPhotoPlaying -> motionPhoto?.file?.let(Uri::fromFile)
            else -> null
        }
        if (playable != null) {
            inlinePlayer.stop()
            inlinePlayer.clearMediaItems()
            inlinePlayer.setMediaItem(MediaItem.fromUri(playable))
            val stripControlled = media.kind == MediaKind.VIDEO && stripTargetId == media.id
            if (stripControlled) inlinePlayer.seekTo(stripTargetPositionMs)
            inlinePlayer.prepare()
            inlinePlayer.playWhenReady = if (stripControlled) stripTargetPlaying else true
        } else {
            inlinePlayer.pause()
            inlinePlayer.clearMediaItems()
        }
    }
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            items.getOrNull(page)?.takeIf { it.id != latestMediaId }?.let(latestOnNavigate)
        }
    }
    LaunchedEffect(media.id, items) {
        val target = items.indexOfFirst { it.id == media.id }
        if (target >= 0 && !pagerState.isScrollInProgress && pagerState.currentPage != target) {
            // A filmstrip drag may cross dozens of items in a second. Animating every crossed
            // page makes the large viewport lag behind and creates the long "travel" users
            // reported. The pager itself still follows a direct viewport swipe continuously;
            // filmstrip-driven navigation snaps the large preview to the centred item.
            pagerState.scrollToPage(target)
        }
    }
    fun settleInfo(show: Boolean) {
        if (show) details = true
        val start = dragY
        scope.launch {
            animate(
                initialValue = start,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 720f),
            ) { value, _ -> dragY = value }
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewerHeight = it.height.toFloat() }.background(Color.Black).viewerInfoGesture(
        key = media.id,
        enabled = infoGestureEnabled,
        onDrag = { dragY = it },
        onRelease = ::settleInfo,
    )) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
            ),
            // ZoomImage only consumes a horizontal drag while its content can still pan in
            // that direction. At either image edge the same gesture naturally falls through
            // to this pager, matching Samsung's "pan, then continue to the next item" behavior.
            userScrollEnabled = true,
            key = { page -> items[page].id },
        ) { page ->
            val candidate = items[page]
            Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
                translationY = if (candidate.id == media.id) dragY * .16f else 0f
            }) {
                if (candidate.id != media.id) {
                    PublicAdjacentPreview(candidate, chromeVisible)
                } else if (media.kind == MediaKind.VIDEO) PublicVideo(
                    media = media,
                    player = inlinePlayer,
                    chromeVisible = chromeVisible,
                    muted = videoMuted,
                    onChromeToggle = { chromeVisible = !chromeVisible },
                    onMutedChange = { videoMuted = it },
                    onMediaCreated = onMediaCreated,
                    onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                ) else if (motionPhotoPlaying && motionPhoto != null) {
                    SamsungInlineVideoPlayer(
                        player = inlinePlayer,
                        mediaKey = "motion-${media.id}",
                        durationHintMs = motionPhoto!!.durationMs,
                        chromeVisible = chromeVisible,
                        muted = videoMuted,
                        bottomPadding = SamsungViewerActionBarHeight.value.toInt(),
                        onChromeToggle = { chromeVisible = !chromeVisible },
                        onMutedChange = { videoMuted = it },
                        onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                    )
                } else {
                    ZoomableImage(
                        media = media,
                        onTap = { chromeVisible = !chromeVisible },
                        onZoomChanged = { zoomed -> infoGestureEnabled = !zoomed },
                    )
                    if (motionPhoto != null && chromeVisible) {
                        IconButton(
                            onClick = { motionPhotoPlaying = true },
                            modifier = Modifier.align(Alignment.Center).size(64.dp)
                                .background(Color(0xB827272B), CircleShape),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, "Play Motion Photo", tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                    }
                }
            }
        }
        if (showRecognizedText && media.kind == MediaKind.IMAGE) {
            recognizedDocument?.let { document ->
                OcrWordOverlay(document, media.width, media.height, Modifier.fillMaxSize())
            }
        }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.TopCenter)) { Row(Modifier.fillMaxWidth().background(Color(0x52000000)).statusBarsPadding().height(80.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            SamsungTextRecognitionButton(
                visible = recognizedDocument != null,
                onClick = { showRecognizedText = true },
            )
            IconButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS)) }
                    .onFailure { Toast.makeText(context, "Wireless display settings are unavailable", Toast.LENGTH_SHORT).show() }
            }) { Icon(Icons.Outlined.Slideshow, "Smart View", tint = Color.White) }
            Box {
                IconButton(onClick = { moreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More options", tint = Color.White) }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Details") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { moreMenu = false; details = true })
                    if (media.kind == MediaKind.VIDEO) {
                        // Match the concise Samsung video overflow menu; editing, sharing and
                        // deletion remain in their dedicated bottom-bar actions.
                        DropdownMenuItem(text = { Text("Open in Video player") }, onClick = {
                            moreMenu = false; openExternalVideo(context, media, items)
                        })
                        DropdownMenuItem(text = { Text("Create GIF") }, onClick = { moreMenu = false; onCreateGif() })
                        DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                            moreMenu = false; setAsWallpaper(context, media)
                        })
                        DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(true) })
                    } else {
                        DropdownMenuItem(text = { Text("Scan document") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }, onClick = {
                            moreMenu = false; onScanDocument()
                        })
                        DropdownMenuItem(text = { Text("Copy to clipboard") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = {
                            moreMenu = false
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newUri(context.contentResolver, media.name, media.uri))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text("Copy to album") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { moreMenu = false; transferMove = false })
                        DropdownMenuItem(text = { Text("Move to album") }, leadingIcon = { Icon(Icons.Outlined.Collections, null) }, onClick = { moreMenu = false; transferMove = true })
                        DropdownMenuItem(text = { Text("AI photo assist") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }, onClick = { moreMenu = false; onAiAssist() })
                        DropdownMenuItem(text = { Text("Find visually similar") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, onClick = { moreMenu = false; onFindSimilar() })
                        DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = {
                            moreMenu = false; setAsWallpaper(context, media)
                        })
                        DropdownMenuItem(text = { Text("Print") }, leadingIcon = { Icon(Icons.Outlined.Print, null) }, onClick = {
                            moreMenu = false; printImage(context, media)
                        })
                        DropdownMenuItem(text = { Text("Copy to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(false) })
                        DropdownMenuItem(text = { Text("Move to Secure Gallery") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, onClick = { moreMenu = false; onSecure(true) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Move to recycle bin") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { moreMenu = false; deletePrompt = true })
                    }
                }
            }
        } }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.BottomCenter)) {
            SamsungCenterFilmstrip(
                items = items,
                currentKey = media.id,
                key = { it.id },
                isVideo = { it.kind == MediaKind.VIDEO },
                durationMs = { it.durationMs },
                mediaWidth = { it.width },
                mediaHeight = { it.height },
                videoPositionMs = { candidate -> if (candidate.id == media.id) inlinePlayer.currentPosition else 0L },
                videoPlaying = { candidate -> candidate.id == media.id && inlinePlayer.isPlaying },
                videoFrameSource = { it.uri },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .padding(bottom = SamsungViewerActionBarHeight).height(49.dp),
                onCurrentChanged = { candidate ->
                    if (candidate.id != media.id) onNavigate(candidate)
                },
                onVideoTapped = { candidate ->
                    stripTargetId = candidate.id
                    stripTargetPositionMs = if (candidate.id == media.id) inlinePlayer.currentPosition else 0L
                    stripTargetPlaying = candidate.id == media.id && inlinePlayer.isPlaying
                    if (candidate.id != media.id) onNavigate(candidate)
                },
                onVideoScrub = { candidate, position, final ->
                    if (candidate.id != media.id) {
                        stripTargetId = candidate.id
                        stripTargetPositionMs = position
                        stripTargetPlaying = false
                        onNavigate(candidate)
                    } else {
                        stripTargetId = media.id
                        stripTargetPositionMs = position
                        stripTargetPlaying = if (stripScrubInProgress) resumeAfterStripScrub else inlinePlayer.playWhenReady
                        if (final) {
                            inlinePlayer.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                            inlinePlayer.seekTo(position)
                            lastStripSeekDispatchMs = android.os.SystemClock.uptimeMillis()
                            if (inlinePlayer.isScrubbingModeEnabled) inlinePlayer.setScrubbingModeEnabled(false)
                            stripTargetPlaying = resumeAfterStripScrub
                            inlinePlayer.playWhenReady = resumeAfterStripScrub
                            stripScrubInProgress = false
                        } else {
                            if (!stripScrubInProgress) {
                                resumeAfterStripScrub = inlinePlayer.playWhenReady &&
                                    inlinePlayer.playbackState != Player.STATE_ENDED
                                stripScrubInProgress = true
                            }
                            if (!inlinePlayer.isScrubbingModeEnabled) inlinePlayer.setScrubbingModeEnabled(true)
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastStripSeekDispatchMs >= 32L) {
                                inlinePlayer.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                inlinePlayer.seekTo(position)
                                inlinePlayer.playWhenReady = resumeAfterStripScrub
                                lastStripSeekDispatchMs = now
                            }
                        }
                    }
                },
                thumbnail = { candidate, modifier ->
                    Box(modifier) {
                        val request = ImageRequest.Builder(context).data(candidate.uri).apply {
                            if (candidate.kind == MediaKind.VIDEO) videoFrameMillis(1_000)
                        }.build()
                        GalleryThumbnail(candidate, request)
                    }
                },
            )
        }
        if (dragY < -8f) Card(
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 96.dp).graphicsLayer {
                translationY = (viewerHeight * .18f + dragY).coerceAtLeast(0f)
            },
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(media.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(media.bucketName, color = VaultSecondary)
                Text("Release for details", color = VaultBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
        AnimatedVisibility(chromeVisible, modifier = Modifier.align(Alignment.BottomCenter)) { Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(SamsungViewerActionBarHeight).background(Color(0x66000000)).padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SamsungViewerAction(Icons.Outlined.Favorite, if (media.isFavourite) "Remove from favourites" else "Add to favourites", onClick = onFavourite)
            SamsungViewerAction(Icons.Outlined.Edit, "Edit", onClick = onEdit)
            SamsungViewerAction(Icons.Outlined.AutoAwesome, "AI photo assist", enabled = media.kind == MediaKind.IMAGE, onClick = onAiAssist)
            SamsungViewerAction(Icons.Outlined.Share, "Share", onClick = onShare)
            SamsungViewerAction(Icons.Outlined.Delete, "Move to recycle bin") { deletePrompt = true }
        } }
    }
    if (showRecognizedText) recognizedDocument?.let { document ->
        RecognizedTextSheet(
            document = document,
            onDismiss = { showRecognizedText = false },
            onScanDocument = {
                showRecognizedText = false
                onScanDocument()
            },
        )
    }
    if (deletePrompt) AlertDialog(
        onDismissRequest = { deletePrompt = false },
        title = { Text("Move to recycle bin?") },
        text = { Text("Android keeps the item in the system recycle bin so you can restore it later.") },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { deletePrompt = false; onDelete() }) { Text("Move to bin") } },
    )
    transferMove?.let { move ->
        PublicAlbumDestinationPicker(
            move = move,
            albums = destinationAlbums,
            sourceAlbumNames = setOf(media.bucketName),
            onDismiss = { transferMove = null },
            onSelect = { target ->
                transferMove = null
                (context as? MainGalleryActivity)?.transferMediaToAlbum(listOf(media), target, move)
            },
        )
    }
    if (details) SamsungMediaDetailsSheet(media, onDismiss = { details = false }, onEdit = { details = false; onEdit() })
}

@Composable
private fun SamsungMediaDetailsSheet(media: GalleryMedia, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    var rating by remember(media.id) { mutableIntStateOf(runCatching { GallerySearchIndex(context).use { it.rating(media.id) } }.getOrDefault(0)) }
    var tag by remember(media.id) { mutableStateOf(runCatching { GallerySearchIndex(context).use { it.tags(media.id) } }.getOrDefault("")) }
    var editingTag by remember { mutableStateOf(false) }
    var extendedMetadataVisible by remember { mutableStateOf(false) }
    val megapixels = (media.width.toLong() * media.height.toLong()) / 1_000_000.0
    val technicalInfo by produceState<VideoTechnicalInfo?>(initialValue = null, media.uri, media.kind) {
        value = if (media.kind == MediaKind.VIDEO) withContext(Dispatchers.IO) {
            runCatching { readVideoTechnicalInfo(context, media.uri) }.getOrNull()
        } else null
    }
    val imageInfo by produceState<ImageTechnicalInfo?>(initialValue = null, media.uri, media.kind) {
        value = if (media.kind == MediaKind.IMAGE) withContext(Dispatchers.IO) {
            runCatching { readImageTechnicalInfo(context, media.uri) }.getOrNull()
        } else null
    }
    val extendedMetadata by produceState<List<ExtendedMetadataSection>?>(initialValue = null, media.uri) {
        value = withContext(Dispatchers.IO) { runCatching { readExtendedMetadata(context, media.uri) }.getOrDefault(emptyList()) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        shape = RectangleShape,
        dragHandle = null,
        scrimColor = Color.Transparent,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.58f).padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(media.dateTakenMs)), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("Edit") }
            }
            Text(media.name, style = MaterialTheme.typography.titleMedium)
            Text("/Internal storage/${media.relativePath.trimEnd('/')}", color = VaultSecondary)
            if (media.kind == MediaKind.VIDEO) {
                val resolutionClass = videoResolutionClass(media.width, media.height)
                Text("${GalleryLogic.fileSizeLabel(media.sizeBytes)}  |  ${media.width} × ${media.height}${resolutionClass?.let { "  |  $it" }.orEmpty()}", color = VaultSecondary)
                val details = buildList {
                    add(GalleryLogic.durationLabel(media.durationMs))
                    technicalInfo?.videoCodec?.let(::add)
                    technicalInfo?.audioCodec?.let(::add)
                    technicalInfo?.frameRate?.let { add("$it fps") }
                    technicalInfo?.bitrate?.let(::add)
                    technicalInfo?.colorSpace?.let(::add)
                    technicalInfo?.hdr?.let(::add)
                }.joinToString("  |  ")
                Text(details, color = VaultSecondary)
            } else {
                Text("${GalleryLogic.fileSizeLabel(media.sizeBytes)}  |  ${media.width} × ${media.height}  |  ${"%.1f".format(Locale.ROOT, megapixels)} MP", color = VaultSecondary)
                Text(media.mimeType, color = VaultSecondary)
                imageInfo?.camera?.let { Text("Camera  $it", color = VaultSecondary) }
                imageInfo?.lens?.let { Text("Lens  $it", color = VaultSecondary) }
                listOfNotNull(imageInfo?.exposure, imageInfo?.aperture, imageInfo?.iso, imageInfo?.focalLength)
                    .takeIf { it.isNotEmpty() }?.let { Text(it.joinToString("  |  "), color = VaultSecondary) }
                imageInfo?.colorSpace?.let { Text("Colour space  $it", color = VaultSecondary) }
            }
            val latitude = imageInfo?.latitude
            val longitude = imageInfo?.longitude
            if (latitude != null && longitude != null) Text("Location  $latitude, $longitude", color = VaultSecondary)
            if (!extendedMetadata.isNullOrEmpty()) {
                TextButton(onClick = { extendedMetadataVisible = !extendedMetadataVisible }) { Text(if (extendedMetadataVisible) "Hide extended metadata" else "Show extended metadata") }
                if (extendedMetadataVisible) Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    extendedMetadata.orEmpty().forEach { section ->
                        Text(section.name, style = MaterialTheme.typography.titleSmall)
                        section.values.forEach { (name, value) -> Text("$name  $value", color = VaultSecondary, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rating", color = VaultSecondary, modifier = Modifier.width(72.dp))
                (1..5).forEach { value ->
                    IconButton(onClick = {
                        rating = if (rating == value) 0 else value
                        GallerySearchIndex(context).use { it.setRating(media.id, rating) }
                    }) {
                        Icon(if (value <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, "$value star rating", tint = if (value <= rating) Color(0xFFFFD60A) else VaultSecondary)
                    }
                }
            }
            if (editingTag) {
                OutlinedTextField(tag, { tag = it.take(40) }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { editingTag = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        GallerySearchIndex(context).use { it.setTags(media.id, tag.trim()) }
                        editingTag = false
                        Toast.makeText(context, "Tag saved", Toast.LENGTH_SHORT).show()
                    }) { Text("Save") }
                }
            } else TextButton(onClick = { editingTag = true }) { Text(if (tag.isBlank()) "Add tag" else "Tag: $tag") }
        }
    }
}

@Composable
private fun MediaDetailsSheet(media: GalleryMedia, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    var rating by remember(media.id) { mutableIntStateOf(runCatching { GallerySearchIndex(context).use { it.rating(media.id) } }.getOrDefault(0)) }
    var tag by remember(media.id) { mutableStateOf(runCatching { GallerySearchIndex(context).use { it.tags(media.id) } }.getOrDefault("")) }
    var editingTag by remember { mutableStateOf(false) }
    val megapixels = (media.width.toLong() * media.height.toLong()) / 1_000_000.0
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = VaultSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(media.dateTakenMs)), style = MaterialTheme.typography.headlineMedium)
            Text(media.name, style = MaterialTheme.typography.titleMedium)
            Text("${media.relativePath}${media.name}", color = VaultSecondary)
            Text(media.bucketName, color = VaultSecondary)
            Text("${GalleryLogic.fileSizeLabel(media.sizeBytes)}   ${media.width} × ${media.height}   ${"%.1f".format(Locale.ROOT, megapixels)} MP", color = VaultSecondary)
            if (media.kind == MediaKind.VIDEO) Text("Duration ${GalleryLogic.durationLabel(media.durationMs)}", color = VaultSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rating", color = VaultSecondary, modifier = Modifier.width(72.dp))
                (1..5).forEach { value -> IconButton(onClick = {
                    rating = if (rating == value) 0 else value
                    GallerySearchIndex(context).use { it.setRating(media.id, rating) }
                }) { Icon(if (value <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, "$value star rating", tint = if (value <= rating) Color(0xFFFFD60A) else VaultSecondary) } }
            }
            Text(media.mimeType, color = VaultSecondary)
            if (editingTag) {
                OutlinedTextField(tag, { tag = it.take(40) }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onClick = { editingTag = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        GallerySearchIndex(context).use { it.setTags(media.id, tag.trim()) }
                        editingTag = false
                        Toast.makeText(context, "Tag saved", Toast.LENGTH_SHORT).show()
                    }) { Text("Save") }
                }
            } else {
                TextButton(onClick = { editingTag = true }) { Text(if (tag.isBlank()) "Add tag" else "Tag: $tag") }
            }
            HorizontalDivider(color = Color(0xFF3A3A3E))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun PublicPhotoEditor(media: GalleryMedia, launchMode: EditorLaunchMode, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, media.uri) {
        value = withContext(Dispatchers.IO) { decodeBitmapForEditing(context, media.uri) }
    }
    Box(Modifier.fillMaxSize()) {
        PhotoEditorScreen(media.name, bitmap, saving, onBack, launchMode, media.uri.toString()) { edited, mode, exportOptions ->
            saving = true
            val safeMode = if (mode == EditorSaveMode.REPLACE && !canSafelyOverwritePhoto(media.mimeType)) EditorSaveMode.COPY else mode
            (context as? MainGalleryActivity)?.saveEditedPhoto(media, edited, safeMode, exportOptions) { saved ->
                saving = false
                saved.onSuccess {
                    Toast.makeText(
                        context,
                        when {
                            mode == EditorSaveMode.REPLACE && safeMode == EditorSaveMode.COPY -> "${media.mimeType.substringAfter('/').uppercase()} cannot be overwritten safely. An edited JPEG copy was saved in ${media.bucketName}."
                            safeMode == EditorSaveMode.REPLACE -> "Photo saved"
                            else -> "Edited copy saved in ${media.bucketName}"
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                    onSaved()
                }
                    .onFailure { error = it.message ?: "Could not save edited copy" }
            }
        }
        error?.let { message ->
            AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } })
        }
    }
}

@Composable
private fun PublicVideoEditor(media: GalleryMedia, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    VideoEditorScreen(media.name, media.uri, media.durationMs, onBack = onBack, projectSource = media.uri.toString()) { output, result, mode ->
        (context as? MainGalleryActivity)?.saveEditedVideo(media, output, mode) { saved ->
            saved.onSuccess {
                Toast.makeText(
                    context,
                    if (mode == VideoEditorSaveMode.REPLACE) "Video saved" else "Edited ${result.width}×${result.height} copy saved in ${media.bucketName}",
                    Toast.LENGTH_LONG,
                ).show()
                onSaved()
            }.onFailure { error = it.message ?: "Could not save the edited video" }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@Composable
private fun PublicTrashScreen(
    media: List<GalleryMedia>,
    onBack: () -> Unit,
    onRestore: (GalleryMedia) -> Unit,
    onDelete: (GalleryMedia) -> Unit,
    onEmpty: () -> Unit,
) {
    var selected by remember { mutableStateOf<GalleryMedia?>(null) }
    var emptyPrompt by remember { mutableStateOf(false) }
    val columns = remember { mutableIntStateOf(4) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(
            title = "Recycle bin",
            back = onBack,
            extra = { if (media.isNotEmpty()) TextButton(onClick = { emptyPrompt = true }) { Text("Empty") } },
        )
        Text("Items remain here according to Android's system recycle-bin policy.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recycle bin is empty", color = VaultSecondary) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns.intValue),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.pinchToResizeGrid(columns, 3, 12),
            ) { items(media, key = { it.id }) { item -> MediaTile(item, false, { selected = it }, null) } }
        }
    }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.name) },
            text = { Text("Restore this item or delete it permanently?") },
            dismissButton = { TextButton(onClick = { selected = null; onDelete(item) }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            confirmButton = { TextButton(onClick = { selected = null; onRestore(item) }) { Text("Restore") } },
        )
    }
    if (emptyPrompt) AlertDialog(
        onDismissRequest = { emptyPrompt = false },
        title = { Text("Empty recycle bin?") },
        text = { Text("All ${media.size} items will be permanently deleted. This cannot be undone.") },
        dismissButton = { TextButton(onClick = { emptyPrompt = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { emptyPrompt = false; onEmpty() }) { Text("Delete all", color = MaterialTheme.colorScheme.error) } },
    )
}

@Composable
private fun PublicAdjacentPreview(media: GalleryMedia, chromeVisible: Boolean) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val request = remember(media.uri) {
            ImageRequest.Builder(context).data(media.uri).apply { if (media.kind == MediaKind.VIDEO) videoFrameMillis(1_000) }.build()
        }
        AsyncImage(request, media.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (media.kind == MediaKind.VIDEO) SamsungAdjacentVideoChrome(
            durationMs = media.durationMs,
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ZoomableImage(
    media: GalleryMedia,
    onTap: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val zoomState = rememberCoilZoomState()
    val rawPreview by produceState<android.graphics.Bitmap?>(null, media.uri, media.mimeType) {
        value = if (isRawImage(media.mimeType, media.name)) withContext(Dispatchers.IO) {
            decodeBitmapForEditing(context, media.uri, maxDimension = 4096)
        } else null
    }
    LaunchedEffect(zoomState) {
        snapshotFlow { zoomState.zoomable.transform.scaleX > zoomState.zoomable.minScale * 1.01f }
            .distinctUntilChanged()
            .collect(onZoomChanged)
    }
    Box(Modifier.fillMaxSize()) {
        CoilZoomAsyncImage(
            model = rawPreview ?: media.uri,
            contentDescription = media.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            zoomState = zoomState,
            onTap = { onTap() },
        )
        if (isRawImage(media.mimeType, media.name)) {
            Text(
                "RAW preview",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PublicVideo(
    media: GalleryMedia,
    player: ExoPlayer,
    chromeVisible: Boolean,
    muted: Boolean,
    onChromeToggle: () -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onMediaCreated: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SamsungInlineVideoPlayer(
        player = player,
        mediaKey = media.id,
        durationHintMs = media.durationMs,
        chromeVisible = chromeVisible,
        muted = muted,
        bottomPadding = SamsungViewerActionBarHeight.value.toInt(),
        onChromeToggle = onChromeToggle,
        onMutedChange = onMutedChange,
        onZoomChanged = onZoomChanged,
        onCaptureFrame = {
        val position = player.currentPosition
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { captureVideoFrame(context, media, position) } }
            result.onSuccess { Toast.makeText(context, "Frame saved in ${media.bucketName}", Toast.LENGTH_SHORT).show(); onMediaCreated() }
                .onFailure { Toast.makeText(context, it.message ?: "Could not capture frame", Toast.LENGTH_LONG).show() }
        }
    },
    )
}

private fun shareUris(activity: Activity, uris: List<Uri>, mime: String) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    intent.type = mime.ifBlank { "*/*" }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    activity.startActivity(Intent.createChooser(intent, "Share media"))
}

private fun openExternalVideo(
    context: android.content.Context,
    media: GalleryMedia,
    sourceItems: List<GalleryMedia> = listOf(media),
): Boolean = runCatching {
    val videos = sourceItems.filter { it.kind == MediaKind.VIDEO }.ifEmpty { listOf(media) }
    val queueIndex = videos.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
    context.startActivity(
        Intent(context, VaultVideoPlayerActivity::class.java)
            .setData(media.uri)
            .putExtra(Intent.EXTRA_TITLE, media.name)
            .putStringArrayListExtra("queue_uris", ArrayList(videos.map { it.uri.toString() }))
            .putStringArrayListExtra("queue_titles", ArrayList(videos.map { it.name }))
            .putExtra("queue_index", queueIndex)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
    true
}.getOrElse {
    Toast.makeText(context, "Could not open Vault Video Player; using Gallery instead", Toast.LENGTH_SHORT).show()
    false
}

private fun setAsWallpaper(context: android.content.Context, media: GalleryMedia) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(media.uri, media.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra("mimeType", media.mimeType))
    }.onFailure { Toast.makeText(context, "No compatible wallpaper app is installed", Toast.LENGTH_SHORT).show() }
}

private fun printImage(context: android.content.Context, media: GalleryMedia) {
    runCatching {
        androidx.print.PrintHelper(context).apply {
            scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
            colorMode = androidx.print.PrintHelper.COLOR_MODE_COLOR
        }.printBitmap(media.name, media.uri)
    }.onFailure { Toast.makeText(context, "Printing is unavailable on this device", Toast.LENGTH_SHORT).show() }
}

private fun captureVideoFrame(context: android.content.Context, source: GalleryMedia, positionMs: Long): Uri {
    val retriever = android.media.MediaMetadataRetriever()
    val bitmap = try {
        retriever.setDataSource(context, source.uri)
        retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
            ?: error("This video frame could not be decoded")
    } finally {
        runCatching { retriever.release() }
    }
    val resolver = context.contentResolver
    val safeAlbum = source.bucketName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Pictures" }
    val frameTakenMs = source.dateTakenMs.takeIf { it > 0L }?.plus(positionMs.coerceAtLeast(0L))
        ?: System.currentTimeMillis()
    val values = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${source.name.substringBeforeLast('.')}_${GalleryLogic.durationLabel(positionMs).replace(':', '-')}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        // A video's existing path may be under Android/media (for example WhatsApp). Android
        // deliberately rejects new MediaStore inserts into Android/*, so captured stills always
        // go to a legal Pictures album while retaining the source album name.
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$safeAlbum/")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
        put(MediaStore.Images.ImageColumns.DATE_TAKEN, frameTakenMs)
    }
    val destination = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: run {
        bitmap.recycle(); error("Could not create captured frame")
    }
    try {
        resolver.openOutputStream(destination, "w")?.use { output ->
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 96, output))
        } ?: error("Could not write captured frame")
        resolver.openFileDescriptor(destination, "rw")?.use { descriptor ->
            val exif = androidx.exifinterface.media.ExifInterface(descriptor.fileDescriptor)
            val timestamp = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(frameTakenMs))
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, timestamp)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
            exif.setAttribute(
                androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                "Captured from ${source.name} at ${positionMs.coerceAtLeast(0L)} ms; source MediaStore id ${source.id}",
            )
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, "Video snapshot from ${source.name}")
            exif.saveAttributes()
        }
        resolver.update(destination, android.content.ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return destination
    } catch (error: Throwable) {
        resolver.delete(destination, null, null)
        throw error
    } finally {
        bitmap.recycle()
    }
}

private fun copyUrisToClipboard(context: android.content.Context, media: List<GalleryMedia>) {
    if (media.isEmpty()) return
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    val clip = android.content.ClipData.newUri(context.contentResolver, media.first().name, media.first().uri)
    media.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it.uri)) }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied ${media.size} item${if (media.size == 1) "" else "s"} to clipboard", Toast.LENGTH_SHORT).show()
}

private fun copyMediaToTree(context: android.content.Context, media: List<GalleryMedia>, treeUri: Uri): Int {
    val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return 0
    var copied = 0
    media.forEach { item ->
        runCatching {
            val target = directory.createFile(item.mimeType.ifBlank { "application/octet-stream" }, item.name) ?: error("Could not create file")
            context.contentResolver.openInputStream(item.uri).use { input ->
                requireNotNull(input)
                context.contentResolver.openOutputStream(target.uri, "w").use { output -> requireNotNull(output); input.copyTo(output) }
            }
        }.onSuccess { copied++ }
    }
    return copied
}

private fun copyUrisToTree(context: android.content.Context, uris: List<Uri>, treeUri: Uri): Int {
    val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return 0
    var copied = 0
    uris.forEachIndexed { index, uri ->
        runCatching {
            var name = "Media ${index + 1}"
            var mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(0).orEmpty().ifBlank { name }
            }
            val target = directory.createFile(mime, name) ?: error("Could not create file")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                context.contentResolver.openOutputStream(target.uri, "w").use { output -> requireNotNull(output); input.copyTo(output) }
            }
        }.onSuccess { copied++ }
    }
    return copied
}

private fun copyUrisToAlbum(context: android.content.Context, uris: List<Uri>, albumName: String): Int {
    val safeAlbum = albumName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "New album" }
    var copied = 0
    uris.forEachIndexed { index, source ->
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(source).orEmpty().ifBlank { "image/jpeg" }
            var name = "Media ${index + 1}"
            resolver.query(source, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(0).orEmpty().ifBlank { name }
            }
            val collection = if (mime.startsWith("video/")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$safeAlbum")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val destination = resolver.insert(collection, values) ?: error("Could not create destination")
            try {
                resolver.openInputStream(source).use { input ->
                    requireNotNull(input)
                    resolver.openOutputStream(destination, "w").use { output -> requireNotNull(output); input.copyTo(output) }
                }
                resolver.update(destination, android.content.ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (error: Throwable) {
                resolver.delete(destination, null, null)
                throw error
            }
        }.onSuccess { copied++ }
    }
    return copied
}

private fun copyMediaToNamedAlbum(context: android.content.Context, media: List<GalleryMedia>, albumName: String): Int {
    val safeAlbum = albumName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "New album" }
    var copied = 0
    media.forEach { item ->
        runCatching {
            val resolver = context.contentResolver
            val collection = if (item.kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$safeAlbum")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.Images.ImageColumns.DATE_TAKEN, item.dateTakenMs)
            }
            val destination = resolver.insert(collection, values) ?: error("Could not create destination")
            try {
                resolver.openInputStream(item.uri).use { input ->
                    requireNotNull(input)
                    resolver.openOutputStream(destination, "w").use { output -> requireNotNull(output); input.copyTo(output) }
                }
                resolver.update(destination, android.content.ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (error: Throwable) {
                resolver.delete(destination, null, null); throw error
            }
        }.onSuccess { copied++ }
    }
    return copied
}

/** Builds a viewer item for content opened by another app without importing or copying it. */
private fun resolveExternalMedia(context: android.content.Context, uri: Uri, suppliedMime: String?): GalleryMedia? {
    val resolver = context.contentResolver
    val mime = suppliedMime?.takeUnless { it == "*/*" } ?: resolver.getType(uri).orEmpty()
    val kind = when {
        mime.startsWith("video/") -> MediaKind.VIDEO
        mime.startsWith("image/") -> MediaKind.IMAGE
        else -> return null
    }
    var name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Media"
    var size = 0L
    runCatching {
        resolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { name = cursor.getString(it).orEmpty().ifBlank { name } }
                cursor.getColumnIndex(android.provider.OpenableColumns.SIZE).takeIf { it >= 0 }
                    ?.let { size = cursor.getLong(it) }
            }
        }
    }
    var width = 0
    var height = 0
    var duration = 0L
    if (kind == MediaKind.IMAGE) {
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.Options().also { options ->
                    options.inJustDecodeBounds = true
                    android.graphics.BitmapFactory.decodeStream(input, null, options)
                    width = options.outWidth.coerceAtLeast(0)
                    height = options.outHeight.coerceAtLeast(0)
                }
            }
        }
    } else {
        runCatching {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        }
    }
    return GalleryMedia(
        id = Long.MIN_VALUE + uri.toString().hashCode().toLong(),
        uri = uri,
        name = name,
        mimeType = mime,
        kind = kind,
        width = width,
        height = height,
        durationMs = duration,
        sizeBytes = size,
        dateTakenMs = System.currentTimeMillis(),
        bucketId = Long.MIN_VALUE,
        bucketName = "Opened media",
        isFavourite = false,
    )
}

internal fun decodeBitmapForEditing(context: android.content.Context, uri: Uri, maxDimension: Int = Int.MAX_VALUE): android.graphics.Bitmap? {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= 28) {
        return runCatching {
            val source = android.graphics.ImageDecoder.createSource(resolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val safetyIssue = imageDecodeSafetyIssue(width, height, mimeType = resolver.getType(uri))
                require(safetyIssue == null) { safetyIssue ?: "Unsafe image payload" }
                val longest = maxOf(width, height)
                if (maxDimension != Int.MAX_VALUE && longest > maxDimension) {
                    val scale = maxDimension.toFloat() / longest
                    decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
                }
            }
        }.getOrNull()
    }
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    if (imageDecodeSafetyIssue(bounds.outWidth, bounds.outHeight, mimeType = bounds.outMimeType ?: resolver.getType(uri)) != null) return null
    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) sample *= 2
    val options = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
}

internal fun saveEditedBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap, source: GalleryMedia, exportOptions: PhotoExportOptions? = null): Uri {
    val resolver = context.contentResolver
    val base = source.name.substringBeforeLast('.', source.name)
    val preservePng = exportOptions == null && source.mimeType.equals("image/png", true)
    val preserveWebp = exportOptions == null && Build.VERSION.SDK_INT >= 30 && source.mimeType.equals("image/webp", true)
    val requestedFormat = exportOptions?.format
    val extension = requestedFormat?.extension ?: if (preservePng) "png" else if (preserveWebp) "webp" else "jpg"
    val mimeType = requestedFormat?.mimeType ?: if (preservePng) "image/png" else if (preserveWebp) "image/webp" else "image/jpeg"
    val format = when (requestedFormat) {
        PhotoOutputFormat.PNG -> android.graphics.Bitmap.CompressFormat.PNG
        PhotoOutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) android.graphics.Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") android.graphics.Bitmap.CompressFormat.WEBP
        PhotoOutputFormat.JPEG -> android.graphics.Bitmap.CompressFormat.JPEG
        PhotoOutputFormat.HEIC, PhotoOutputFormat.AVIF -> null
        null -> if (preservePng) android.graphics.Bitmap.CompressFormat.PNG else if (preserveWebp) android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS else android.graphics.Bitmap.CompressFormat.JPEG
    }
    val quality = exportOptions?.quality?.coerceIn(1, 100) ?: if (format == android.graphics.Bitmap.CompressFormat.JPEG) 95 else 100
    val name = "${base}_edited_${System.currentTimeMillis()}.$extension"
    val preservedJpegMetadata = if (mimeType == "image/jpeg" && exportOptions?.stripMetadata != true) {
        runCatching { resolver.openInputStream(source.uri)?.use(::extractPreservableJpegMetadata).orEmpty() }.getOrDefault(emptyList())
    } else emptyList()
    val safeAlbum = source.bucketName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Pictures" }
    // Keep the edit beside its source, when Android permits writing to that path.
    val destination = source.relativePath.trim('/').takeIf { path ->
        path.isNotBlank() && !path.split('/').any { it == "." || it == ".." } &&
            (path.startsWith("DCIM/") || path.startsWith("Pictures/") || path.startsWith("Download/") || path.startsWith("Movies/"))
    } ?: "Pictures/$safeAlbum"
    val values = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "$destination/")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
        if (exportOptions?.stripMetadata != true) {
            put(MediaStore.Images.ImageColumns.DATE_TAKEN, source.dateTakenMs)
            put(MediaStore.MediaColumns.DATE_ADDED, source.dateTakenMs / 1000L)
            put(MediaStore.MediaColumns.DATE_MODIFIED, source.dateTakenMs / 1000L)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create edited image")
    try {
        if (requestedFormat == PhotoOutputFormat.HEIC || requestedFormat == PhotoOutputFormat.AVIF) {
            resolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ModernImageEncoder.encode(bitmap, requestedFormat, quality, descriptor.fileDescriptor)
            } ?: error("Could not write edited image")
        } else if (mimeType == "image/jpeg" && preservedJpegMetadata.isNotEmpty()) {
            val temporary = java.io.File.createTempFile("edited-jpeg-", ".jpg", context.cacheDir)
            try {
                temporary.outputStream().buffered().use { output ->
                    check(bitmap.compress(checkNotNull(format), quality, output)) { "Could not encode edited image" }
                }
                resolver.openOutputStream(uri, "w")?.use { output ->
                    temporary.inputStream().use { encoded -> writeJpegWithPreservedMetadata(encoded, output, preservedJpegMetadata) }
                } ?: error("Could not write edited image")
            } finally {
                ModernImageEncoder.securelyDelete(temporary)
            }
        } else {
            resolver.openOutputStream(uri, "w")?.use { output ->
                check(bitmap.compress(checkNotNull(format), quality, output)) { "Could not encode edited image" }
            } ?: error("Could not write edited image")
        }
        if (exportOptions?.stripMetadata != true) copyImageMetadata(context, source.uri, uri, source.dateTakenMs)
        resolver.update(uri, android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            if (exportOptions?.stripMetadata != true) {
                put(MediaStore.Images.ImageColumns.DATE_TAKEN, source.dateTakenMs)
                put(MediaStore.MediaColumns.DATE_ADDED, source.dateTakenMs / 1000L)
            }
            put(MediaStore.MediaColumns.WIDTH, bitmap.width)
            put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
        }, null, null)
        return uri
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    }
}

internal fun canSafelyOverwritePhoto(mimeType: String): Boolean = mimeType.lowercase() in setOf(
    "image/jpeg", "image/jpg", "image/png", "image/webp",
)

/** Keep capture metadata on edited copies while normalising orientation because ImageDecoder has
 * already applied it to the pixels. Dimensions and compression-specific fields are intentionally
 * not copied. */
private val preservedExifTags = listOf(
    androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
    androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
    androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED,
    androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME,
    androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    androidx.exifinterface.media.ExifInterface.TAG_MAKE,
    androidx.exifinterface.media.ExifInterface.TAG_MODEL,
    androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL,
    androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER,
    androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME,
    androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH,
    androidx.exifinterface.media.ExifInterface.TAG_FLASH,
    androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE,
    androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
)

private data class ImageMetadataSnapshot(
    val attributes: Map<String, String>,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
)

private fun readImageMetadata(context: android.content.Context, source: Uri, fallbackDateMs: Long = 0L): ImageMetadataSnapshot? = runCatching {
    context.contentResolver.openFileDescriptor(source, "r")?.use { sourceFd ->
        val exif = androidx.exifinterface.media.ExifInterface(sourceFd.fileDescriptor)
        val location = exif.latLong
        val attributes = preservedExifTags.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap().toMutableMap()
        if (fallbackDateMs > 0L && androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL !in attributes) {
            val formatted = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(fallbackDateMs))
            attributes[androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL] = formatted
            attributes.putIfAbsent(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, formatted)
        }
        ImageMetadataSnapshot(
            attributes = attributes,
            latitude = location?.getOrNull(0),
            longitude = location?.getOrNull(1),
            altitude = exif.getAltitude(Double.NaN).takeUnless(Double::isNaN),
        )
    }
}.getOrNull()

private fun writeImageMetadata(context: android.content.Context, destination: Uri, metadata: ImageMetadataSnapshot?) {
    if (metadata == null) return
    runCatching {
        context.contentResolver.openFileDescriptor(destination, "rw")?.use { destinationFd ->
            val exif = androidx.exifinterface.media.ExifInterface(destinationFd.fileDescriptor)
            metadata.attributes.forEach(exif::setAttribute)
            if (metadata.latitude != null && metadata.longitude != null) exif.setLatLong(metadata.latitude, metadata.longitude)
            metadata.altitude?.let(exif::setAltitude)
            exif.setAttribute(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString(),
            )
            exif.saveAttributes()
        }
    }
}

private fun copyImageMetadata(context: android.content.Context, source: Uri, destination: Uri, fallbackDateMs: Long = 0L) {
    writeImageMetadata(context, destination, readImageMetadata(context, source, fallbackDateMs))
}

private fun overwriteEditedBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap, source: GalleryMedia): Uri {
    val metadata = readImageMetadata(context, source.uri)
    val format = when {
        source.mimeType.equals("image/png", true) -> android.graphics.Bitmap.CompressFormat.PNG
        Build.VERSION.SDK_INT >= 30 && source.mimeType.equals("image/webp", true) -> android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS
        else -> android.graphics.Bitmap.CompressFormat.JPEG
    }
    context.contentResolver.openOutputStream(source.uri, "w")?.use { stream ->
        check(bitmap.compress(format, if (format == android.graphics.Bitmap.CompressFormat.JPEG) 97 else 100, stream)) {
            "Could not encode edited photo"
        }
    } ?: error("Could not open the original photo for writing")
    writeImageMetadata(context, source.uri, metadata)
    context.contentResolver.update(
        source.uri,
        android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
            put(MediaStore.MediaColumns.WIDTH, bitmap.width)
            put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
        },
        null,
        null,
    )
    return source.uri
}

private fun launchFirstInEditor(context: android.content.Context, media: List<GalleryMedia>) {
    val item = media.firstOrNull() ?: return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(item.uri, item.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.onFailure { Toast.makeText(context, "No compatible metadata editor is installed", Toast.LENGTH_SHORT).show() }
}

private fun openCreativeChooser(context: android.content.Context, media: List<GalleryMedia>, type: String) {
    if (media.isEmpty()) return
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        this.type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(media.map { it.uri }))
        putExtra(Intent.EXTRA_TITLE, type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Create $type with")) }
        .onFailure { Toast.makeText(context, "No compatible creator is installed", Toast.LENGTH_SHORT).show() }
}

private fun pinCollectionShortcut(context: android.content.Context, title: String) {
    val manager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
    if (manager?.isRequestPinShortcutSupported != true) {
        Toast.makeText(context, "Your launcher does not support pinned shortcuts", Toast.LENGTH_SHORT).show()
        return
    }
    val shortcut = android.content.pm.ShortcutInfo.Builder(context, "collection-${title.lowercase().replace(' ', '-')}")
        .setShortLabel(title)
        .setLongLabel("Open $title in Gallery")
        .setIcon(android.graphics.drawable.Icon.createWithResource(context, com.danyal.vaultgallery.R.drawable.ic_gallery))
        .setIntent(Intent(context, MainGalleryActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra("collection", title))
        .build()
    manager.requestPinShortcut(shortcut, null)
}

private fun launchSecureImport(
    activity: Activity,
    uris: ArrayList<Uri>,
    moveAfterImport: Boolean,
    sourceKind: TransferSourceKind,
    sourceFolderCount: Int,
) {
    activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)
        .putParcelableArrayListExtra("import_uris", uris)
        .putExtra("move_after_import", moveAfterImport)
        .putExtra("transfer_source_kind", sourceKind.name)
        .putExtra("transfer_source_folder_count", sourceFolderCount)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
}

private fun launchSecureGallery(activity: Activity) {
    activity.startActivity(Intent(activity, SecureGalleryActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
}
