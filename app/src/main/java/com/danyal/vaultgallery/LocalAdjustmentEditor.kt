package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.pow

internal enum class LocalMaskMode(val label: String) { LINEAR("Gradient"), RADIAL("Radial"), BRUSH("Brush") }
internal enum class LocalAdjustmentKind(val label: String) { EXPOSURE("Exposure"), SATURATION("Saturation"), WARMTH("Warmth") }

internal data class LocalAdjustmentSpec(
    val mode: LocalMaskMode,
    val kind: LocalAdjustmentKind,
    val amount: Float,
    val feather: Float,
    val inverted: Boolean,
    val centerX: Float,
    val centerY: Float,
    val endX: Float,
    val endY: Float,
    val radius: Float,
    val brushSize: Float,
    val brushPoints: List<Pair<Float, Float>>,
)

@Composable
internal fun LocalAdjustmentEditor(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val scope = rememberCoroutineScope()
    val small = remember(source) { source.scaledEditorPreview(720) }
    var mode by remember { mutableStateOf(LocalMaskMode.LINEAR) }
    var kind by remember { mutableStateOf(LocalAdjustmentKind.EXPOSURE) }
    var amount by remember { mutableFloatStateOf(.35f) }
    var feather by remember { mutableFloatStateOf(.45f) }
    var inverted by remember { mutableStateOf(false) }
    var center by remember { mutableStateOf(Offset(.5f, .35f)) }
    var end by remember { mutableStateOf(Offset(.5f, .75f)) }
    var radius by remember { mutableFloatStateOf(.32f) }
    var brushSize by remember { mutableFloatStateOf(.12f) }
    var brushPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val latestBrushPoints by rememberUpdatedState(brushPoints)

    fun spec() = LocalAdjustmentSpec(
        mode, kind, amount, feather, inverted, center.x, center.y, end.x, end.y,
        radius, brushSize, brushPoints,
    )
    val preview by produceState(small, mode, kind, amount, feather, inverted, center, end, radius, brushSize, brushPoints) {
        value = withContext(Dispatchers.Default) { runCatching { renderLocalAdjustment(small, spec()) }.getOrElse { small } }
    }
    BackHandler(onBack = onCancel)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel", tint = androidx.compose.ui.graphics.Color.White) }
                Text("Local adjustments", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !applying, onClick = {
                    scope.launch {
                        applying = true
                        runCatching { withContext(Dispatchers.Default) { renderLocalAdjustment(source, spec()) } }
                            .onSuccess(onDone).onFailure { error = it.message ?: "Local adjustment failed" }
                        applying = false
                    }
                }) { Text("Done") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black).navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LocalMaskMode.entries.forEach { choice -> FilterChip(mode == choice, { mode = choice }, label = { Text(choice.label) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LocalAdjustmentKind.entries.forEach { choice -> FilterChip(kind == choice, { kind = choice }, label = { Text(choice.label) }) }
                }
                Text("Amount ${"%+.0f".format(amount * 100)}", color = androidx.compose.ui.graphics.Color.White)
                Slider(amount, { amount = it }, valueRange = -1f..1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (mode == LocalMaskMode.BRUSH) "Brush size" else if (mode == LocalMaskMode.RADIAL) "Radius" else "Feather", color = VaultSecondary, modifier = Modifier.weight(1f))
                    FilterChip(inverted, { inverted = !inverted }, label = { Text("Invert") })
                    if (mode == LocalMaskMode.BRUSH) TextButton(onClick = { brushPoints = emptyList() }) { Text("Clear") }
                }
                Slider(
                    value = if (mode == LocalMaskMode.BRUSH) brushSize else if (mode == LocalMaskMode.RADIAL) radius else feather,
                    onValueChange = { if (mode == LocalMaskMode.BRUSH) brushSize = it else if (mode == LocalMaskMode.RADIAL) radius = it else feather = it },
                    valueRange = if (mode == LocalMaskMode.BRUSH) .02f..0.4f else .05f..0.8f,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).onSizeChanged { canvasSize = it }.pointerInput(mode, canvasSize) {
                detectDragGestures(
                    onDragStart = { position ->
                        val normalized = Offset(position.x / size.width, position.y / size.height)
                        when (mode) {
                            LocalMaskMode.BRUSH -> brushPoints = latestBrushPoints + (normalized.x to normalized.y)
                            LocalMaskMode.RADIAL -> center = normalized
                            LocalMaskMode.LINEAR -> center = normalized
                        }
                    },
                    onDrag = { change, _ ->
                        val normalized = Offset((change.position.x / size.width).coerceIn(0f, 1f), (change.position.y / size.height).coerceIn(0f, 1f))
                        when (mode) {
                            LocalMaskMode.BRUSH -> brushPoints = (latestBrushPoints + (normalized.x to normalized.y)).takeLast(2_000)
                            LocalMaskMode.RADIAL -> center = normalized
                            LocalMaskMode.LINEAR -> end = normalized
                        }
                    },
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Image(preview.asImageBitmap(), "Local adjustment preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            // The effect render is replacement-cancelled in the background, while this vector
            // guide is drawn synchronously with the pointer. The user therefore never loses
            // direct visual contact with the active mask, even on a costly brush update.
            ComposeCanvas(Modifier.fillMaxSize()) {
                val guide = ComposeColor(0xD9FFD60A)
                when (mode) {
                    LocalMaskMode.BRUSH -> {
                        val points = brushPoints.map { Offset(it.first * size.width, it.second * size.height) }
                        if (points.size == 1) drawCircle(guide, brushSize * minOf(size.width, size.height), points.first())
                        points.zipWithNext().forEach { (a, b) ->
                            drawLine(guide, a, b, strokeWidth = brushSize * minOf(size.width, size.height) * 2f)
                        }
                    }
                    LocalMaskMode.RADIAL -> {
                        val point = Offset(center.x * size.width, center.y * size.height)
                        drawCircle(guide, radius * minOf(size.width, size.height), point, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        drawCircle(guide, 5.dp.toPx(), point)
                    }
                    LocalMaskMode.LINEAR -> {
                        val start = Offset(center.x * size.width, center.y * size.height)
                        val finish = Offset(end.x * size.width, end.y * size.height)
                        drawLine(guide, start, finish, 2.dp.toPx())
                        drawCircle(guide, 5.dp.toPx(), start)
                        drawCircle(guide, 5.dp.toPx(), finish)
                    }
                }
            }
            if (applying) CircularProgressIndicator()
            error?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)) }
        }
    }
}

