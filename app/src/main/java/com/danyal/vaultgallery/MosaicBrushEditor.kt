package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

internal enum class MosaicBrushStyle(val label: String) { PIXELATE("Pixelate"), DOTS("Dots"), BARS("Bars") }

private data class MosaicStroke(val points: List<Pair<Float, Float>>, val radius: Float, val style: MosaicBrushStyle)

/** Full-resolution destructive mosaic/pattern brush. Strokes are stored as normalized coordinates,
 * so the exported mask exactly follows the fitted preview on every screen size. */
internal class MosaicBrushView(context: android.content.Context) : View(context) {
    private var source: Bitmap? = null
    private var pixelated: Bitmap? = null
    private val strokes = mutableListOf<MosaicStroke>()
    private val redo = mutableListOf<MosaicStroke>()
    private val active = mutableListOf<Pair<Float, Float>>()
    var radiusFraction: Float = .035f
    var style: MosaicBrushStyle = MosaicBrushStyle.PIXELATE
    var onHistoryChanged: ((Boolean, Boolean) -> Unit)? = null

    fun setSource(bitmap: Bitmap) {
        source = bitmap
        pixelated?.takeUnless { it === bitmap }?.recycle()
        pixelated = makePixelated(bitmap)
        strokes.clear(); redo.clear(); active.clear()
        notifyHistory(); invalidate()
    }

    fun undo() { if (strokes.isNotEmpty()) redo += strokes.removeAt(strokes.lastIndex); notifyHistory(); invalidate() }
    fun redo() { if (redo.isNotEmpty()) strokes += redo.removeAt(redo.lastIndex); notifyHistory(); invalidate() }
    private fun notifyHistory() = onHistoryChanged?.invoke(strokes.isNotEmpty(), redo.isNotEmpty())

    private fun imageRect(bitmap: Bitmap): RectF {
        val scale = min(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        return RectF((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = source ?: return
        val effect = pixelated ?: return
        val destination = imageRect(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destination, paint)
        drawStrokes(canvas, effect, destination, strokes + active.takeIf { it.size > 1 }?.let { listOf(MosaicStroke(it.toList(), radiusFraction, style)) }.orEmpty())
    }

    private fun drawStrokes(canvas: Canvas, effect: Bitmap, destination: RectF, all: List<MosaicStroke>) {
        all.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                val x = destination.left + point.first * destination.width()
                val y = destination.top + point.second * destination.height()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val widthPx = stroke.radius * min(destination.width(), destination.height()) * 2f
            canvas.save()
            canvas.clipPath(Path().apply {
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = widthPx; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
                // Convert the stroked centerline to a filled path for clipping.
                maskPaint.getFillPath(path, this)
            })
            canvas.drawBitmap(effect, null, destination, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            drawPattern(canvas, destination, stroke.style, widthPx)
            canvas.restore()
        }
    }

    private fun drawPattern(canvas: Canvas, destination: RectF, style: MosaicBrushStyle, stepHint: Float) {
        when (style) {
            MosaicBrushStyle.PIXELATE -> Unit
            MosaicBrushStyle.DOTS -> {
                val step = max(12f, stepHint / 3f)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFFFFF.toInt() }
                var y = destination.top
                while (y <= destination.bottom) {
                    var x = destination.left
                    while (x <= destination.right) { canvas.drawCircle(x, y, step * .16f, paint); x += step }
                    y += step
                }
            }
            MosaicBrushStyle.BARS -> {
                val step = max(14f, stepHint / 2.5f)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 }
                var x = destination.left - destination.height()
                while (x < destination.right) {
                    canvas.drawLine(x, destination.bottom, x + destination.height(), destination.top, paint.apply { strokeWidth = step * .22f })
                    x += step
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bitmap = source ?: return false
        val rect = imageRect(bitmap)
        fun point(): Pair<Float, Float> = ((event.x - rect.left) / rect.width()).coerceIn(0f, 1f) to
            ((event.y - rect.top) / rect.height()).coerceIn(0f, 1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { active.clear(); active += point(); parent?.requestDisallowInterceptTouchEvent(true); invalidate(); return true }
            MotionEvent.ACTION_MOVE -> { active += point(); invalidate(); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active.isNotEmpty()) strokes += MosaicStroke(active.toList(), radiusFraction, style)
                active.clear(); redo.clear(); parent?.requestDisallowInterceptTouchEvent(false); notifyHistory(); invalidate()
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun render(): Bitmap {
        val bitmap = requireNotNull(source)
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val effect = makePixelated(bitmap)
        try {
            val destination = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
            drawStrokes(Canvas(output), effect, destination, strokes)
        } finally { effect.recycle() }
        return output
    }

    private fun makePixelated(bitmap: Bitmap): Bitmap {
        val shortSide = min(bitmap.width, bitmap.height)
        val block = (shortSide / 90).coerceIn(8, 64)
        val tiny = Bitmap.createScaledBitmap(bitmap, max(1, bitmap.width / block), max(1, bitmap.height / block), false)
        return Bitmap.createScaledBitmap(tiny, bitmap.width, bitmap.height, false).also { tiny.recycle() }
    }

    override fun onDetachedFromWindow() {
        pixelated?.takeUnless { it === source }?.recycle()
        pixelated = null
        super.onDetachedFromWindow()
    }
}

@Composable
internal fun MosaicBrushEditor(
    source: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<MosaicBrushView?>(null) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var size by remember { mutableFloatStateOf(.035f) }
    var style by remember { mutableStateOf(MosaicBrushStyle.PIXELATE) }
    var rendering by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    Column(Modifier.fillMaxSize().background(ComposeColor.Black)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel mosaic", tint = ComposeColor.White) }
            Text("Mosaic & patterns", color = ComposeColor.White, modifier = Modifier.weight(1f))
            IconButton(enabled = canUndo, onClick = { view?.undo() }) { Icon(Icons.Outlined.Undo, "Undo", tint = if (canUndo) ComposeColor.White else VaultSecondary) }
            IconButton(enabled = canRedo, onClick = { view?.redo() }) { Icon(Icons.Outlined.Redo, "Redo", tint = if (canRedo) ComposeColor.White else VaultSecondary) }
            TextButton(enabled = canUndo && !rendering, onClick = {
                val active = view ?: return@TextButton
                scope.launch {
                    rendering = true
                    val output = withContext(Dispatchers.Default) { active.render() }
                    rendering = false
                    onDone(output)
                }
            }) { Text(if (rendering) "Rendering…" else "Done") }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context -> MosaicBrushView(context).also { created ->
                    created.setSource(source)
                    created.radiusFraction = size
                    created.style = style
                    created.onHistoryChanged = { undo, redo -> canUndo = undo; canRedo = redo }
                    view = created
                } },
                update = { it.radiusFraction = size; it.style = style },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.fillMaxWidth().background(ComposeColor.Black).navigationBarsPadding().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MosaicBrushStyle.entries.forEach { choice ->
                    FilterChip(selected = style == choice, onClick = { style = choice }, label = { Text(choice.label) })
                }
            }
            Text("Brush size ${(size * 200).toInt()}%", color = ComposeColor.White)
            Slider(value = size, onValueChange = { size = it }, valueRange = .012f.. .12f)
            Text("Paint directly over faces, text, number plates, or any region to conceal it.", color = VaultSecondary)
        }
    }
}
