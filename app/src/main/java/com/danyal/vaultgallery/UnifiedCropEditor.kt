package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private data class CropRatio(val label: String, val value: Float?)
private enum class CropOverlay(val label: String) { THIRDS("Thirds"), DIAGONAL("Diagonal"), GOLDEN("Golden") }
internal enum class CropDrag { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

private val standardCropRatios = listOf(
    CropRatio("Free", null), CropRatio("Original", -1f), CropRatio("1:1", 1f), CropRatio("3:4", 3f / 4f),
    CropRatio("4:3", 4f / 3f), CropRatio("9:16", 9f / 16f), CropRatio("16:9", 16f / 9f),
    CropRatio("Full portrait", 9f / 19.5f), CropRatio("Full landscape", 19.5f / 9f), CropRatio("Custom", Float.NaN),
)

@Composable
internal fun UnifiedCropEditor(
    source: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    var bitmap by remember(source) { mutableStateOf(source) }
    var crop by remember(bitmap) { mutableStateOf(Rect(.05f, .05f, .95f, .95f)) }
    var ratio by remember(bitmap) { mutableStateOf<CropRatio?>(standardCropRatios[0]) }
    var overlay by remember { mutableStateOf(CropOverlay.THIRDS) }
    var customDialog by remember { mutableStateOf(false) }
    var customWidth by remember { mutableStateOf("4") }
    var customHeight by remember { mutableStateOf("5") }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val latestCrop by rememberUpdatedState(crop)
    val latestRatio by rememberUpdatedState(ratio)

    fun applyRatio(selected: CropRatio, customValue: Float? = null) {
        val actual = when {
            customValue != null -> customValue
            selected.value == -1f -> bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
            else -> selected.value
        }
        ratio = selected.copy(value = actual)
        if (actual == null || actual.isNaN()) return
        val normalizedRatio = actual * bitmap.height / bitmap.width.toFloat()
        val maxWidth = .9f
        val maxHeight = .9f
        val width = min(maxWidth, maxHeight * normalizedRatio)
        val height = width / normalizedRatio
        crop = Rect(.5f - width / 2f, .5f - height / 2f, .5f + width / 2f, .5f + height / 2f)
    }

    fun transform(rotation: Float = 0f, flipX: Boolean = false, flipY: Boolean = false) {
        val matrix = Matrix().apply {
            if (flipX || flipY) postScale(if (flipX) -1f else 1f, if (flipY) -1f else 1f)
            if (rotation != 0f) postRotate(rotation)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        crop = Rect(.05f, .05f, .95f, .95f)
        ratio = standardCropRatios[0]
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel crop", tint = Color.White) }
                Text("Crop", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { crop = Rect(.05f, .05f, .95f, .95f); ratio = standardCropRatios[0] }) { Text("Reset") }
                TextButton(onClick = {
                    val left = (crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val top = (crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                    val width = ((crop.width * bitmap.width).toInt()).coerceIn(1, bitmap.width - left)
                    val height = ((crop.height * bitmap.height).toInt()).coerceIn(1, bitmap.height - top)
                    onDone(Bitmap.createBitmap(bitmap, left, top, width, height))
                }) { Text("Done") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { transform(rotation = -90f) }) { Icon(Icons.Outlined.Rotate90DegreesCcw, "Rotate left", tint = Color.White) }
                    IconButton(onClick = { transform(rotation = 90f) }) { Icon(Icons.Outlined.Rotate90DegreesCcw, "Rotate right", tint = Color.White, modifier = Modifier.graphicsLayer(rotationZ = 180f)) }
                    IconButton(onClick = { transform(flipX = true) }) { Icon(Icons.Outlined.Flip, "Flip horizontally", tint = Color.White) }
                    IconButton(onClick = { transform(flipY = true) }) { Icon(Icons.Outlined.Flip, "Flip vertically", tint = Color.White, modifier = Modifier.graphicsLayer(rotationZ = 90f)) }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                ) {
                    items(CropOverlay.entries) { item ->
                        FilterChip(
                            selected = overlay == item,
                            onClick = { overlay = item },
                            label = { Text(item.label, maxLines = 1) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    items(standardCropRatios) { item ->
                        FilterChip(
                            selected = ratio?.label == item.label,
                            onClick = { if (item.value?.isNaN() == true) customDialog = true else applyRatio(item) },
                            label = { Text(item.label) },
                        )
                    }
                }
                Text("Drag corners or move inside the frame", color = VaultSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Canvas(
                // Do not key this input handler with crop. Doing so cancels and recreates the
                // gesture after every movement, which is exactly the "nothing moves until I lift"
                // failure. Updated-state references let one uninterrupted gesture own all frames.
                Modifier.fillMaxSize().onSizeChanged { canvasSize = it }.pointerInput(bitmap, canvasSize) {
                    var drag: CropDrag? = null
                    detectDragGestures(
                        onDragStart = { point -> drag = cropHitTarget(point, latestCrop, canvasSize, bitmap.width, bitmap.height) },
                        onDragEnd = { drag = null },
                        onDragCancel = { drag = null },
                    ) { change, amount ->
                        change.consume()
                        crop = updateCropRect(latestCrop, drag ?: CropDrag.MOVE, amount, canvasSize, bitmap.width, bitmap.height, latestRatio?.value)
                    }
                },
            ) {
                val fitted = fittedImageRect(size.width, size.height, bitmap.width, bitmap.height)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(bitmap, null, android.graphics.RectF(fitted.left, fitted.top, fitted.right, fitted.bottom), android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
                }
                val frame = Rect(
                    fitted.left + crop.left * fitted.width,
                    fitted.top + crop.top * fitted.height,
                    fitted.left + crop.right * fitted.width,
                    fitted.top + crop.bottom * fitted.height,
                )
                val shade = Color(0xA8000000)
                drawRect(shade, topLeft = fitted.topLeft, size = androidx.compose.ui.geometry.Size(fitted.width, frame.top - fitted.top))
                drawRect(shade, topLeft = Offset(fitted.left, frame.bottom), size = androidx.compose.ui.geometry.Size(fitted.width, fitted.bottom - frame.bottom))
                drawRect(shade, topLeft = Offset(fitted.left, frame.top), size = androidx.compose.ui.geometry.Size(frame.left - fitted.left, frame.height))
                drawRect(shade, topLeft = Offset(frame.right, frame.top), size = androidx.compose.ui.geometry.Size(fitted.right - frame.right, frame.height))
                drawRect(Color.White, topLeft = frame.topLeft, size = frame.size, style = Stroke(2.dp.toPx()))
                drawCropOverlay(frame, overlay)
                listOf(frame.topLeft, Offset(frame.right, frame.top), Offset(frame.left, frame.bottom), Offset(frame.right, frame.bottom)).forEach { point ->
                    drawCircle(Color.White, 7.dp.toPx(), point)
                    drawCircle(Color.Black, 3.dp.toPx(), point)
                }
            }
        }
    }

    if (customDialog) AlertDialog(
        onDismissRequest = { customDialog = false },
        title = { Text("Custom aspect ratio") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(customWidth, { customWidth = it.filter(Char::isDigit).take(4) }, label = { Text("Width") }, modifier = Modifier.weight(1f))
                Text(" : ", modifier = Modifier.padding(horizontal = 6.dp))
                OutlinedTextField(customHeight, { customHeight = it.filter(Char::isDigit).take(4) }, label = { Text("Height") }, modifier = Modifier.weight(1f))
            }
        },
        dismissButton = { TextButton(onClick = { customDialog = false }) { Text("Cancel") } },
        confirmButton = { TextButton(
            enabled = customWidth.toFloatOrNull()?.let { it > 0f } == true && customHeight.toFloatOrNull()?.let { it > 0f } == true,
            onClick = {
                val value = customWidth.toFloat() / customHeight.toFloat()
                applyRatio(CropRatio("${customWidth}:${customHeight}", value), value)
                customDialog = false
            },
        ) { Text("Apply") } },
    )
}

private fun fittedImageRect(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int): Rect {
    if (canvasWidth <= 0f || canvasHeight <= 0f) return Rect.Zero
    val scale = min(canvasWidth / imageWidth.coerceAtLeast(1), canvasHeight / imageHeight.coerceAtLeast(1))
    val width = imageWidth * scale
    val height = imageHeight * scale
    return Rect((canvasWidth - width) / 2f, (canvasHeight - height) / 2f, (canvasWidth + width) / 2f, (canvasHeight + height) / 2f)
}

private fun cropHitTarget(point: Offset, crop: Rect, size: IntSize, imageWidth: Int, imageHeight: Int): CropDrag {
    val fitted = fittedImageRect(size.width.toFloat(), size.height.toFloat(), imageWidth, imageHeight)
    val points = listOf(
        CropDrag.TOP_LEFT to Offset(fitted.left + crop.left * fitted.width, fitted.top + crop.top * fitted.height),
        CropDrag.TOP_RIGHT to Offset(fitted.left + crop.right * fitted.width, fitted.top + crop.top * fitted.height),
        CropDrag.BOTTOM_LEFT to Offset(fitted.left + crop.left * fitted.width, fitted.top + crop.bottom * fitted.height),
        CropDrag.BOTTOM_RIGHT to Offset(fitted.left + crop.right * fitted.width, fitted.top + crop.bottom * fitted.height),
    )
    return points.minByOrNull { (_, handle) -> (point - handle).getDistance() }
        ?.takeIf { (_, handle) -> (point - handle).getDistance() <= 56f }?.first ?: CropDrag.MOVE
}

internal fun updateCropRect(
    original: Rect,
    drag: CropDrag,
    delta: Offset,
    size: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    lockedRatio: Float?,
): Rect {
    val fitted = fittedImageRect(size.width.toFloat(), size.height.toFloat(), imageWidth, imageHeight)
    if (fitted.width <= 0f || fitted.height <= 0f) return original
    val dx = delta.x / fitted.width
    val dy = delta.y / fitted.height
    if (drag == CropDrag.MOVE) {
        val moveX = dx.coerceIn(-original.left, 1f - original.right)
        val moveY = dy.coerceIn(-original.top, 1f - original.bottom)
        return original.translate(Offset(moveX, moveY))
    }
    var left = original.left
    var top = original.top
    var right = original.right
    var bottom = original.bottom
    when (drag) {
        CropDrag.TOP_LEFT -> { left += dx; top += dy }
        CropDrag.TOP_RIGHT -> { right += dx; top += dy }
        CropDrag.BOTTOM_LEFT -> { left += dx; bottom += dy }
        CropDrag.BOTTOM_RIGHT -> { right += dx; bottom += dy }
        CropDrag.MOVE -> Unit
    }
    left = left.coerceIn(0f, right - .05f)
    right = right.coerceIn(left + .05f, 1f)
    top = top.coerceIn(0f, bottom - .05f)
    bottom = bottom.coerceIn(top + .05f, 1f)
    if (lockedRatio != null && lockedRatio.isFinite() && lockedRatio > 0f) {
        val normalizedRatio = lockedRatio * imageHeight / imageWidth.toFloat()
        val anchorX = if (drag == CropDrag.TOP_LEFT || drag == CropDrag.BOTTOM_LEFT) right else left
        val anchorY = if (drag == CropDrag.TOP_LEFT || drag == CropDrag.TOP_RIGHT) bottom else top
        var width = abs((if (drag == CropDrag.TOP_LEFT || drag == CropDrag.BOTTOM_LEFT) left else right) - anchorX).coerceAtLeast(.05f)
        var height = width / normalizedRatio
        if (height > 1f) { height = 1f; width = height * normalizedRatio }
        if (drag == CropDrag.TOP_LEFT || drag == CropDrag.BOTTOM_LEFT) left = (anchorX - width).coerceAtLeast(0f) else right = (anchorX + width).coerceAtMost(1f)
        if (drag == CropDrag.TOP_LEFT || drag == CropDrag.TOP_RIGHT) top = (anchorY - height).coerceAtLeast(0f) else bottom = (anchorY + height).coerceAtMost(1f)
    }
    return Rect(left, top, right, bottom)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropOverlay(frame: Rect, overlay: CropOverlay) {
    val paint = Color.White.copy(alpha = .65f)
    when (overlay) {
        CropOverlay.THIRDS -> for (fraction in listOf(1f / 3f, 2f / 3f)) {
            drawLine(paint, Offset(frame.left + frame.width * fraction, frame.top), Offset(frame.left + frame.width * fraction, frame.bottom), 1f)
            drawLine(paint, Offset(frame.left, frame.top + frame.height * fraction), Offset(frame.right, frame.top + frame.height * fraction), 1f)
        }
        CropOverlay.DIAGONAL -> {
            drawLine(paint, frame.topLeft, frame.bottomRight, 1f)
            drawLine(paint, Offset(frame.right, frame.top), Offset(frame.left, frame.bottom), 1f)
        }
        CropOverlay.GOLDEN -> for (fraction in listOf(.382f, .618f)) {
            drawLine(paint, Offset(frame.left + frame.width * fraction, frame.top), Offset(frame.left + frame.width * fraction, frame.bottom), 1f)
            drawLine(paint, Offset(frame.left, frame.top + frame.height * fraction), Offset(frame.right, frame.top + frame.height * fraction), 1f)
        }
    }
}
