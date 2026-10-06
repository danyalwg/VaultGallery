package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenterOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.Stroke as MediaPipeStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.min

internal enum class AiSelectionEffect(
    val label: String,
    val description: String,
    val icon: ImageVector,
) {
    ERASE("Object eraser", "Remove the selected object and rebuild its surroundings", Icons.Outlined.DeleteSweep),
    BACKDROP("New backdrop", "Keep the object and create a clean studio background", Icons.Outlined.ContentCut),
    DEPTH_BLUR("Depth blur", "Create natural bokeh around any selected object", Icons.Outlined.BlurOn),
    RELIGHT("Relight", "Lift the selected object and shape the background light", Icons.Outlined.Highlight),
    COLOR_POP("Color pop", "Keep the selection in color and mute everything else", Icons.Outlined.ColorLens),
    STICKER("Sticker cutout", "Add a crisp cutout edge on a clean canvas", Icons.Outlined.AutoAwesome),
}

private enum class SelectionBrush(val label: String) {
    INCLUDE("Add"),
    REMOVE("Subtract"),
    LASSO("Lasso"),
}

private val AiSelectYellow = Color(0xFFFFD60A)

private data class SelectionStroke(val brush: SelectionBrush, val points: List<Offset>)

/** A full-screen, genuinely model-backed arbitrary-object selection workspace. */
@Composable
internal fun AiSelectionStudio(
    source: Bitmap,
    initialEffect: AiSelectionEffect = AiSelectionEffect.ERASE,
    startWithLasso: Boolean = false,
    onCancel: () -> Unit,
    onApply: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val aiRuntime = remember(context) { AiExecutionRuntime(context) }
    val scope = rememberCoroutineScope()
    // Selection and effect previews are interaction textures. Full resolution is rendered only
    // by Apply, which keeps edge/feather/effect controls responsive without changing output.
    val previewSource = remember(source) { source.scaledForAiPreview(768) }
    var engine by remember(source) { mutableStateOf<MagicTouchSelectionEngine?>(null) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var brush by remember(source, startWithLasso) {
        mutableStateOf(if (startWithLasso) SelectionBrush.LASSO else SelectionBrush.INCLUDE)
    }
    var strokes by remember { mutableStateOf<List<SelectionStroke>>(emptyList()) }
    var activeStroke by remember { mutableStateOf<SelectionStroke?>(null) }
    var mask by remember { mutableStateOf<Bitmap?>(null) }
    var feather by remember { mutableFloatStateOf(0f) }
    var expansion by remember { mutableIntStateOf(0) }
    var inverted by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var showMask by remember { mutableStateOf(true) }
    var effect by remember(source, initialEffect) { mutableStateOf(initialEffect) }
    var selectionJob by remember { mutableStateOf<Job?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectionMode by remember { mutableStateOf("Object") }
    val latestStrokes by rememberUpdatedState(strokes)

    LaunchedEffect(source) {
        engine = withContext(Dispatchers.Default) {
            runCatching { MagicTouchSelectionEngine(context, source) }
                .onFailure { error = it.message ?: "AI selection could not start" }
                .getOrNull()
        }
    }
    // Key cleanup to the workspace, not the asynchronously installed engine. Keying this effect to
    // [engine] disposed the previous effect after state assignment; its closure then observed and
    // closed the brand-new native engine before the first tap.
    DisposableEffect(source) {
        onDispose {
            selectionJob?.cancel()
            engine?.close()
        }
    }
    DisposableEffect(previewSource) {
        onDispose {
            if (previewSource !== source && !previewSource.isRecycled) previewSource.recycle()
        }
    }

    fun requestMask(next: List<SelectionStroke>) {
        selectionMode = "Object"
        strokes = next
        val activeEngine = engine ?: return
        selectionJob?.cancel()
        selectionJob = scope.launch {
            selecting = true
            runCatching { withContext(Dispatchers.Default) { activeEngine.segment(next) } }
                .onSuccess { mask = it; showMask = true }
                .onFailure { error = it.message ?: "Could not select that object" }
            selecting = false
        }
    }

    fun requestPersonMask(background: Boolean) {
        selectionJob?.cancel()
        selectionJob = scope.launch {
            selecting = true
            runCatching { withContext(Dispatchers.Default) { personSelectionMask(source) } }
                .onSuccess {
                    strokes = emptyList()
                    mask = it
                    inverted = background
                    selectionMode = if (background) "Background" else "Person"
                    showMask = true
                }
                .onFailure { error = it.message ?: "A person could not be selected" }
            selecting = false
        }
    }

    val refinedMask by produceState<Bitmap?>(mask, mask, feather, expansion, inverted) {
        val original = mask
        value = if (original == null || (feather < .5f && expansion == 0 && !inverted)) original else {
            withContext(Dispatchers.Default) { runCatching { refineSelectionMask(original, expansion, feather, inverted) }.getOrDefault(original) }
        }
    }
    val livePreview by produceState<Bitmap?>(null, previewSource, refinedMask, effect, showMask) {
        val activeMask = refinedMask
        // Always keep a replacement-cancelled effect preview under the editable mask. Toggling
        // the mask now changes only the overlay; it no longer gates the actual effect render.
        value = if (activeMask == null) null else withContext(Dispatchers.Default) {
            runCatching {
                aiRuntime.run(
                    AiOperationSpec("selection-${effect.name.lowercase()}", AiBackend.MEDIAPIPE, memoryMultiplier = 3),
                    previewSource,
                ) { applySelectionEffect(context, it, activeMask, effect, finalQuality = false) }
            }.getOrNull()
        }
    }
    val imageRect = remember(canvasSize, previewSource) {
        fittedImageRect(canvasSize, previewSource.width, previewSource.height)
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel", tint = Color.White) }
                Column {
                    Text("AI Select", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("SmartCut · on-device", color = AiSelectYellow, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                TextButton(enabled = mask != null && !applying, onClick = { showMask = !showMask }) {
                    Text(if (showMask) "Preview" else "Show mask")
                }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Object", "Person", "Background").forEach { mode ->
                        FilterChip(
                            selected = selectionMode == mode,
                            onClick = {
                                when (mode) {
                                    "Person" -> requestPersonMask(background = false)
                                    "Background" -> requestPersonMask(background = true)
                                    else -> {
                                        selectionJob?.cancel(); selectionMode = "Object"; inverted = false
                                        mask = null; strokes = emptyList(); showMask = true
                                    }
                                }
                            },
                            label = { Text(mode) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionBrush.entries.forEach { option ->
                        FilterChip(
                            selected = brush == option,
                            onClick = { brush = option },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF242427),
                                labelColor = Color.White,
                                iconColor = Color.White,
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black,
                            ),
                            leadingIcon = {
                                Icon(
                                    when (option) {
                                        SelectionBrush.INCLUDE -> Icons.Outlined.Brush
                                        SelectionBrush.REMOVE -> Icons.Outlined.RemoveCircleOutline
                                        SelectionBrush.LASSO -> Icons.Outlined.ContentCut
                                    },
                                    null,
                                    modifier = Modifier.size(17.dp),
                                )
                            },
                            label = { Text(option.label) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        enabled = strokes.isNotEmpty(),
                        onClick = {
                            val remaining = strokes.dropLast(1)
                            if (remaining.isEmpty()) {
                                selectionJob?.cancel(); strokes = emptyList(); mask = null
                            } else requestMask(remaining)
                        },
                    ) { Icon(Icons.Outlined.Undo, "Undo selection stroke", tint = Color.White) }
                    IconButton(onClick = { selectionJob?.cancel(); strokes = emptyList(); mask = null }) {
                        Icon(Icons.Outlined.LayersClear, "Clear selection", tint = Color.White)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Edge", color = VaultSecondary, modifier = Modifier.width(58.dp))
                    Slider(expansion.toFloat(), { expansion = it.toInt() }, valueRange = -20f..20f, steps = 39, modifier = Modifier.weight(1f))
                    Text(if (expansion > 0) "+$expansion" else "$expansion", color = Color.White, modifier = Modifier.width(38.dp))
                    FilterChip(inverted, { inverted = !inverted }, label = { Text("Invert") })
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Feather", color = VaultSecondary, modifier = Modifier.width(58.dp))
                    Slider(feather, { feather = it }, valueRange = 0f..30f, modifier = Modifier.weight(1f))
                    Text(feather.toInt().toString(), color = Color.White, modifier = Modifier.width(38.dp))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AiSelectionEffect.entries) { option ->
                        val selected = effect == option
                        Column(
                            Modifier.width(116.dp).clickable { effect = option; showMask = false }.border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) AiSelectYellow else Color(0xFF3C3C43),
                                shape = RoundedCornerShape(18.dp),
                            ).background(Color(0xFF25252A), RoundedCornerShape(18.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(option.icon, option.label, tint = if (selected) AiSelectYellow else Color.White)
                            Text(option.label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(option.description, color = VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
                Button(
                    enabled = mask != null && !selecting && !applying,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    onClick = {
                        val activeMask = refinedMask ?: return@Button
                        scope.launch {
                            applying = true
                            runCatching {
                                aiRuntime.run(
                                    AiOperationSpec(
                                        "selection-${effect.name.lowercase()}",
                                        if (effect == AiSelectionEffect.ERASE) AiBackend.ONNX else AiBackend.MEDIAPIPE,
                                        // LaMa operates on a bounded crop rather than duplicating the full
                                        // camera image seven times; keep the admission estimate accurate.
                                        memoryMultiplier = if (effect == AiSelectionEffect.ERASE) 3 else 4,
                                    ),
                                    source,
                                ) { applySelectionEffect(context, it, activeMask, effect, finalQuality = true) }
                            }
                                .onSuccess(onApply)
                                .onFailure { error = it.message ?: "AI edit failed" }
                            applying = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp),
                ) {
                    if (applying) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(effect.icon, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (mask == null) "Select an object first" else "Apply ${effect.label}")
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black).onSizeChanged { canvasSize = it }
                .pointerInput(engine, brush, imageRect) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val firstPoint = down.position.toNormalized(imageRect)
                        if (firstPoint == null || engine == null) return@awaitEachGesture
                        val points = mutableListOf(firstPoint)
                        activeStroke = SelectionStroke(brush, points.toList())
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val point = change.position.toNormalized(imageRect)
                            if (point != null && (points.last() - point).getDistance() > .003f) {
                                points += point
                                activeStroke = SelectionStroke(brush, points.toList())
                            }
                            if (change.positionChange() != Offset.Zero) change.consume()
                            if (!change.pressed) break
                        }
                        val completed = SelectionStroke(brush, points.toList())
                        activeStroke = null
                        requestMask(latestStrokes + completed)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                (livePreview ?: previewSource).asImageBitmap(),
                "AI edit preview",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (showMask && refinedMask != null) Image(
                refinedMask!!.asImageBitmap(),
                "Selected object mask",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color(0xB15B8CFF), BlendMode.SrcIn),
            )
            Canvas(Modifier.fillMaxSize()) {
                (strokes + listOfNotNull(activeStroke)).forEach { stroke ->
                    val screenPoints = stroke.points.map { normalized ->
                        Offset(imageRect.left + normalized.x * imageRect.width, imageRect.top + normalized.y * imageRect.height)
                    }
                    val color = when (stroke.brush) {
                        SelectionBrush.INCLUDE -> Color(0xFF66E38B)
                        SelectionBrush.REMOVE -> Color(0xFFFF6A72)
                        SelectionBrush.LASSO -> Color(0xFF61A4FF)
                    }
                    if (screenPoints.size == 1) drawCircle(color, 8.dp.toPx(), screenPoints.first())
                    else {
                        val path = Path().apply {
                            moveTo(screenPoints.first().x, screenPoints.first().y)
                            screenPoints.drop(1).forEach { lineTo(it.x, it.y) }
                            if (stroke.brush == SelectionBrush.LASSO) close()
                        }
                        drawPath(path, color, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
            if (engine == null || selecting || (!showMask && mask != null && livePreview == null)) {
                Box(Modifier.background(Color(0xAA17171B), CircleShape).padding(14.dp)) {
                    CircularProgressIndicator(color = VaultPrimary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            if (mask == null && engine != null && !selecting) {
                Text(
                    "Tap or paint over anything to select it",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
                        .background(Color(0xB819191D), RoundedCornerShape(24.dp)).padding(horizontal = 15.dp, vertical = 8.dp),
                )
            }
            error?.let { message ->
                Text(
                    message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                        .background(Color(0xDD9B272D), RoundedCornerShape(16.dp)).padding(12.dp),
                )
            }
        }
    }
}

private suspend fun applySelectionEffect(
    context: Context,
    source: Bitmap,
    mask: Bitmap,
    effect: AiSelectionEffect,
    finalQuality: Boolean,
): Bitmap = when (effect) {
    AiSelectionEffect.ERASE -> if (finalQuality) aiSelectionErase(context, source, mask) else aiSelectionErase(source, mask)
    AiSelectionEffect.BACKDROP -> aiSelectionStudioBackdrop(source, mask)
    AiSelectionEffect.DEPTH_BLUR -> aiSelectionBackgroundBlur(source, mask)
    AiSelectionEffect.RELIGHT -> aiSelectionRelight(source, mask)
    AiSelectionEffect.COLOR_POP -> aiSelectionColorPop(source, mask)
    AiSelectionEffect.STICKER -> aiSelectionSticker(source, mask)
}

internal fun refineSelectionMask(source: Bitmap, expansion: Int, feather: Float, invert: Boolean): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    val rgba = Mat()
    val gray = Mat()
    val refined = Mat()
    var kernel: Mat? = null
    return try {
        Utils.bitmapToMat(source, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.threshold(gray, refined, 127.0, 255.0, Imgproc.THRESH_BINARY)
        if (expansion != 0) {
            val radius = kotlin.math.abs(expansion).coerceIn(1, 20)
            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size((radius * 2 + 1).toDouble(), (radius * 2 + 1).toDouble()))
            if (expansion > 0) Imgproc.dilate(refined, refined, kernel) else Imgproc.erode(refined, refined, kernel)
        }
        if (feather >= .5f) {
            val radius = feather.toInt().coerceIn(1, 30)
            val size = radius * 2 + 1
            Imgproc.GaussianBlur(refined, refined, Size(size.toDouble(), size.toDouble()), 0.0)
        }
        if (invert) Core.bitwise_not(refined, refined)
        alphaMaskBitmapLike(source, refined)
    } finally {
        rgba.release(); gray.release(); refined.release(); kernel?.release()
    }
}

/**
 * Google's official MediaPipe Magic Touch v2 model. This replaces the former GrabCut heuristic:
 * taps and strokes now describe semantic objects instead of merely similar colors and edges.
 */
private class MagicTouchSelectionEngine(context: Context, source: Bitmap) {
    private val verifiedModel = verifyInteractiveSegmentationModel(context)
    private val scaledBitmap = source.scaledForAiPreview(1_024)
    private val modelBitmap = Bitmap.createBitmap(
        scaledBitmap.width,
        scaledBitmap.height,
        Bitmap.Config.ARGB_8888,
    ).also { android.graphics.Canvas(it).drawBitmap(scaledBitmap, 0f, 0f, null) }
    private val segmenter = InteractiveSegmenter.createFromOptions(
        context,
        InteractiveSegmenterOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("interactive_segmentation.task").build())
            .build(),
    ).also { it.setImage(BitmapImageBuilder(modelBitmap).build()) }

    init {
        if (scaledBitmap !== source && !scaledBitmap.isRecycled) scaledBitmap.recycle()
    }

    fun segment(strokes: List<SelectionStroke>): Bitmap {
        require(strokes.any { it.brush != SelectionBrush.REMOVE && it.points.isNotEmpty() }) {
            "Paint over an object with Add before subtracting"
        }
        val mediaPipeStrokes = strokes.filter { it.points.isNotEmpty() }.map { stroke ->
            MediaPipeStroke.builder()
                .setBrushMode(
                    when (stroke.brush) {
                        SelectionBrush.INCLUDE -> MediaPipeStroke.BrushMode.POSITIVE
                        SelectionBrush.REMOVE -> MediaPipeStroke.BrushMode.NEGATIVE
                        SelectionBrush.LASSO -> MediaPipeStroke.BrushMode.LASSO
                    },
                )
                .setPoints(stroke.points.map { NormalizedKeypoint.create(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) })
                .setCompleted(true)
                .build()
        }
        val result = segmenter.segment(mediaPipeStrokes)
        val values = FloatArray(result.width * result.height)
        ByteBufferExtractor.extract(result).asFloatBuffer().apply { rewind(); get(values) }
        val pixels = IntArray(values.size) { index ->
            AndroidColor.argb((values[index].coerceIn(0f, 1f) * 255f).toInt(), 255, 255, 255)
        }
        val semanticMask = Bitmap.createBitmap(pixels, result.width, result.height, Bitmap.Config.ARGB_8888)
        return try {
            edgeAwareSelectionMask(modelBitmap, semanticMask)
        } finally {
            semanticMask.recycle()
        }
    }

    fun close() {
        segmenter.close()
        if (!modelBitmap.isRecycled) modelBitmap.recycle()
    }
}

@Volatile private var interactiveSegmentationModelVerified = false

@Synchronized
private fun verifyInteractiveSegmentationModel(context: Context) {
    if (interactiveSegmentationModelVerified) return
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    context.assets.open("interactive_segmentation.task").buffered().use { input ->
        val buffer = ByteArray(256 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        buffer.fill(0)
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    require(actual == "38431bc66b883404e8397f74c3579404315b9b52b04a46c6346fe906a7309b03") {
        "The object-selection model failed its integrity check"
    }
    interactiveSegmentationModelVerified = true
}

private fun Bitmap.scaledForAiPreview(maxDimension: Int): Bitmap {
    val longest = maxOf(width, height).coerceAtLeast(1)
    if (longest <= maxDimension) return this
    val scale = maxDimension.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
}

private fun fittedImageRect(container: IntSize, imageWidth: Int, imageHeight: Int): Rect {
    if (container.width <= 0 || container.height <= 0 || imageWidth <= 0 || imageHeight <= 0) return Rect.Zero
    val scale = min(container.width.toFloat() / imageWidth, container.height.toFloat() / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return Rect((container.width - width) / 2f, (container.height - height) / 2f, (container.width + width) / 2f, (container.height + height) / 2f)
}

private fun Offset.toNormalized(rect: Rect): Offset? {
    if (rect == Rect.Zero || x !in rect.left..rect.right || y !in rect.top..rect.bottom) return null
    return Offset(((x - rect.left) / rect.width).coerceIn(0f, 1f), ((y - rect.top) / rect.height).coerceIn(0f, 1f))
}
