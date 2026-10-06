package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class LensCorrectionParameters(
    /** Negative values correct barrel distortion; positive values correct pincushion distortion. */
    val distortion: Float = 0f,
    val centerX: Float = .5f,
    val centerY: Float = .5f,
    val scale: Float = 1f,
)

/**
 * Optical radial-distortion correction. Preview work is downsampled and replacement-cancelled;
 * full-resolution rendering processes scanline tiles so a 50 MP source does not require a second
 * full-size coordinate map in memory.
 */
@Composable
internal fun LensCorrectionEditor(
    source: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    BackHandler(onBack = onCancel)
    val scope = rememberCoroutineScope()
    // Keep the interactive surface deliberately small enough to finish a replacement render
    // inside a display frame on a flagship phone. The full-resolution source is touched only
    // after Apply, so a moving thumb never waits on a 12/50 MP bitmap.
    val previewSource = remember(source) { boundedLensPreview(source, 720) }
    var distortion by remember { mutableFloatStateOf(0f) }
    var centerX by remember { mutableFloatStateOf(.5f) }
    var centerY by remember { mutableFloatStateOf(.5f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var preview by remember(previewSource) { mutableStateOf(previewSource) }
    var rendering by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val previewVersion = remember { AtomicInteger() }

    fun parameters() = LensCorrectionParameters(distortion, centerX, centerY, scale)

    LaunchedEffect(distortion, centerX, centerY, scale) {
        previewJob?.cancel()
        val version = previewVersion.incrementAndGet()
        previewJob = scope.launch {
            val rendered = withContext(Dispatchers.Default) { renderLensCorrection(previewSource, parameters()) }
            if (version == previewVersion.get()) preview = rendered
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel lens correction") }
                Text("Lens correction", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    distortion = 0f; centerX = .5f; centerY = .5f; scale = 1f
                }) { Icon(Icons.Outlined.RestartAlt, "Reset lens correction") }
                IconButton(enabled = !rendering, onClick = {
                    rendering = true
                    scope.launch {
                        val result = withContext(Dispatchers.Default) { renderLensCorrection(source, parameters()) }
                        rendering = false
                        onDone(result)
                    }
                }) { Icon(Icons.Outlined.Check, "Apply lens correction") }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF17171A))
                    .navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LensSlider("Distortion", distortion, -.45f..0.45f) { distortion = it }
                LensSlider("Centre X", centerX, .25f..0.75f) { centerX = it }
                LensSlider("Centre Y", centerY, .25f..0.75f) { centerY = it }
                LensSlider("Scale", scale, .8f..1.25f) { scale = it }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Image(preview.asImageBitmap(), "Lens-correction preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (rendering) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x88000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun LensSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(86.dp), style = MaterialTheme.typography.labelLarge)
        Slider(value, onValue, Modifier.weight(1f), valueRange = range)
        Text("%.2f".format(value), Modifier.width(52.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private fun boundedLensPreview(source: Bitmap, maxEdge: Int): Bitmap {
    val largest = max(source.width, source.height)
    if (largest <= maxEdge) return source
    val ratio = maxEdge.toFloat() / largest
    return Bitmap.createScaledBitmap(source, max(1, (source.width * ratio).toInt()), max(1, (source.height * ratio).toInt()), true)
}

/** Visible for deterministic pixel-equivalence and memory-bound tests. */
internal fun renderLensCorrection(source: Bitmap, parameters: LensCorrectionParameters): Bitmap {
    if (parameters.distortion == 0f && parameters.centerX == .5f && parameters.centerY == .5f && parameters.scale == 1f) {
        return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
    }
    val width = source.width
    val height = source.height
    val output = createEditBitmapLike(source, width, height)
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val tileHeight = max(1, min(96, 4_000_000 / max(1, width)))
    val aspect = width.toFloat() / max(1, height)
    val cx = parameters.centerX * (width - 1)
    val cy = parameters.centerY * (height - 1)
    val k = parameters.distortion
    val zoom = parameters.scale.coerceIn(.8f, 1.25f)
    var top = 0
    while (top < height) {
        val rows = min(tileHeight, height - top)
        val tile = IntArray(width * rows)
        for (localY in 0 until rows) {
            val y = top + localY
            val ny = ((y - cy) / max(1f, height / 2f)) / zoom
            for (x in 0 until width) {
                val nx = ((x - cx) / max(1f, width / 2f)) * aspect / zoom
                val radius2 = nx * nx + ny * ny
                val radial = 1f + k * radius2 + (k * .35f) * radius2 * radius2
                val sx = cx + (nx * radial / aspect) * (width / 2f)
                val sy = cy + (ny * radial) * (height / 2f)
                tile[localY * width + x] = bilinearSample(sourcePixels, width, height, sx, sy)
            }
        }
        output.setPixels(tile, 0, width, 0, top, width, rows)
        top += rows
    }
    return output
}

private fun bilinearSample(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
    if (x < 0f || y < 0f || x > width - 1f || y > height - 1f) return Color.TRANSPARENT
    val x0 = floor(x).toInt().coerceIn(0, width - 1)
    val y0 = floor(y).toInt().coerceIn(0, height - 1)
    val x1 = min(x0 + 1, width - 1)
    val y1 = min(y0 + 1, height - 1)
    val fx = x - x0
    val fy = y - y0
    fun channel(shift: Int): Int {
        val a = (pixels[y0 * width + x0] ushr shift) and 0xFF
        val b = (pixels[y0 * width + x1] ushr shift) and 0xFF
        val c = (pixels[y1 * width + x0] ushr shift) and 0xFF
        val d = (pixels[y1 * width + x1] ushr shift) and 0xFF
        return ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy).toInt().coerceIn(0, 255)
    }
    return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