internal fun renderLocalAdjustment(source: Bitmap, spec: LocalAdjustmentSpec): Bitmap {
    val width = source.width
    val height = source.height
    val mask = when (spec.mode) {
        LocalMaskMode.BRUSH -> brushMask(width, height, spec)
        else -> FloatArray(width * height) { index ->
            val x = (index % width).toFloat() / width.coerceAtLeast(1)
            val y = (index / width).toFloat() / height.coerceAtLeast(1)
            val raw = when (spec.mode) {
                LocalMaskMode.RADIAL -> {
                    val distance = hypot(x - spec.centerX, y - spec.centerY) / spec.radius.coerceAtLeast(.01f)
                    smoothStep(1f, (1f - spec.feather).coerceIn(0f, .99f), distance)
                }
                LocalMaskMode.LINEAR -> {
                    val dx = spec.endX - spec.centerX
                    val dy = spec.endY - spec.centerY
                    val length = hypot(dx, dy).coerceAtLeast(.01f)
                    val projection = ((x - spec.centerX) * dx + (y - spec.centerY) * dy) / (length * length)
                    smoothStep(0f, spec.feather.coerceAtLeast(.05f), projection)
                }
                LocalMaskMode.BRUSH -> 0f
            }
            if (spec.inverted) 1f - raw else raw
        }
    }
    val input = IntArray(width * height)
    val output = IntArray(width * height)
    source.getPixels(input, 0, width, 0, 0, width, height)
    for (index in input.indices) {
        val pixel = input[index]
        val alpha = mask[index].coerceIn(0f, 1f)
        var r = Color.red(pixel).toFloat(); var g = Color.green(pixel).toFloat(); var b = Color.blue(pixel).toFloat()
        when (spec.kind) {
            LocalAdjustmentKind.EXPOSURE -> {
                val multiplier = 2.0.pow(spec.amount.toDouble()).toFloat()
                r *= multiplier; g *= multiplier; b *= multiplier
            }
            LocalAdjustmentKind.SATURATION -> {
                val luma = r * .2126f + g * .7152f + b * .0722f
                val saturation = 1f + spec.amount
                r = luma + (r - luma) * saturation; g = luma + (g - luma) * saturation; b = luma + (b - luma) * saturation
            }
            LocalAdjustmentKind.WARMTH -> { r += 42f * spec.amount; b -= 42f * spec.amount }
        }
        fun mix(original: Int, adjusted: Float) = (original + (adjusted.coerceIn(0f, 255f) - original) * alpha).toInt().coerceIn(0, 255)
        output[index] = Color.argb(Color.alpha(pixel), mix(Color.red(pixel), r), mix(Color.green(pixel), g), mix(Color.blue(pixel), b))
    }
    return createEditBitmapFromPixels(source, output, width, height)
}

private fun brushMask(width: Int, height: Int, spec: LocalAdjustmentSpec): FloatArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    val canvas = Canvas(bitmap)
    val path = Path()
    spec.brushPoints.firstOrNull()?.let { path.moveTo(it.first * width, it.second * height) }
    spec.brushPoints.drop(1).forEach { path.lineTo(it.first * width, it.second * height) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = spec.brushSize * minOf(width, height)
        maskFilter = BlurMaskFilter((spec.feather * strokeWidth).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawPath(path, paint)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()
    return FloatArray(pixels.size) { index ->
        val alpha = Color.alpha(pixels[index]) / 255f
        if (spec.inverted) 1f - alpha else alpha
    }
}

private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    if (edge0 == edge1) return if (value < edge0) 0f else 1f
    val x = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
