package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.ShapeLine
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

internal enum class VectorLayerKind { TEXT, STICKER, RECTANGLE, OVAL, DRAWING }
internal enum class VectorBlend(val label: String, val platform: BlendMode) {
    NORMAL("Normal", BlendMode.SRC_OVER), MULTIPLY("Multiply", BlendMode.MULTIPLY),
    SCREEN("Screen", BlendMode.SCREEN), OVERLAY("Overlay", BlendMode.OVERLAY),
}

internal data class EditableVectorLayer(
    val id: String = UUID.randomUUID().toString(),
    val kind: VectorLayerKind,
    val content: String = "",
    val color: Int = AndroidColor.WHITE,
    val x: Float = .5f,
    val y: Float = .5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val blend: VectorBlend = VectorBlend.NORMAL,
    val points: List<Offset> = emptyList(),
)

@Composable
internal fun VectorLayerEditor(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    BackHandler(onBack = onCancel)
    var layers by remember { mutableStateOf<List<EditableVectorLayer>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf<List<List<EditableVectorLayer>>>(emptyList()) }
    var redo by remember { mutableStateOf<List<List<EditableVectorLayer>>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var textDialog by remember { mutableStateOf(false) }
    var textDraft by remember { mutableStateOf("") }
    var drawing by remember { mutableStateOf(false) }
    var layerPanel by remember { mutableStateOf(false) }
    val latestLayers by rememberUpdatedState(layers)

    fun commit(next: List<EditableVectorLayer>) {
        if (next == layers) return
        undo = (undo + listOf(layers)).takeLast(50)
        layers = next
        redo = emptyList()
    }
    fun add(layer: EditableVectorLayer) { commit(layers + layer); selectedId = layer.id }
    fun updateSelected(transform: (EditableVectorLayer) -> EditableVectorLayer, checkpoint: Boolean = false) {
        val next = layers.map { if (it.id == selectedId) transform(it) else it }
        if (checkpoint) commit(next) else layers = next
    }

    if (textDialog) {
        AlertDialog(
            onDismissRequest = { textDialog = false },
            title = { Text("Add text") },
            text = { OutlinedTextField(textDraft, { textDraft = it }, label = { Text("Text") }) },
            confirmButton = { TextButton(onClick = {
                if (textDraft.isNotBlank()) add(EditableVectorLayer(kind = VectorLayerKind.TEXT, content = textDraft.trim()))
                textDraft = ""; textDialog = false
            }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { textDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel layers") }
                Text("Layers", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(enabled = undo.isNotEmpty(), onClick = {
                    val previous = undo.last(); undo = undo.dropLast(1); redo = (redo + listOf(layers)).takeLast(50); layers = previous
                }) { Icon(Icons.Outlined.Undo, "Undo") }
                IconButton(enabled = redo.isNotEmpty(), onClick = {
                    val next = redo.last(); redo = redo.dropLast(1); undo = (undo + listOf(layers)).takeLast(50); layers = next
                }) { Icon(Icons.Outlined.Redo, "Redo") }
                IconButton(onClick = { onDone(renderVectorScene(source, layers)) }) { Icon(Icons.Outlined.Check, "Apply layers") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color(0xFF17171A)).navigationBarsPadding().padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LayerTool("Text", Icons.Outlined.TextFields) { textDialog = true }
                    LayerTool("Sticker", Icons.Outlined.Add) { add(EditableVectorLayer(kind = VectorLayerKind.STICKER, content = "✨")) }
                    LayerTool("Rectangle", Icons.Outlined.ShapeLine) { add(EditableVectorLayer(kind = VectorLayerKind.RECTANGLE, color = AndroidColor.WHITE)) }
                    LayerTool("Oval", Icons.Outlined.ShapeLine) { add(EditableVectorLayer(kind = VectorLayerKind.OVAL, color = AndroidColor.WHITE)) }
                    LayerTool(if (drawing) "Drawing on" else "Draw", Icons.Outlined.Brush) { drawing = !drawing }
                    LayerTool("Layer panel", Icons.Outlined.Layers) { layerPanel = !layerPanel }
                }
                if (layerPanel) VectorLayerPanel(
                    layers = layers,
                    selectedId = selectedId,
                    onSelect = { selectedId = it },
                    onCommit = ::commit,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
                    .pointerInput(drawing, selectedId, canvasSize) {
                        if (drawing) {
                            var activeId: String? = null
                            detectDragGestures(
                                onDragStart = { position ->
                                    val id = UUID.randomUUID().toString(); activeId = id
                                    val target = fitRect(source.width, source.height, size.width.toFloat(), size.height.toFloat())
                                    val p = Offset(
                                        ((position.x - target.left) / target.width()).coerceIn(0f, 1f),
                                        ((position.y - target.top) / target.height()).coerceIn(0f, 1f),
                                    )
                                    undo = (undo + listOf(latestLayers)).takeLast(50); redo = emptyList()
                                    layers = latestLayers + EditableVectorLayer(id = id, kind = VectorLayerKind.DRAWING, points = listOf(p))
                                    selectedId = id
                                },
                                onDrag = { change, _ ->
                                    val target = fitRect(source.width, source.height, size.width.toFloat(), size.height.toFloat())
                                    val p = Offset(
                                        ((change.position.x - target.left) / target.width()).coerceIn(0f, 1f),
                                        ((change.position.y - target.top) / target.height()).coerceIn(0f, 1f),
                                    )
                                    layers = latestLayers.map { if (it.id == activeId) it.copy(points = it.points + p) else it }
                                },
                                onDragEnd = { activeId = null },
                            )
                        } else {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val selected = latestLayers.firstOrNull { it.id == selectedId }
                                if (selected != null && !selected.locked) {
                                    val target = fitRect(source.width, source.height, size.width.toFloat(), size.height.toFloat())
                                    layers = latestLayers.map {
                                        if (it.id == selectedId) it.copy(
                                            x = (it.x + pan.x / target.width().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                            y = (it.y + pan.y / target.height().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                            scale = (it.scale * zoom).coerceIn(.15f, 8f),
                                            rotation = it.rotation + rotation,
                                        ) else it
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(drawing) {
                        if (!drawing) detectTapGestures { tap ->
                            val target = fitRect(source.width, source.height, size.width.toFloat(), size.height.toFloat())
                            selectedId = latestLayers.asReversed().minByOrNull {
                                val dx = (tap.x - target.left) / target.width() - it.x
                                val dy = (tap.y - target.top) / target.height() - it.y
                                dx * dx + dy * dy
                            }?.id
                        }
                    },
            ) {
                drawIntoCanvas { canvas ->
                    val target = fitRect(source.width, source.height, size.width, size.height)
                    canvas.nativeCanvas.drawBitmap(source, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(target.left, target.top)
                    layers.forEach { drawVectorLayer(canvas.nativeCanvas, it, target.width(), target.height(), selected = it.id == selectedId) }
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
}

@Composable
private fun LayerTool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.height(48.dp)) { Icon(icon, null); Spacer(Modifier.width(6.dp)); Text(label) }
}

@Composable
private fun VectorLayerPanel(
    layers: List<EditableVectorLayer>, selectedId: String?, onSelect: (String) -> Unit,
    onCommit: (List<EditableVectorLayer>) -> Unit,
) {
    val selected = layers.firstOrNull { it.id == selectedId }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            layers.asReversed().forEach { layer ->
                FilterChip(layer.id == selectedId, { onSelect(layer.id) }, label = { Text(layer.kind.name.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }
        if (selected != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onCommit(layers.map { if (it.id == selected.id) it.copy(visible = !it.visible) else it }) }) {
                    Icon(if (selected.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "Toggle visibility")
                }
                IconButton(onClick = { onCommit(layers.map { if (it.id == selected.id) it.copy(locked = !it.locked) else it }) }) {
                    Icon(if (selected.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, "Toggle layer lock")
                }
                IconButton(onClick = { onCommit(layers + selected.copy(id = UUID.randomUUID().toString(), x = (selected.x + .04f).coerceAtMost(1f), y = (selected.y + .04f).coerceAtMost(1f))) }) {
                    Icon(Icons.Outlined.ContentCopy, "Duplicate layer")
                }
                IconButton(onClick = { onCommit(layers.filterNot { it.id == selected.id }) }) { Icon(Icons.Outlined.Delete, "Delete layer") }
                TextButton(onClick = {
                    val index = layers.indexOfFirst { it.id == selected.id }
                    if (index in 0 until layers.lastIndex) onCommit(layers.toMutableList().apply { add(index + 1, removeAt(index)) })
                }) { Text("Up") }
                TextButton(onClick = {
                    val index = layers.indexOfFirst { it.id == selected.id }
                    if (index > 0) onCommit(layers.toMutableList().apply { add(index - 1, removeAt(index)) })
                }) { Text("Down") }
                TextButton(onClick = { onCommit(layers.map { if (it.id == selected.id) it.copy(blend = VectorBlend.entries[(it.blend.ordinal + 1) % VectorBlend.entries.size]) else it }) }) {
                    Text(selected.blend.label)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacity", Modifier.width(64.dp), style = MaterialTheme.typography.labelMedium)
                Slider(selected.opacity, { value -> onCommit(layers.map { if (it.id == selected.id) it.copy(opacity = value) else it }) }, Modifier.weight(1f))
            }
        }
    }
}

internal fun renderVectorScene(source: Bitmap, layers: List<EditableVectorLayer>): Bitmap {
    val output = createEditBitmapLike(source)
    val canvas = AndroidCanvas(output)
    canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    layers.forEach { drawVectorLayer(canvas, it, source.width.toFloat(), source.height.toFloat(), selected = false) }
    return output
}

private fun drawVectorLayer(canvas: AndroidCanvas, layer: EditableVectorLayer, width: Float, height: Float, selected: Boolean) {
    if (!layer.visible) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = layer.color; alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt(); blendMode = layer.blend.platform
        style = if (layer.kind == VectorLayerKind.RECTANGLE || layer.kind == VectorLayerKind.OVAL) Paint.Style.STROKE else Paint.Style.FILL
        strokeWidth = max(3f, min(width, height) * .008f * layer.scale)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    if (layer.kind == VectorLayerKind.DRAWING) {
        if (layer.points.size > 1) {
            val path = Path().apply {
                moveTo(layer.points.first().x * width, layer.points.first().y * height)
                layer.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
            }
            paint.style = Paint.Style.STROKE
            canvas.drawPath(path, paint)
        }
        return
    }
    canvas.save()
    canvas.translate(layer.x * width, layer.y * height)
    canvas.rotate(layer.rotation)
    val base = min(width, height) * .12f * layer.scale
    when (layer.kind) {
        VectorLayerKind.TEXT -> {
            paint.textSize = base; paint.textAlign = Paint.Align.CENTER; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText(layer.content, 0f, -((paint.ascent() + paint.descent()) / 2f), paint)
        }
        VectorLayerKind.STICKER -> {
            paint.textSize = base * 1.35f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText(layer.content, 0f, -((paint.ascent() + paint.descent()) / 2f), paint)
        }
        VectorLayerKind.RECTANGLE -> canvas.drawRoundRect(-base, -base * .7f, base, base * .7f, base * .12f, base * .12f, paint)
        VectorLayerKind.OVAL -> canvas.drawOval(-base, -base * .7f, base, base * .7f, paint)
        VectorLayerKind.DRAWING -> Unit
    }
    if (selected) {
        paint.color = AndroidColor.WHITE; paint.alpha = 220; paint.blendMode = BlendMode.SRC_OVER; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        canvas.drawRect(-base * 1.15f, -base * .9f, base * 1.15f, base * .9f, paint)
    }
    canvas.restore()
}

private fun fitRect(sourceWidth: Int, sourceHeight: Int, width: Float, height: Float): android.graphics.RectF {
    val scale = min(width / max(1, sourceWidth), height / max(1, sourceHeight))
    val w = sourceWidth * scale; val h = sourceHeight * scale
    return android.graphics.RectF((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
}
