package com.danyal.vaultgallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

internal data class CurvePoint(val x: Float, val y: Float)
internal enum class CurveChannel(val label: String, val colour: Color) {
    RGB("RGB", Color.White), RED("Red", Color(0xFFFF5A5F)), GREEN("Green", Color(0xFF43D17A)), BLUE("Blue", Color(0xFF4D8DFF)),
}

internal fun normalizedCurve(points: List<CurvePoint>): List<CurvePoint> {
    val middle = points.asSequence().map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
        .filter { it.x > 0f && it.x < 1f }.sortedBy(CurvePoint::x).fold(ArrayList<CurvePoint>()) { result, point ->
            if (result.lastOrNull()?.let { abs(it.x - point.x) < .01f } == true) result[result.lastIndex] = point else result += point
            result
        }
    return listOf(CurvePoint(0f, points.firstOrNull { it.x <= .001f }?.y?.coerceIn(0f, 1f) ?: 0f)) +
        middle + CurvePoint(1f, points.lastOrNull { it.x >= .999f }?.y?.coerceIn(0f, 1f) ?: 1f)
}

internal fun curveLut(points: List<CurvePoint>): IntArray {
    val curve = normalizedCurve(points)
    return IntArray(256) { input ->
        val x = input / 255f
        val upper = curve.indexOfFirst { it.x >= x }.let { if (it < 0) curve.lastIndex else it }
        val lower = (upper - 1).coerceAtLeast(0)
        val a = curve[lower]
        val b = curve[upper]
        val fraction = if (b.x <= a.x) 0f else ((x - a.x) / (b.x - a.x)).coerceIn(0f, 1f)
        ((a.y + (b.y - a.y) * fraction) * 255f).toInt().coerceIn(0, 255)
    }
}

internal fun applyCurves(source: Bitmap, curves: Map<CurveChannel, List<CurvePoint>>): Bitmap {
    val rgb = curveLut(curves[CurveChannel.RGB].orEmpty())
    val red = curveLut(curves[CurveChannel.RED].orEmpty())
    val green = curveLut(curves[CurveChannel.GREEN].orEmpty())
    val blue = curveLut(curves[CurveChannel.BLUE].orEmpty())
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    pixels.indices.forEach { index ->
        val colour = pixels[index]
        val a = colour ushr 24
        val r = rgb[red[(colour ushr 16) and 0xff]]
        val g = rgb[green[(colour ushr 8) and 0xff]]
        val b = rgb[blue[colour and 0xff]]
        pixels[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return createEditBitmapFromPixels(source, pixels, source.width, source.height)
}

@Composable
internal fun CurvesEditor(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val identity = remember { listOf(CurvePoint(0f, 0f), CurvePoint(.25f, .25f), CurvePoint(.5f, .5f), CurvePoint(.75f, .75f), CurvePoint(1f, 1f)) }
    var curves by remember { mutableStateOf(CurveChannel.entries.associateWith { identity }) }
    var channel by remember { mutableStateOf(CurveChannel.RGB) }
    var selectedIndex by remember { mutableIntStateOf(2) }
    var preview by remember(source) { mutableStateOf(source) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val latestCurves by rememberUpdatedState(curves)
    BackHandler(enabled = !applying, onBack = onCancel)
    LaunchedEffect(curves) {
        val longest = max(source.width, source.height)
        // A bounded interaction texture keeps point dragging live. Apply still renders from the
        // original bitmap, so reducing this surface does not reduce saved quality.
        val scale = minOf(1f, 720f / longest.coerceAtLeast(1))
        val sample = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true) else source
        val changed = withContext(Dispatchers.Default) { applyCurves(sample, curves) }
        if (preview !== source) preview.recycle()
        if (sample !== source) sample.recycle()
        preview = changed
    }
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !applying, onClick = onCancel) { Text("Cancel") }
            Text("Curves", color = Color.White, modifier = Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Button(enabled = !applying, onClick = {
                applying = true
                error = null
            }) { Text("Apply") }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(preview.asImageBitmap(), "Curves preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (applying) CircularProgressIndicator()
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CurveChannel.entries.forEach { item -> FilterChip(channel == item, { channel = item; selectedIndex = 2 }, label = { Text(item.label) }) }
        }
        val points = curves[channel].orEmpty()
        Canvas(
            Modifier.fillMaxWidth().height(220.dp).padding(16.dp).background(Color(0xFF17171B), RoundedCornerShape(12.dp))
                .pointerInput(channel) {
                    var activeIndex = selectedIndex
                    detectDragGestures(
                        onDragStart = { position ->
                            val livePoints = latestCurves[channel].orEmpty()
                            val x = (position.x / size.width).coerceIn(0f, 1f)
                            val y = (1f - position.y / size.height).coerceIn(0f, 1f)
                            val nearest = livePoints.indices.minByOrNull { abs(livePoints[it].x - x) + abs(livePoints[it].y - y) } ?: 0
                            val updated = if (abs(livePoints[nearest].x - x) + abs(livePoints[nearest].y - y) < .15f) livePoints else normalizedCurve(livePoints + CurvePoint(x, y))
                            activeIndex = updated.indices.minByOrNull { abs(updated[it].x - x) + abs(updated[it].y - y) } ?: 0
                            selectedIndex = activeIndex
                            curves = latestCurves + (channel to updated)
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            val current = latestCurves[channel].orEmpty().toMutableList()
                            if (activeIndex !in current.indices) return@detectDragGestures
                            val old = current[activeIndex]
                            val minX = current.getOrNull(activeIndex - 1)?.x?.plus(.01f) ?: 0f
                            val maxX = current.getOrNull(activeIndex + 1)?.x?.minus(.01f) ?: 1f
                            current[activeIndex] = CurvePoint(
                                if (activeIndex == 0 || activeIndex == current.lastIndex) old.x else (old.x + drag.x / size.width).coerceIn(minX, maxX),
                                (old.y - drag.y / size.height).coerceIn(0f, 1f),
                            )
                            curves = latestCurves + (channel to current)
                        },
                    )
                },
        ) {
            repeat(5) { step ->
                val fraction = step / 4f
                drawLine(Color(0xFF3A3A40), Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height))
                drawLine(Color(0xFF3A3A40), Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction))
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                val offset = Offset(point.x * size.width, (1f - point.y) * size.height)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, channel.colour, style = Stroke(4.dp.toPx()))
            points.forEachIndexed { index, point ->
                drawCircle(if (index == selectedIndex) Color.White else channel.colour, if (index == selectedIndex) 9.dp.toPx() else 6.dp.toPx(), Offset(point.x * size.width, (1f - point.y) * size.height))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = points.size > 2 && selectedIndex !in listOf(0, points.lastIndex), onClick = {
                curves = curves + (channel to points.filterIndexed { index, _ -> index != selectedIndex })
                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
            }) { Text("Remove point") }
            TextButton(onClick = { curves = curves + (channel to identity); selectedIndex = 2 }) { Text("Reset channel") }
        }
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
    }
    LaunchedEffect(applying) {
        if (applying) runCatching { withContext(Dispatchers.Default) { applyCurves(source, curves) } }
            .onSuccess(onDone).onFailure { error = it.message ?: "Could not apply curves"; applying = false }
    }
}
