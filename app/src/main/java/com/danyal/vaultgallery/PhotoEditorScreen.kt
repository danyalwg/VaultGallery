package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Details
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Exposure
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.danyal.vaultgallery.ui.VaultBlue
import com.danyal.vaultgallery.ui.VaultPrimary
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.editor.EditProjectStore
import com.danyal.vaultgallery.editor.PhotoEditProject
import com.danyal.vaultgallery.editor.PhotoOperation
import com.danyal.vaultgallery.editor.PhotoPreset
import com.danyal.vaultgallery.editor.PhotoPresetStore
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal enum class EditorLaunchMode { STANDARD, AI_ASSIST, DOCUMENT_SCAN }
internal enum class EditorSaveMode { REPLACE, COPY }
enum class PhotoOutputFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp"),
    HEIC("HEIC", "heic", "image/heic"),
    AVIF("AVIF", "avif", "image/avif"),
}
data class PhotoExportOptions(
    val format: PhotoOutputFormat,
    val quality: Int,
    val maxDimension: Int?,
    val stripMetadata: Boolean,
)

/** Mirrors the five stable destinations in Samsung Gallery's editor. */
private enum class EditorTool(val label: String) {
    TRANSFORM("Transform"), FILTERS("Filters"), TONE("Tone"), DECORATIONS("Decorations"), TOOLS("Tools")
}

private enum class DecorationMode(val label: String) { DRAW("Draw"), MOSAIC("Mosaic"), STICKERS("Stickers"), TEXT("Text") }
private enum class StickerCategory(val label: String) { EMOTIONS("Emotions"), SYMBOLS("Symbols"), NATURE("Nature"), BADGES("Badges") }
private data class GraphicSticker(val text: String, val color: Int)

private fun createGraphicSticker(sticker: GraphicSticker): Bitmap = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888).also { bitmap ->
    val canvas = Canvas(bitmap)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sticker.color }
    canvas.drawRoundRect(RectF(8f, 8f, 312f, 152f), 48f, 48f, background)
    canvas.drawRoundRect(RectF(15f, 15f, 305f, 145f), 42f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 6f
    })
    canvas.drawText(sticker.text, 160f, 108f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (sticker.color == 0xFFFFD60A.toInt()) AndroidColor.BLACK else AndroidColor.WHITE
        textSize = 70f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(5f, 0f, 3f, 0x66000000)
    })
}
private enum class PhotoEditKind { ANNOTATION, ADJUSTMENT, BITMAP }
private enum class GeometryMode(val label: String, val range: ClosedFloatingPointRange<Float>) {
    STRAIGHTEN("Straighten", -45f..45f),
    HORIZONTAL("Horizontal perspective", -.35f..0.35f),
    VERTICAL("Vertical perspective", -.35f..0.35f),
}

private enum class ToneControl(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val initial: Float,
) {
    LIGHT_BALANCE("Light balance", -50f..50f, 0f),
    BRIGHTNESS("Brightness", -80f..80f, 0f),
    EXPOSURE("Exposure", -50f..50f, 0f),
    CONTRAST("Contrast", .5f..1.6f, 1f),
    HIGHLIGHTS("Highlights", -50f..50f, 0f),
    SHADOWS("Shadows", -50f..50f, 0f),
    BLACK_POINT("Black point", 0f..64f, 0f),
    WHITE_POINT("White point", 191f..255f, 255f),
    SATURATION("Saturation", 0f..2f, 1f),
    TINT("Tint", -50f..50f, 0f),
    TEMPERATURE("Temperature", -60f..60f, 0f),
}

private enum class AiToolGroup(val label: String) {
    ENHANCE("Enhance"), RESTORE("Restore"), PORTRAIT("Portrait"), DOCUMENT("Document"), CREATIVE("Creative")
}

private val EditorYellow = Color(0xFFFFD60A)

private data class PhotoAdjustmentState(
    val filter: FilterChoice,
    val lightBalance: Float,
    val brightness: Float,
    val exposure: Float,
    val contrast: Float,
    val highlights: Float,
    val shadows: Float,
    val blackPoint: Float,
    val whitePoint: Float,
    val saturation: Float,
    val tint: Float,
    val temperature: Float,
    val redGain: Float,
    val greenGain: Float,
    val blueGain: Float,
)

private data class AiEditAction(
    val title: String,
    val description: String,
    val transform: suspend (Bitmap) -> Bitmap,
    val neural: Boolean = false,
)

private data class AiSelectionLaunch(
    val bitmap: Bitmap,
    val effect: AiSelectionEffect,
    val startWithLasso: Boolean = false,
)

/**
 * Filters are deliberately represented as ordinary colour matrices instead of PhotoEditor's
 * GPU-backed FilterImageView filters. The latter inserts a SurfaceView and saveAsBitmap can wait
 * forever for that surface on modern Pixels. ImageView colour filters are drawn into the editor
 * snapshot synchronously, so the preview and the saved bitmap use the same reliable path.
 */
private data class FilterChoice(val label: String, val matrix: FloatArray? = null)

private fun colourMatrix(
    red: Float = 1f,
    green: Float = 1f,
    blue: Float = 1f,
    redOffset: Float = 0f,
    greenOffset: Float = 0f,
    blueOffset: Float = 0f,
) = floatArrayOf(
    red, 0f, 0f, 0f, redOffset,
    0f, green, 0f, 0f, greenOffset,
    0f, 0f, blue, 0f, blueOffset,
    0f, 0f, 0f, 1f, 0f,
)

private fun contrastMatrix(amount: Float, offset: Float = 0f) = floatArrayOf(
    amount, 0f, 0f, 0f, 128f * (1f - amount) + offset,
    0f, amount, 0f, 0f, 128f * (1f - amount) + offset,
    0f, 0f, amount, 0f, 128f * (1f - amount) + offset,
    0f, 0f, 0f, 1f, 0f,
)

private val editorFilters = listOf(
    FilterChoice("Original"),
    FilterChoice("Auto", contrastMatrix(1.08f, 4f)),
    FilterChoice("Vivid", android.graphics.ColorMatrix().apply { setSaturation(1.35f) }.array),
    FilterChoice("Warm", colourMatrix(red = 1.08f, green = 1.01f, blue = .88f, redOffset = 7f, blueOffset = -4f)),
    FilterChoice("Mono", android.graphics.ColorMatrix().apply { setSaturation(0f) }.array),
    FilterChoice("Film", colourMatrix(red = 1.04f, green = .98f, blue = .90f, redOffset = 8f, greenOffset = 3f, blueOffset = 10f)),
    FilterChoice("Dramatic", contrastMatrix(1.24f, -4f)),
    FilterChoice("Duotone", floatArrayOf(
        .72f, .18f, .10f, 0f, 8f,
        .10f, .66f, .18f, 0f, 0f,
        .08f, .18f, .78f, 0f, 12f,
        0f, 0f, 0f, 1f, 0f,
    )),
    FilterChoice("Glow", contrastMatrix(.92f, 16f)),
    FilterChoice("Fade", contrastMatrix(.82f, 22f)),
    FilterChoice("Lomo", colourMatrix(red = 1.12f, green = 1.02f, blue = .90f, redOffset = 2f, blueOffset = -5f)),
    FilterChoice("Negative", floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )),
    FilterChoice("Sepia", floatArrayOf(
        .393f, .769f, .189f, 0f, 0f,
        .349f, .686f, .168f, 0f, 0f,
        .272f, .534f, .131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )),
    FilterChoice("Cool", colourMatrix(red = .90f, green = 1.01f, blue = 1.12f, redOffset = -3f, blueOffset = 7f)),
    FilterChoice("Vintage", colourMatrix(red = 1.02f, green = .94f, blue = .82f, redOffset = 12f, greenOffset = 6f, blueOffset = 8f)),
)

private val editorColors = listOf(
    AndroidColor.WHITE, AndroidColor.BLACK, 0xFFF44336.toInt(), 0xFFFF9800.toInt(),
    0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFF9C27B0.toInt(),
)

/** Deterministic CPU renderer used when an edit has no overlay layers. It avoids PhotoEditor's
 * view-capture path entirely for filters and tone changes while producing the same color matrix
 * shown by the source ImageView. */
internal fun renderPhotoColorMatrix(source: Bitmap, matrix: AndroidColorMatrix): Bitmap =
    createEditBitmapLike(source).also { output ->
        Canvas(output).drawBitmap(
            source,
            0f,
            0f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            },
        )
    }

internal fun renderPhotoAdjustment(source: Bitmap, operation: PhotoOperation.Adjustment): Bitmap {
    val matrix = AndroidColorMatrix().apply {
        editorFilters.firstOrNull { it.label == operation.filter }?.matrix?.let(::set)
    }
    matrix.postConcat(AndroidColorMatrix().apply { setSaturation(operation.saturation) })
    val lift = operation.brightness + operation.exposure * 1.4f + operation.shadows.coerceAtLeast(0f) * .55f +
        operation.highlights.coerceAtMost(0f) * .35f + operation.lightBalance * .35f
    val contrastOffset = 128f * (1f - operation.contrast) + lift
    matrix.postConcat(AndroidColorMatrix(floatArrayOf(
        operation.contrast + operation.temperature / 180f, operation.tint / 260f, 0f, 0f, contrastOffset,
        0f, operation.contrast, operation.tint / 300f, 0f, contrastOffset,
        0f, -operation.tint / 260f, operation.contrast - operation.temperature / 180f, 0f, contrastOffset,
        0f, 0f, 0f, 1f, 0f,
    )))
    val levels = levelTransform(operation.blackPoint, operation.whitePoint)
    matrix.postConcat(AndroidColorMatrix(floatArrayOf(
        levels.scale * operation.redGain, 0f, 0f, 0f, levels.offset,
        0f, levels.scale * operation.greenGain, 0f, 0f, levels.offset,
        0f, 0f, levels.scale * operation.blueGain, 0f, levels.offset,
        0f, 0f, 0f, 1f, 0f,
    )))
    return renderPhotoColorMatrix(source, matrix)
}

/**
 * Shared production editor surface. PhotoEditor currently composites markup while Vault Gallery's
 * own gesture-driven geometry editor handles cropping in both public and encrypted galleries.
 */
@Composable
internal fun PhotoEditorScreen(
    title: String,
    bitmap: Bitmap?,
    saving: Boolean,
    onBack: () -> Unit,
    launchMode: EditorLaunchMode = EditorLaunchMode.STANDARD,
    projectSource: String = title,
    onSave: (Bitmap, EditorSaveMode, PhotoExportOptions?) -> Unit,
) {
    if (launchMode == EditorLaunchMode.DOCUMENT_SCAN) {
        DocumentScanEditor(title = title, bitmap = bitmap, saving = saving, onBack = onBack) {
            onSave(it, EditorSaveMode.COPY, null)
        }
        return
    }
    val context = LocalContext.current
    val aiRuntime = remember(context) { AiExecutionRuntime(context) }
    val projectStore = remember(context) { EditProjectStore(context) }
    val presetStore = remember(context) { PhotoPresetStore(context) }
    val scope = rememberCoroutineScope()
    var projectId by remember(projectSource) { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var projectLoaded by remember(projectSource) { mutableStateOf(false) }
    var workingBitmap by remember(bitmap) { mutableStateOf(bitmap) }
    var editor by remember { mutableStateOf<PhotoEditor?>(null) }
    var editorView by remember { mutableStateOf<PhotoEditorView?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var liveGeometryRotation by remember { mutableFloatStateOf(0f) }
    var liveGeometryScaleX by remember { mutableFloatStateOf(1f) }
    var tool by remember { mutableStateOf(EditorTool.TRANSFORM) }
    var decorationMode by remember { mutableStateOf(DecorationMode.DRAW) }
    var selectedTone by remember { mutableStateOf(ToneControl.BRIGHTNESS) }
    var selectedFilter by remember { mutableStateOf(editorFilters.first()) }
    var brushColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var brushSize by remember { mutableFloatStateOf(12f) }
    var brushOpacity by remember { mutableFloatStateOf(100f) }
    var stickerCategory by remember { mutableStateOf(StickerCategory.EMOTIONS) }
    var textDialog by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var textColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var processing by remember { mutableStateOf(false) }
    var cropError by remember { mutableStateOf<String?>(null) }
    var cropReady by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var highlights by remember { mutableFloatStateOf(0f) }
    var shadows by remember { mutableFloatStateOf(0f) }
    var blackPoint by remember { mutableFloatStateOf(0f) }
    var whitePoint by remember { mutableFloatStateOf(255f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var lightBalance by remember { mutableFloatStateOf(0f) }
    var redGain by remember { mutableFloatStateOf(1f) }
    var greenGain by remember { mutableFloatStateOf(1f) }
    var blueGain by remember { mutableFloatStateOf(1f) }
    var aiToolGroup by remember { mutableStateOf(AiToolGroup.ENHANCE) }
    var selectedAiAction by remember { mutableStateOf<AiEditAction?>(null) }
    var aiSelectionSource by remember { mutableStateOf<AiSelectionLaunch?>(null) }
    var mosaicSource by remember { mutableStateOf<Bitmap?>(null) }
    var geometrySource by remember { mutableStateOf<Pair<Bitmap, GeometryMode>?>(null) }
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }
    var selectiveColorSource by remember { mutableStateOf<Bitmap?>(null) }
    var cubeLutSource by remember { mutableStateOf<Bitmap?>(null) }
    var curvesSource by remember { mutableStateOf<Bitmap?>(null) }
    var localAdjustmentSource by remember { mutableStateOf<Bitmap?>(null) }
    var lensCorrectionSource by remember { mutableStateOf<Bitmap?>(null) }
    var vectorLayerSource by remember { mutableStateOf<Bitmap?>(null) }
    var aiLabSource by remember(bitmap, launchMode) {
        mutableStateOf(if (launchMode == EditorLaunchMode.AI_ASSIST) bitmap else null)
    }
    var bitmapUndoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var bitmapRedoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var adjustmentUndoStack by remember { mutableStateOf<List<PhotoAdjustmentState>>(emptyList()) }
    var adjustmentRedoStack by remember { mutableStateOf<List<PhotoAdjustmentState>>(emptyList()) }
    var toneBeforeGesture by remember { mutableStateOf<PhotoAdjustmentState?>(null) }
    var undoOrder by remember { mutableStateOf<List<PhotoEditKind>>(emptyList()) }
    var redoOrder by remember { mutableStateOf<List<PhotoEditKind>>(emptyList()) }
    var suppressAnnotationHistory by remember { mutableStateOf(false) }
    var annotationGestureActive by remember { mutableStateOf(false) }
    var comparingOriginal by remember { mutableStateOf(false) }
    var discardPrompt by remember { mutableStateOf(false) }
    var histogramVisible by remember { mutableStateOf(false) }
    var clippingVisible by remember { mutableStateOf(false) }
    var whiteBalancePickerActive by remember { mutableStateOf(false) }
    var splitCompareVisible by remember { mutableStateOf(false) }
    var splitCompareFraction by remember { mutableFloatStateOf(.5f) }
    var editorCanvasSize by remember { mutableStateOf(IntSize.Zero) }
    var exportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(PhotoOutputFormat.JPEG) }
    var exportQuality by remember { mutableFloatStateOf(92f) }
    var exportMaxDimension by remember { mutableStateOf<Int?>(null) }
    var exportStripMetadata by remember { mutableStateOf(false) }
    var presetDialog by remember { mutableStateOf(false) }
    var presets by remember { mutableStateOf<List<PhotoPreset>>(emptyList()) }

    fun bitmapHistoryLimit(value: Bitmap): Int = if (value.allocationByteCount >= 96 * 1024 * 1024) 1 else 3

    fun recordEditKind(kind: PhotoEditKind, replaceHistory: Boolean = false) {
        undoOrder = if (replaceHistory) listOf(kind) else (undoOrder + kind).takeLast(40)
        redoOrder = emptyList()
    }

    fun checkpointBitmap(before: Bitmap) {
        // One 50 MP ARGB frame is about 200 MB. Keep a single destructive checkpoint for
        // exceptionally large originals while retaining three for ordinary photos.
        bitmapUndoStack = (bitmapUndoStack + before).takeLast(bitmapHistoryLimit(before))
        bitmapRedoStack = emptyList()
        adjustmentUndoStack = emptyList()
        adjustmentRedoStack = emptyList()
        recordEditKind(PhotoEditKind.BITMAP, replaceHistory = true)
    }

    // Keep save actions inert for one transition beat after returning from the crop surface so a
    // finishing pointer-up cannot accidentally trigger Save underneath the editor transition.
    LaunchedEffect(cropReady) {
        if (cropReady) {
            delay(700)
            cropReady = false
        }
    }

    fun currentAdjustmentMatrix(): AndroidColorMatrix {
        val matrix = AndroidColorMatrix().apply {
            selectedFilter.matrix?.let(::set)
        }
        matrix.postConcat(AndroidColorMatrix().apply { setSaturation(saturation) })
        // Color-matrix preview for Samsung's tone rail. Expensive local-detail operations
        // (sharpness/definition) are explicit tools below so dragging this slider stays live.
        val lift = brightness + exposure * 1.4f + shadows.coerceAtLeast(0f) * .55f +
            highlights.coerceAtMost(0f) * .35f + lightBalance * .35f
        val contrastOffset = 128f * (1f - contrast) + lift
        matrix.postConcat(AndroidColorMatrix(floatArrayOf(
            contrast + warmth / 180f, tint / 260f, 0f, 0f, contrastOffset,
            0f, contrast, tint / 300f, 0f, contrastOffset,
            0f, -tint / 260f, contrast - warmth / 180f, 0f, contrastOffset,
            0f, 0f, 0f, 1f, 0f,
        )))
        val levels = levelTransform(blackPoint, whitePoint)
        matrix.postConcat(AndroidColorMatrix(floatArrayOf(
            levels.scale * redGain, 0f, 0f, 0f, levels.offset,
            0f, levels.scale * greenGain, 0f, 0f, levels.offset,
            0f, 0f, levels.scale * blueGain, 0f, levels.offset,
            0f, 0f, 0f, 1f, 0f,
        )))
        return matrix
    }

    fun applyTune() {
        editorView?.source?.colorFilter = ColorMatrixColorFilter(currentAdjustmentMatrix())
    }

    val preserveTransparency = bitmap?.hasAlpha() == true
    val saveSettings = remember(preserveTransparency) {
        SaveSettings.Builder()
            .setTransparencyEnabled(preserveTransparency)
            .setClearViewsEnabled(false)
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .setCompressQuality(97)
            .build()
    }

    suspend fun renderComposite(active: PhotoEditor): Bitmap {
        val source = workingBitmap ?: error("The photo is unavailable")
        return if (!active.isUndoAvailable) {
            withContext(Dispatchers.Default) { renderPhotoColorMatrix(source, currentAdjustmentMatrix()) }
        } else {
            // Drawing/text/sticker layers must be composited by PhotoEditor. The previous GPU
            // filter SurfaceView is no longer used, so this path remains bounded and reliable.
            withTimeout(45_000L) { active.saveAsBitmap(saveSettings) }
        }
    }

    fun resetLiveTone() {
        brightness = 0f; contrast = 1f; saturation = 1f; warmth = 0f
        exposure = 0f; highlights = 0f; shadows = 0f; tint = 0f; lightBalance = 0f
        blackPoint = 0f; whitePoint = 255f
        redGain = 1f; greenGain = 1f; blueGain = 1f
    }

    fun adjustmentState() = PhotoAdjustmentState(
        filter = selectedFilter,
        lightBalance = lightBalance,
        brightness = brightness,
        exposure = exposure,
        contrast = contrast,
        highlights = highlights,
        shadows = shadows,
        blackPoint = blackPoint,
        whitePoint = whitePoint,
        saturation = saturation,
        tint = tint,
        temperature = warmth,
        redGain = redGain,
        greenGain = greenGain,
        blueGain = blueGain,
    )

    fun adjustmentOperation() = PhotoOperation.Adjustment(
        filter = selectedFilter.label,
        lightBalance = lightBalance,
        brightness = brightness,
        exposure = exposure,
        contrast = contrast,
        highlights = highlights,
        shadows = shadows,
        blackPoint = blackPoint,
        whitePoint = whitePoint,
        saturation = saturation,
        tint = tint,
        temperature = warmth,
        redGain = redGain,
        greenGain = greenGain,
        blueGain = blueGain,
    )

    fun applyAdjustmentOperation(operation: PhotoOperation.Adjustment) {
        val before = adjustmentState()
        selectedFilter = editorFilters.firstOrNull { it.label == operation.filter } ?: editorFilters.first()
        lightBalance = operation.lightBalance
        brightness = operation.brightness
        exposure = operation.exposure
        contrast = operation.contrast
        highlights = operation.highlights
        shadows = operation.shadows
        blackPoint = operation.blackPoint
        whitePoint = operation.whitePoint
        saturation = operation.saturation
        tint = operation.tint
        warmth = operation.temperature
        redGain = operation.redGain
        greenGain = operation.greenGain
        blueGain = operation.blueGain
        adjustmentUndoStack = (adjustmentUndoStack + before).takeLast(30)
        adjustmentRedoStack = emptyList()
        recordEditKind(PhotoEditKind.ADJUSTMENT)
        applyTune()
    }

    fun restoreAdjustments(state: PhotoAdjustmentState) {
        selectedFilter = state.filter
        lightBalance = state.lightBalance
        brightness = state.brightness
        exposure = state.exposure
        contrast = state.contrast
        highlights = state.highlights
        shadows = state.shadows
        blackPoint = state.blackPoint
        whitePoint = state.whitePoint
        saturation = state.saturation
        tint = state.tint
        warmth = state.temperature
        redGain = state.redGain
        greenGain = state.greenGain
        blueGain = state.blueGain
        applyTune()
    }

    LaunchedEffect(projectSource) {
        presets = presetStore.list()
        projectStore.loadPhoto(projectSource)?.let { project ->
            projectId = project.id
            project.activeOperations.filterIsInstance<PhotoOperation.Adjustment>().lastOrNull()?.let { operation ->
                selectedFilter = editorFilters.firstOrNull { it.label == operation.filter } ?: editorFilters.first()
                lightBalance = operation.lightBalance
                brightness = operation.brightness
                exposure = operation.exposure
                contrast = operation.contrast
                highlights = operation.highlights
                shadows = operation.shadows
                blackPoint = operation.blackPoint
                whitePoint = operation.whitePoint
                saturation = operation.saturation
                tint = operation.tint
                warmth = operation.temperature
                redGain = operation.redGain
                greenGain = operation.greenGain
                blueGain = operation.blueGain
            }
        }
        projectLoaded = true
    }

    LaunchedEffect(
        projectLoaded, selectedFilter, lightBalance, brightness, exposure, contrast, highlights, shadows,
        blackPoint, whitePoint, saturation, tint, warmth, redGain, greenGain, blueGain,
    ) {
        if (!projectLoaded) return@LaunchedEffect
        delay(400)
        projectStore.savePhoto(
            PhotoEditProject(
                id = projectId,
                source = projectSource,
                operations = listOf(
                    adjustmentOperation(),
                ),
                cursor = 1,
            ),
        )
    }

    fun sampleWhiteBalance(position: Offset) {
        val source = workingBitmap ?: return
        val width = editorCanvasSize.width.toFloat()
        val height = editorCanvasSize.height.toFloat()
        if (width <= 0f || height <= 0f) return
        val scale = minOf(width / source.width.coerceAtLeast(1), height / source.height.coerceAtLeast(1))
        val renderedWidth = source.width * scale
        val renderedHeight = source.height * scale
        val left = (width - renderedWidth) / 2f
        val top = (height - renderedHeight) / 2f
        if (position.x !in left..(left + renderedWidth) || position.y !in top..(top + renderedHeight)) return
        val centerX = ((position.x - left) / scale).toInt().coerceIn(0, source.width - 1)
        val centerY = ((position.y - top) / scale).toInt().coerceIn(0, source.height - 1)
        var red = 0L; var green = 0L; var blue = 0L; var samples = 0L
        val radius = maxOf(2, (12f / scale).toInt())
        for (y in (centerY - radius).coerceAtLeast(0)..(centerY + radius).coerceAtMost(source.height - 1)) {
            for (x in (centerX - radius).coerceAtLeast(0)..(centerX + radius).coerceAtMost(source.width - 1)) {
                val pixel = source.getPixel(x, y)
                red += AndroidColor.red(pixel); green += AndroidColor.green(pixel); blue += AndroidColor.blue(pixel); samples++
            }
        }
        if (samples == 0L) return
        val r = (red / samples).toFloat().coerceAtLeast(1f)
        val g = (green / samples).toFloat().coerceAtLeast(1f)
        val b = (blue / samples).toFloat().coerceAtLeast(1f)
        val gains = neutralWhiteBalance(r, g, b)
        val before = adjustmentState()
        redGain = gains.red
        greenGain = gains.green
        blueGain = gains.blue
        adjustmentUndoStack = (adjustmentUndoStack + before).takeLast(30)
        adjustmentRedoStack = emptyList()
        recordEditKind(PhotoEditKind.ADJUSTMENT)
        whiteBalancePickerActive = false
        applyTune()
    }

    fun beginToneGesture() {
        if (toneBeforeGesture == null) toneBeforeGesture = adjustmentState()
    }

    fun commitToneGesture() {
        toneBeforeGesture?.let { before ->
            if (before != adjustmentState()) {
                adjustmentUndoStack = (adjustmentUndoStack + before).takeLast(30)
                adjustmentRedoStack = emptyList()
                recordEditKind(PhotoEditKind.ADJUSTMENT)
            }
        }
        toneBeforeGesture = null
    }

    fun applyAiTransform(label: String, transform: suspend (Bitmap) -> Bitmap) {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching {
                val composite = renderComposite(active)
                val transformed = aiRuntime.run(
                    AiOperationSpec(label.lowercase().replace(' ', '-'), AiBackend.OPENCV),
                    composite,
                    transform,
                )
                composite to transformed
            }.onSuccess { (composite, transformed) ->
                // Compose/AndroidView may still draw the old source for one frame. Let the
                // runtime reclaim it instead of recycling a bitmap that is still on a canvas.
                checkpointBitmap(composite)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                selectedAiAction = null
                generation++
            }.onFailure { cropError = "$label failed: ${it.message ?: "unknown error"}" }
            processing = false
        }
    }

    fun launchAiSelection(initialEffect: AiSelectionEffect, startWithLasso: Boolean = false) {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { aiSelectionSource = AiSelectionLaunch(it, initialEffect, startWithLasso) }
                .onFailure { cropError = it.message ?: "Could not prepare AI Select" }
            processing = false
        }
    }

    fun launchAiLab() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { aiLabSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare Editing Studio" }
            processing = false
        }
    }

    fun launchMosaic() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { mosaicSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare the mosaic brush" }
            processing = false
        }
    }

    fun launchGeometry(mode: GeometryMode) {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { geometrySource = it to mode }
                .onFailure { cropError = it.message ?: "Could not prepare ${mode.label.lowercase()}" }
            processing = false
        }
    }

    fun launchSelectiveColor() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { selectiveColorSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare selective colour" }
            processing = false
        }
    }

    fun launchCubeLut() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { cubeLutSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare 3D LUT" }
            processing = false
        }
    }

    fun launchCurves() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { curvesSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare curves" }
            processing = false
        }
    }

    fun launchLensCorrection() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { lensCorrectionSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare lens correction" }
            processing = false
        }
    }

    fun launchVectorLayers() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { vectorLayerSource = it }
                .onFailure { cropError = it.message ?: "Could not prepare vector layers" }
            processing = false
        }
    }

    vectorLayerSource?.let { source ->
        VectorLayerEditor(
            source = source,
            onCancel = { vectorLayerSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                vectorLayerSource = null
                generation++
            },
        )
        return
    }

    lensCorrectionSource?.let { source ->
        LensCorrectionEditor(
            source = source,
            onCancel = { lensCorrectionSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                lensCorrectionSource = null
                generation++
            },
        )
        return
    }

    curvesSource?.let { source ->
        CurvesEditor(
            source = source,
            onCancel = { curvesSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                curvesSource = null
                generation++
            },
        )
        return
    }

    localAdjustmentSource?.let { source ->
        LocalAdjustmentEditor(
            source = source,
            onCancel = { localAdjustmentSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                localAdjustmentSource = null
                generation++
            },
        )
        return
    }

    cubeLutSource?.let { source ->
        CubeLutEditor(
            source = source,
            onCancel = { cubeLutSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                cubeLutSource = null
                generation++
            },
        )
        return
    }

    selectiveColorSource?.let { source ->
        SelectiveColorEditor(
            source = source,
            onCancel = { selectiveColorSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                selectiveColorSource = null
                generation++
            },
        )
        return
    }

    mosaicSource?.let { source ->
        MosaicBrushEditor(
            source = source,
            onCancel = { mosaicSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                mosaicSource = null
                decorationMode = DecorationMode.MOSAIC
                generation++
            },
        )
        return
    }

    val labSource = aiLabSource
    if (labSource != null) {
        AiPhotoLabScreen(
            source = labSource,
            onCancel = {
                aiLabSource = null
                if (launchMode == EditorLaunchMode.AI_ASSIST && labSource === bitmap) onBack()
            },
            onDone = { transformed ->
                checkpointBitmap(labSource)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                selectedAiAction = null
                aiLabSource = null
                tool = EditorTool.TOOLS
                generation++
            },
        )
        return
    }

    geometrySource?.let { (source, mode) ->
        GeometryAdjustmentScreen(
            source = source,
            mode = mode,
            onCancel = { geometrySource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                geometrySource = null
                generation++
            },
        )
        return
    }

    cropSource?.let { source ->
        UnifiedCropEditor(
            source = source,
            onCancel = { cropSource = null },
            onDone = { transformed ->
                checkpointBitmap(source)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                cropSource = null
                tool = EditorTool.TRANSFORM
                generation++
                cropReady = true
            },
        )
        return
    }

    val selectionSession = aiSelectionSource
    if (selectionSession != null) {
        AiSelectionStudio(
            source = selectionSession.bitmap,
            initialEffect = selectionSession.effect,
            startWithLasso = selectionSession.startWithLasso,
            onCancel = { aiSelectionSource = null },
            onApply = { transformed ->
                checkpointBitmap(selectionSession.bitmap)
                workingBitmap = transformed
                selectedAiAction = null
                selectedFilter = editorFilters.first()
                resetLiveTone()
                aiSelectionSource = null
                generation++
            },
        )
        return
    }

    fun setTool(next: EditorTool) {
        tool = next
        editor?.setBrushDrawingMode(next == EditorTool.DECORATIONS && decorationMode == DecorationMode.DRAW)
    }

    fun applyGeometry(label: String, matrix: Matrix, previewRotation: Float = 0f, previewScaleX: Float = 1f) {
        val active = editor ?: return
        // Present the transform on the existing hardware layer immediately. The full bitmap is
        // prepared off the UI thread and atomically replaces this preview when ready.
        liveGeometryRotation = previewRotation
        liveGeometryScaleX = previewScaleX
        scope.launch {
            processing = true
            runCatching {
                val composite = renderComposite(active)
                val transformed = withContext(Dispatchers.Default) {
                    Bitmap.createBitmap(composite, 0, 0, composite.width, composite.height, matrix, true)
                }
                composite to transformed
            }.onSuccess { (composite, transformed) ->
                checkpointBitmap(composite)
                workingBitmap = transformed
                selectedFilter = editorFilters.first()
                resetLiveTone()
                selectedAiAction = null
                generation++
            }.onFailure { cropError = "$label failed: ${it.message ?: "unknown error"}" }
            liveGeometryRotation = 0f
            liveGeometryScaleX = 1f
            processing = false
        }
    }

    fun hasUnsavedChanges(): Boolean = editor?.isUndoAvailable == true || bitmapUndoStack.isNotEmpty() ||
        adjustmentUndoStack.isNotEmpty() ||
        selectedFilter != editorFilters.first() || brightness != 0f || contrast != 1f || saturation != 1f ||
        warmth != 0f || exposure != 0f || highlights != 0f || shadows != 0f || tint != 0f || lightBalance != 0f

    fun requestClose() {
        if (hasUnsavedChanges()) discardPrompt = true else onBack()
    }

    BackHandler(onBack = ::requestClose)

    fun undoLastEdit() {
        when (undoOrder.lastOrNull()) {
            PhotoEditKind.ANNOTATION -> {
                suppressAnnotationHistory = true
                if (editor?.undo() == true) {
                    undoOrder = undoOrder.dropLast(1)
                    redoOrder = (redoOrder + PhotoEditKind.ANNOTATION).takeLast(40)
                }
                suppressAnnotationHistory = false
            }
            PhotoEditKind.ADJUSTMENT -> if (adjustmentUndoStack.isNotEmpty()) {
                adjustmentRedoStack = (adjustmentRedoStack + adjustmentState()).takeLast(30)
                restoreAdjustments(adjustmentUndoStack.last())
                adjustmentUndoStack = adjustmentUndoStack.dropLast(1)
                undoOrder = undoOrder.dropLast(1)
                redoOrder = (redoOrder + PhotoEditKind.ADJUSTMENT).takeLast(40)
            }
            PhotoEditKind.BITMAP -> if (bitmapUndoStack.isNotEmpty()) {
                workingBitmap?.let { bitmapRedoStack = (bitmapRedoStack + it).takeLast(bitmapHistoryLimit(it)) }
                workingBitmap = bitmapUndoStack.last()
                bitmapUndoStack = bitmapUndoStack.dropLast(1)
                selectedAiAction = null
                selectedFilter = editorFilters.first()
                generation++
                undoOrder = undoOrder.dropLast(1)
                redoOrder = (redoOrder + PhotoEditKind.BITMAP).takeLast(40)
            }
            null -> Unit
        }
    }

    fun redoLastEdit() {
        when (redoOrder.lastOrNull()) {
            PhotoEditKind.ANNOTATION -> {
                suppressAnnotationHistory = true
                if (editor?.redo() == true) {
                    redoOrder = redoOrder.dropLast(1)
                    undoOrder = (undoOrder + PhotoEditKind.ANNOTATION).takeLast(40)
                }
                suppressAnnotationHistory = false
            }
            PhotoEditKind.ADJUSTMENT -> if (adjustmentRedoStack.isNotEmpty()) {
                adjustmentUndoStack = (adjustmentUndoStack + adjustmentState()).takeLast(30)
                restoreAdjustments(adjustmentRedoStack.last())
                adjustmentRedoStack = adjustmentRedoStack.dropLast(1)
                redoOrder = redoOrder.dropLast(1)
                undoOrder = (undoOrder + PhotoEditKind.ADJUSTMENT).takeLast(40)
            }
            PhotoEditKind.BITMAP -> if (bitmapRedoStack.isNotEmpty()) {
                workingBitmap?.let { bitmapUndoStack = (bitmapUndoStack + it).takeLast(bitmapHistoryLimit(it)) }
                workingBitmap = bitmapRedoStack.last()
                bitmapRedoStack = bitmapRedoStack.dropLast(1)
                selectedAiAction = null
                selectedFilter = editorFilters.first()
                generation++
                redoOrder = redoOrder.dropLast(1)
                undoOrder = (undoOrder + PhotoEditKind.BITMAP).takeLast(40)
            }
            null -> Unit
        }
    }

    fun launchCrop() {
        val active = editor ?: return
        scope.launch {
            processing = true
            runCatching { renderComposite(active) }
                .onSuccess { composite -> processing = false; cropSource = composite }
                .onFailure { processing = false; cropError = it.message ?: "Crop could not be started." }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::requestClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
                IconButton(enabled = workingBitmap != null && !processing, onClick = ::launchAiLab) {
                    Icon(Icons.Outlined.AutoAwesome, "Photo assist", tint = Color.White)
                }
                val canUndo = undoOrder.isNotEmpty()
                val canRedo = redoOrder.isNotEmpty()
                IconButton(enabled = canUndo, onClick = ::undoLastEdit) {
                    Icon(Icons.Outlined.Undo, "Undo", tint = if (canUndo) Color.White else VaultSecondary)
                }
                IconButton(enabled = canRedo, onClick = ::redoLastEdit) {
                    Icon(Icons.Outlined.Redo, "Redo", tint = if (canRedo) Color.White else VaultSecondary)
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    enabled = workingBitmap != null && !processing && hasUnsavedChanges(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = VaultSecondary),
                    onClick = {
                    editor?.clearAllViews()
                    workingBitmap = bitmap
                    bitmapUndoStack = emptyList(); bitmapRedoStack = emptyList(); selectedAiAction = null
                    adjustmentUndoStack = emptyList(); adjustmentRedoStack = emptyList(); toneBeforeGesture = null
                    undoOrder = emptyList(); redoOrder = emptyList()
                    selectedFilter = editorFilters.first()
                    resetLiveTone()
                    generation++
                }) { Text("Revert") }
                TextButton(
                    enabled = workingBitmap != null && !saving && !processing && !cropReady && hasUnsavedChanges(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = VaultSecondary),
                    onClick = {
                    val active = editor ?: return@TextButton
                    scope.launch {
                        processing = true
                        runCatching { renderComposite(active) }
                            .onSuccess { onSave(it, EditorSaveMode.COPY, null) }
                            .onFailure { cropError = it.message ?: "The edited photo could not be saved." }
                        processing = false
                    }
                }) { Text("Save copy") }
                TextButton(
                    enabled = workingBitmap != null && !saving && !processing && !cropReady,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = VaultSecondary),
                    onClick = { exportDialog = true },
                ) { Text("Export") }
                TextButton(
                    enabled = workingBitmap != null && !saving && !processing && !cropReady && hasUnsavedChanges(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = VaultSecondary),
                    onClick = {
                    val active = editor ?: return@TextButton
                    scope.launch {
                        processing = true
                        runCatching { renderComposite(active) }
                            .onSuccess { onSave(it, EditorSaveMode.REPLACE, null) }
                            .onFailure { cropError = it.message ?: "The edited photo could not be saved." }
                        processing = false
                    }
                }) { Text("Save") }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 184.dp, max = 320.dp)
                    .background(Color.Black).navigationBarsPadding().padding(top = 8.dp, bottom = 4.dp),
            ) {
                when (tool) {
                    EditorTool.TRANSFORM -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        item { EditorActionButton(Icons.Outlined.Crop, "Crop", onClick = ::launchCrop) }
                        item { EditorActionButton(Icons.Outlined.AutoFixHigh, "Auto level") { applyAiTransform("Auto level", ::autoStraightenPhoto) } }
                        item { EditorActionButton(Icons.Outlined.Straighten, "Straighten") { launchGeometry(GeometryMode.STRAIGHTEN) } }
                        item { EditorActionButton(Icons.Outlined.AutoFixHigh, "Horizontal") { launchGeometry(GeometryMode.HORIZONTAL) } }
                        item { EditorActionButton(Icons.Outlined.AutoFixHigh, "Vertical") { launchGeometry(GeometryMode.VERTICAL) } }
                        item { EditorActionButton(Icons.Outlined.Flip, "Flip") { applyGeometry("Flip", Matrix().apply { setScale(-1f, 1f) }, previewScaleX = -1f) } }
                        item { EditorActionButton(Icons.Outlined.Rotate90DegreesCcw, "Rotate") { applyGeometry("Rotate", Matrix().apply { setRotate(90f) }, previewRotation = 90f) } }
                        item { EditorActionButton(Icons.Outlined.AspectRatio, "Ratio", onClick = ::launchCrop) }
                    }
                    EditorTool.FILTERS -> LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        item { EditorActionButton(Icons.Outlined.Tune, "Presets") { presetDialog = true } }
                        items(editorFilters) { choice ->
                            PhotoFilterPreview(workingBitmap, choice, selectedFilter == choice) {
                                if (selectedFilter != choice) {
                                    adjustmentUndoStack = (adjustmentUndoStack + adjustmentState()).takeLast(30)
                                    adjustmentRedoStack = emptyList()
                                    recordEditKind(PhotoEditKind.ADJUSTMENT)
                                    selectedFilter = choice
                                    applyTune()
                                }
                            }
                        }
                    }
                    EditorTool.TONE -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            items(ToneControl.entries) { tone ->
                                ToneActionButton(tone, selectedTone == tone) { selectedTone = tone }
                            }
                        }
                        when (selectedTone) {
                            ToneControl.LIGHT_BALANCE -> PhotoTuneSlider(selectedTone.label, lightBalance, selectedTone.range, { beginToneGesture(); lightBalance = it; applyTune() }, ::commitToneGesture)
                            ToneControl.BRIGHTNESS -> PhotoTuneSlider(selectedTone.label, brightness, selectedTone.range, { beginToneGesture(); brightness = it; applyTune() }, ::commitToneGesture)
                            ToneControl.EXPOSURE -> PhotoTuneSlider(selectedTone.label, exposure, selectedTone.range, { beginToneGesture(); exposure = it; applyTune() }, ::commitToneGesture)
                            ToneControl.CONTRAST -> PhotoTuneSlider(selectedTone.label, contrast, selectedTone.range, { beginToneGesture(); contrast = it; applyTune() }, ::commitToneGesture)
                            ToneControl.HIGHLIGHTS -> PhotoTuneSlider(selectedTone.label, highlights, selectedTone.range, { beginToneGesture(); highlights = it; applyTune() }, ::commitToneGesture)
                            ToneControl.SHADOWS -> PhotoTuneSlider(selectedTone.label, shadows, selectedTone.range, { beginToneGesture(); shadows = it; applyTune() }, ::commitToneGesture)
                            ToneControl.BLACK_POINT -> PhotoTuneSlider(selectedTone.label, blackPoint, selectedTone.range, { beginToneGesture(); blackPoint = it.coerceAtMost(whitePoint - 16f); applyTune() }, ::commitToneGesture)
                            ToneControl.WHITE_POINT -> PhotoTuneSlider(selectedTone.label, whitePoint, selectedTone.range, { beginToneGesture(); whitePoint = it.coerceAtLeast(blackPoint + 16f); applyTune() }, ::commitToneGesture)
                            ToneControl.SATURATION -> PhotoTuneSlider(selectedTone.label, saturation, selectedTone.range, { beginToneGesture(); saturation = it; applyTune() }, ::commitToneGesture)
                            ToneControl.TINT -> PhotoTuneSlider(selectedTone.label, tint, selectedTone.range, { beginToneGesture(); tint = it; applyTune() }, ::commitToneGesture)
                            ToneControl.TEMPERATURE -> PhotoTuneSlider(selectedTone.label, warmth, selectedTone.range, { beginToneGesture(); warmth = it; applyTune() }, ::commitToneGesture)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            EditorActionButton(Icons.Outlined.Details, "Sharpness") { applyAiTransform("Sharpness", ::smartSharpenPhoto) }
                            EditorActionButton(Icons.Outlined.BlurOn, "Definition") { applyAiTransform("Definition", ::improveClarityPhoto) }
                        }
                    }
                    EditorTool.DECORATIONS -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            DecorationMode.entries.forEach { mode ->
                                EditorActionButton(
                                    icon = when (mode) {
                                        DecorationMode.DRAW -> Icons.Outlined.Brush
                                        DecorationMode.MOSAIC -> Icons.Outlined.BlurOn
                                        DecorationMode.STICKERS -> Icons.Outlined.EmojiEmotions
                                        DecorationMode.TEXT -> Icons.Outlined.TextFields
                                    },
                                    label = mode.label,
                                    selected = decorationMode == mode,
                                    onClick = {
                                        decorationMode = mode
                                        editor?.setBrushDrawingMode(mode == DecorationMode.DRAW)
                                        if (mode == DecorationMode.MOSAIC) launchMosaic()
                                    },
                                )
                            }
                        }
                        when (decorationMode) {
                            DecorationMode.DRAW -> DrawControls(
                                color = brushColor,
                                size = brushSize,
                                opacity = brushOpacity,
                                onColor = { brushColor = it; editor?.brushColor = it },
                                onSize = { brushSize = it; editor?.brushSize = it },
                                onOpacity = { brushOpacity = it; editor?.setOpacity(it.toInt()) },
                                onPen = {
                                    brushSize = 12f; brushOpacity = 100f
                                    editor?.brushSize = 12f; editor?.setOpacity(100)
                                    editor?.setBrushDrawingMode(true)
                                },
                                onHighlighter = {
                                    brushSize = 34f; brushOpacity = 36f
                                    editor?.brushSize = 34f; editor?.setOpacity(36)
                                    editor?.setBrushDrawingMode(true)
                                },
                                onEraser = { editor?.brushEraser() },
                                onUndo = ::undoLastEdit,
                                onRedo = ::redoLastEdit,
                            )
                            DecorationMode.MOSAIC -> Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = ::launchMosaic) { Icon(Icons.Outlined.BlurOn, null); Spacer(Modifier.size(8.dp)); Text("Open mosaic brush") }
                                Spacer(Modifier.weight(1f)); Text("Pixelate or paint patterns", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            DecorationMode.TEXT -> Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { textDialog = true }) { Icon(Icons.Outlined.TextFields, null); Spacer(Modifier.size(8.dp)); Text("Add text") }
                                Spacer(Modifier.weight(1f)); Text("Drag, pinch and rotate", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            DecorationMode.STICKERS -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StickerCategory.entries.forEach { category ->
                                        FilterChip(stickerCategory == category, { stickerCategory = category }, label = { Text(category.label) })
                                    }
                                }
                                if (stickerCategory == StickerCategory.BADGES) {
                                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf(
                                            GraphicSticker("NEW", 0xFFFFD60A.toInt()), GraphicSticker("WOW", 0xFF30A7FF.toInt()),
                                            GraphicSticker("LOVE", 0xFFFF4F8B.toInt()), GraphicSticker("SALE", 0xFFFF453A.toInt()),
                                            GraphicSticker("YES!", 0xFF30D158.toInt()), GraphicSticker("LOL", 0xFFBF5AF2.toInt()),
                                            GraphicSticker("2026", 0xFF5E5CE6.toInt()), GraphicSticker("#1", 0xFFFF9F0A.toInt()),
                                        )) { badge ->
                                            val preview = remember(badge) { createGraphicSticker(badge) }
                                            Image(
                                                preview.asImageBitmap(),
                                                badge.text,
                                                Modifier.size(62.dp).clickable { editor?.addImage(preview.copy(Bitmap.Config.ARGB_8888, false)) }.padding(4.dp),
                                            )
                                        }
                                    }
                                } else {
                                    val stickers = when (stickerCategory) {
                                        StickerCategory.EMOTIONS -> listOf("😊", "😂", "😍", "😎", "🥳", "🤩", "😘", "🤍", "👍", "👎", "👏", "🙏", "❤️", "🧡", "💛", "💚", "💙", "💜")
                                        StickerCategory.SYMBOLS -> listOf("✅", "❌", "❗", "❓", "📍", "📌", "💬", "💡", "🎵", "🎬", "📷", "🎨", "⭐", "🌟", "✨", "💫", "🔥", "🎉")
                                        StickerCategory.NATURE -> listOf("☀️", "🌙", "☁️", "🌈", "🌸", "🌹", "🍀", "🦋", "🐾", "🌊", "🌴", "🌵", "🍁", "❄️", "🌍", "🪐", "🐝", "🕊️")
                                        StickerCategory.BADGES -> emptyList()
                                    }
                                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(stickers) { emoji ->
                                            Text(emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.size(54.dp).clickable { editor?.addEmoji(emoji) }.padding(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EditorTool.TOOLS -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        item { EditorActionButton(Icons.Outlined.AutoFixHigh, "Object eraser") { launchAiSelection(AiSelectionEffect.ERASE) } }
                        item { EditorActionButton(Icons.Outlined.Layers, "Vector layers", onClick = ::launchVectorLayers) }
                        item { EditorActionButton(Icons.Outlined.Crop, "Lasso cutout") { launchAiSelection(AiSelectionEffect.STICKER, startWithLasso = true) } }
                        item { EditorActionButton(Icons.Outlined.FilterVintage, "Spot colour") { launchAiSelection(AiSelectionEffect.COLOR_POP) } }
                        item { EditorActionButton(Icons.Outlined.Colorize, "Selective HSL") { launchSelectiveColor() } }
                        item { EditorActionButton(Icons.Outlined.FilterVintage, "3D LUT") { launchCubeLut() } }
                        item { EditorActionButton(Icons.Outlined.Tune, "Tint") { setTool(EditorTool.TONE); selectedTone = ToneControl.TINT } }
                        item { EditorActionButton(Icons.Outlined.ShowChart, "Curves") { launchCurves() } }
                        item { EditorActionButton(Icons.Outlined.Brush, "Local masks") {
                            val active = editor ?: return@EditorActionButton
                            scope.launch {
                                processing = true
                                runCatching { renderComposite(active) }
                                    .onSuccess { localAdjustmentSource = it }
                                    .onFailure { cropError = it.message ?: "Could not prepare local adjustments" }
                                processing = false
                            }
                        } }
                        item { EditorActionButton(Icons.Outlined.Colorize, if (whiteBalancePickerActive) "Tap a neutral area" else "White balance picker") { whiteBalancePickerActive = !whiteBalancePickerActive } }
                        item { EditorActionButton(Icons.Outlined.Details, if (histogramVisible) "Hide histogram" else "Histogram") { histogramVisible = !histogramVisible } }
                        item { EditorActionButton(Icons.Outlined.Exposure, if (clippingVisible) "Hide clipping" else "Clipping") { clippingVisible = !clippingVisible } }
                        item { EditorActionButton(Icons.Outlined.Contrast, if (splitCompareVisible) "Hide split compare" else "Split compare") { splitCompareVisible = !splitCompareVisible } }
                        item { EditorActionButton(Icons.Outlined.InvertColors, "Vibrance") { applyAiTransform("Vibrance", ::vibrancePhoto) } }
                        item { EditorActionButton(Icons.Outlined.Details, "Texture") { applyAiTransform("Texture", ::improveClarityPhoto) } }
                        item { EditorActionButton(Icons.Outlined.WbSunny, "Dehaze") { applyAiTransform("Dehaze", ::dehazePhoto) } }
                        item { EditorActionButton(Icons.Outlined.Tune, "Lens correction", onClick = ::launchLensCorrection) }
                        item { EditorActionButton(Icons.Outlined.FilterVintage, "Vignette") { applyAiTransform("Vignette", ::vignettePhoto) } }
                        item { EditorActionButton(Icons.Outlined.Grain, "Grain") { applyAiTransform("Grain", ::filmGrainPhoto) } }
                    }
                }
                if (splitCompareVisible) {
                    PhotoTuneSlider(
                        label = "Before / After split",
                        value = splitCompareFraction,
                        range = .05f..0.95f,
                        onValue = { splitCompareFraction = it },
                        onFinished = {},
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    EditorTool.entries.forEach { item ->
                        EditorRailButton(
                            icon = when (item) {
                                EditorTool.TRANSFORM -> Icons.Outlined.Crop
                                EditorTool.FILTERS -> Icons.Outlined.FilterVintage
                                EditorTool.TONE -> Icons.Outlined.Tune
                                EditorTool.DECORATIONS -> Icons.Outlined.Brush
                                EditorTool.TOOLS -> Icons.Outlined.AutoFixHigh
                            },
                            label = item.label,
                            selected = tool == item,
                            onClick = {
                                setTool(item)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black), contentAlignment = Alignment.Center) {
            val activeBitmap = workingBitmap
            if (activeBitmap == null) {
                CircularProgressIndicator(color = VaultPrimary)
            } else key(generation, activeBitmap) {
                Box(Modifier.fillMaxSize().onSizeChanged { editorCanvasSize = it }) {
                    AndroidView(
                        factory = { viewContext ->
                            val root = FrameLayout(viewContext).apply {
                                setBackgroundColor(AndroidColor.BLACK)
                                clipChildren = false
                                clipToPadding = false
                            }
                            val view = PhotoEditorView(viewContext).also { photoEditorView ->
                                editorView = photoEditorView
                                photoEditorView.setBackgroundColor(AndroidColor.TRANSPARENT)
                                // Preserve the source aspect ratio. FIT_XY made portrait and wide
                                // photos look subtly stretched in the editor even when the saved
                                // bitmap retained the expected dimensions.
                                photoEditorView.source.scaleType = ImageView.ScaleType.FIT_CENTER
                                photoEditorView.source.setImageBitmap(activeBitmap)
                                applyTune()
                                editor = PhotoEditor.Builder(viewContext, photoEditorView)
                                    .setPinchTextScalable(true)
                                    .setClipSourceImage(true)
                                    .build().also {
                                        it.brushColor = brushColor
                                        it.brushSize = brushSize
                                        it.setOpacity(brushOpacity.toInt())
                                        it.setOnPhotoEditorListener(object : OnPhotoEditorListener {
                                            override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) = Unit
                                            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                                                if (!suppressAnnotationHistory) recordEditKind(PhotoEditKind.ANNOTATION)
                                            }
                                            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                                                if (!suppressAnnotationHistory) recordEditKind(PhotoEditKind.ANNOTATION)
                                            }
                                            override fun onStartViewChangeListener(viewType: ViewType) {
                                                annotationGestureActive = true
                                            }
                                            override fun onStopViewChangeListener(viewType: ViewType) {
                                                if (annotationGestureActive && !suppressAnnotationHistory) recordEditKind(PhotoEditKind.ANNOTATION)
                                                annotationGestureActive = false
                                            }
                                            override fun onTouchSourceImage(event: MotionEvent) = Unit
                                        })
                                    }
                            }
                            root.addView(view, FrameLayout.LayoutParams(activeBitmap.width, activeBitmap.height, Gravity.CENTER))
                            root.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                                val scale = minOf(
                                    (right - left).toFloat() / activeBitmap.width.coerceAtLeast(1),
                                    (bottom - top).toFloat() / activeBitmap.height.coerceAtLeast(1),
                                )
                                view.pivotX = activeBitmap.width / 2f
                                view.pivotY = activeBitmap.height / 2f
                                view.scaleX = scale
                                view.scaleY = scale
                            }
                            root
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            rotationZ = liveGeometryRotation
                            scaleX = liveGeometryScaleX
                        },
                    )
                    if (clippingVisible) PhotoClippingOverlay(activeBitmap)
                    if (histogramVisible) PhotoHistogramOverlay(
                        activeBitmap,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                    )
                    if (splitCompareVisible && bitmap != null) {
                        Image(
                            bitmap.asImageBitmap(),
                            "Original photo on the left side of the comparison",
                            Modifier.fillMaxSize().drawWithContent {
                                clipRect(right = size.width * splitCompareFraction) { this@drawWithContent.drawContent() }
                            },
                            contentScale = ContentScale.Fit,
                        )
                        Box(
                            Modifier.fillMaxSize().drawWithContent {
                                val x = size.width * splitCompareFraction
                                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                                drawContent()
                            },
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Before", color = Color.White, modifier = Modifier.background(Color(0x9919191D), RoundedCornerShape(14.dp)).padding(horizontal = 9.dp, vertical = 4.dp))
                            Text("After", color = Color.White, modifier = Modifier.background(Color(0x9919191D), RoundedCornerShape(14.dp)).padding(horizontal = 9.dp, vertical = 4.dp))
                        }
                    }
                    if (tool == EditorTool.TONE || tool == EditorTool.FILTERS || tool == EditorTool.TOOLS) {
                        Box(
                            Modifier.fillMaxSize().pointerInput(activeBitmap, tool, whiteBalancePickerActive) {
                                detectTapGestures(
                                    onTap = { position -> if (whiteBalancePickerActive) sampleWhiteBalance(position) },
                                    onPress = {
                                    if (whiteBalancePickerActive) {
                                        tryAwaitRelease()
                                    } else {
                                        val compare = scope.launch {
                                            kotlinx.coroutines.delay(180)
                                            comparingOriginal = true
                                        }
                                        tryAwaitRelease()
                                        compare.cancel()
                                        comparingOriginal = false
                                    }
                                })
                            },
                        ) {
                            if (comparingOriginal && bitmap != null) Image(
                                bitmap.asImageBitmap(),
                                "Original photo",
                                Modifier.fillMaxSize().background(Color.Black),
                                contentScale = ContentScale.Fit,
                            )
                            Text(
                                when {
                                    whiteBalancePickerActive -> "Tap a neutral white or gray area"
                                    comparingOriginal -> "Before"
                                    else -> "Hold photo to compare"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                                    .clip(RoundedCornerShape(20.dp)).background(Color(0x9919191D)).padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                    if (processing || saving) {
                        Row(
                            Modifier.align(Alignment.TopStart).padding(12.dp)
                                .background(Color(0xCC242429), RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = VaultPrimary)
                            Text(if (saving) "Saving full resolution…" else "Refining full resolution…", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (textDialog) AlertDialog(
        onDismissRequest = { textDialog = false },
        title = { Text("Add text") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(textValue, { textValue = it.take(160) }, label = { Text("Text") }, minLines = 2)
                ColorChooser(textColor) { textColor = it }
            }
        },
        dismissButton = { TextButton(onClick = { textDialog = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = textValue.isNotBlank(), onClick = {
                val style = TextStyleBuilder().apply {
                    withTextColor(textColor)
                    withTextSize(34f)
                    withGravity(Gravity.CENTER)
                    withTextShadow(4f, 0f, 2f, AndroidColor.BLACK)
                }
                editor?.addText(textValue.trim(), style)
                textValue = ""
                textDialog = false
            }) { Text("Add") }
        },
    )

    if (discardPrompt) AlertDialog(
        onDismissRequest = { discardPrompt = false },
        title = { Text("Save your changes or discard them?") },
        dismissButton = {
            Row {
                TextButton(onClick = { discardPrompt = false }) { Text("Cancel") }
                TextButton(onClick = { discardPrompt = false; onBack() }) { Text("Discard") }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving && !processing, onClick = {
                val active = editor ?: return@TextButton
                scope.launch {
                    processing = true
                    runCatching { renderComposite(active) }
                        .onSuccess { discardPrompt = false; onSave(it, EditorSaveMode.COPY, null) }
                        .onFailure { cropError = it.message ?: "The edited photo could not be saved." }
                    processing = false
                }
            }) { Text("Save copy") }
        },
    )

    cropError?.let { message ->
        AlertDialog(
            onDismissRequest = { cropError = null },
            title = { Text("Editor") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { cropError = null }) { Text("OK") } },
        )
    }

    if (exportDialog) {
        val sourceWidth = workingBitmap?.width ?: 1
        val sourceHeight = workingBitmap?.height ?: 1
        val scale = exportMaxDimension?.let { limit -> minOf(1f, limit.toFloat() / maxOf(sourceWidth, sourceHeight)) } ?: 1f
        val outputWidth = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val outputHeight = (sourceHeight * scale).toInt().coerceAtLeast(1)
        val estimateFactor = when (exportFormat) {
            PhotoOutputFormat.JPEG -> .10 + (exportQuality / 100f) * .32
            PhotoOutputFormat.PNG -> 1.15
            PhotoOutputFormat.WEBP -> .08 + (exportQuality / 100f) * .25
            PhotoOutputFormat.HEIC -> .07 + (exportQuality / 100f) * .22
            PhotoOutputFormat.AVIF -> .05 + (exportQuality / 100f) * .18
        }
        val estimateBytes = (outputWidth.toLong() * outputHeight * 4L * estimateFactor).toLong()
        AlertDialog(
            onDismissRequest = { exportDialog = false },
            title = { Text("Export photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PhotoOutputFormat.entries.filter(ModernImageEncoder::isSupported).forEach { format ->
                            FilterChip(exportFormat == format, { exportFormat = format }, label = { Text(format.label) })
                        }
                    }
                    if (exportFormat != PhotoOutputFormat.PNG) {
                        Text("Quality ${exportQuality.toInt()}%", color = VaultSecondary)
                        Slider(exportQuality, { exportQuality = it }, valueRange = 40f..100f)
                    }
                    Text("Dimensions", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "Original", 4096 to "4K", 2048 to "2K", 1080 to "1080").forEach { (size, label) ->
                            FilterChip(exportMaxDimension == size, { exportMaxDimension = size }, label = { Text(label) })
                        }
                    }
                    FilterChip(
                        selected = exportStripMetadata,
                        onClick = { exportStripMetadata = !exportStripMetadata },
                        label = { Text("Privacy copy: remove metadata") },
                    )
                    Text("$outputWidth × $outputHeight • about ${memoryLabelForExport(estimateBytes)}", color = VaultSecondary)
                    val unavailable = PhotoOutputFormat.entries
                        .filter { it == PhotoOutputFormat.HEIC || it == PhotoOutputFormat.AVIF }
                        .filterNot(ModernImageEncoder::isSupported)
                    if (unavailable.isNotEmpty()) {
                        Text(
                            "${unavailable.joinToString { it.label }} unavailable: this phone has no compatible hardware encoder.",
                            color = VaultSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            dismissButton = { TextButton(onClick = { exportDialog = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    val active = editor ?: return@TextButton
                    val options = PhotoExportOptions(exportFormat, exportQuality.toInt(), exportMaxDimension, exportStripMetadata)
                    exportDialog = false
                    scope.launch {
                        processing = true
                        runCatching {
                            val composite = renderComposite(active)
                            val limit = options.maxDimension
                            if (limit != null && maxOf(composite.width, composite.height) > limit) {
                                val outputScale = limit.toFloat() / maxOf(composite.width, composite.height)
                                Bitmap.createScaledBitmap(composite, (composite.width * outputScale).toInt().coerceAtLeast(1), (composite.height * outputScale).toInt().coerceAtLeast(1), true)
                                    .also { if (it !== composite) composite.recycle() }
                            } else composite
                        }.onSuccess { onSave(it, EditorSaveMode.COPY, options) }
                            .onFailure { cropError = it.message ?: "The photo could not be exported." }
                        processing = false
                    }
                }) { Text("Export") }
            },
        )
    }

    if (presetDialog) {
        PhotoPresetDialog(
            presets = presets,
            current = adjustmentOperation(),
            onDismiss = { presetDialog = false },
            onApply = { operation -> applyAdjustmentOperation(operation); presetDialog = false },
            onSave = { preset ->
                scope.launch {
                    presetStore.save(preset)
                    presets = presetStore.list()
                }
            },
            onDelete = { id ->
                scope.launch {
                    presetStore.delete(id)
                    presets = presetStore.list()
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose { editor?.setBrushDrawingMode(false) }
    }
}

@Composable
private fun PhotoPresetDialog(
    presets: List<PhotoPreset>,
    current: PhotoOperation.Adjustment,
    onDismiss: () -> Unit,
    onApply: (PhotoOperation.Adjustment) -> Unit,
    onSave: (PhotoPreset) -> Unit,
    onDelete: (String) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = presets.firstOrNull { it.id == selectedId }
    var name by remember(selectedId) { mutableStateOf(selected?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo presets") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save the current filter and tone settings, or select a preset to apply, rename, update, or delete it.", color = VaultSecondary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text(if (selected == null) "New preset name" else "Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = name.isNotBlank(), onClick = {
                        onSave(
                            if (selected == null) PhotoPreset(name = name.trim(), adjustment = current)
                            else selected.copy(name = name.trim()),
                        )
                        if (selected == null) name = ""
                    }) { Text(if (selected == null) "Save current" else "Rename") }
                    if (selected != null) {
                        TextButton(onClick = { onSave(selected.copy(name = name.ifBlank { selected.name }, adjustment = current)) }) { Text("Update values") }
                    }
                }
                if (presets.isEmpty()) Text("No saved presets", color = VaultSecondary)
                presets.forEach { preset ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (preset.id == selectedId) Color(0xFF343438) else Color(0xFF242427))
                            .clickable { selectedId = preset.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(preset.name, color = Color.White, modifier = Modifier.weight(1f).padding(12.dp), maxLines = 1)
                        TextButton(onClick = { onApply(preset.adjustment) }) { Text("Apply") }
                        TextButton(onClick = { onDelete(preset.id); if (selectedId == preset.id) selectedId = null }) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun memoryLabelForExport(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(java.util.Locale.ROOT, bytes.toDouble() / (1024L * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(java.util.Locale.ROOT, bytes.toDouble() / (1024L * 1024))
    else -> "%.0f KB".format(java.util.Locale.ROOT, bytes.toDouble() / 1024L)
}

@Composable
private fun AiEditingPanel(
    preview: Bitmap?,
    selectedGroup: AiToolGroup,
    selectedAction: AiEditAction?,
    onGroupSelected: (AiToolGroup) -> Unit,
    onActionSelected: (AiEditAction) -> Unit,
    onApply: () -> Unit,
    onSelectAnything: () -> Unit,
) {
    val actions = when (selectedGroup) {
        AiToolGroup.ENHANCE -> listOf(
            AiEditAction("Auto enhance", "Balanced light and local contrast", ::autoEnhancePhoto),
            AiEditAction("Low-light rescue", "Lift dark scenes without flattening them", ::recoverLowLightPhoto),
            AiEditAction("Remove noise", "Clean grain and color speckles", ::denoisePhoto),
            AiEditAction("Clarity", "Recover fine texture and definition", ::improveClarityPhoto),
            AiEditAction("Smart sharpen", "Crisper edges with restrained halos", ::smartSharpenPhoto),
            AiEditAction("White balance", "Neutralize unwanted color casts", ::whiteBalancePhoto),
            AiEditAction("Soft glow", "Gentle highlight bloom for portraits", ::softGlowPhoto),
            AiEditAction("Lift shadows", "Open dark areas while protecting whites", ::liftShadowsPhoto),
            AiEditAction("Recover highlights", "Bring harsh bright areas under control", ::recoverHighlightsPhoto),
            AiEditAction("Dehaze", "Cut atmospheric haze with local contrast", ::dehazePhoto),
            AiEditAction("HDR detail", "Add controlled micro-contrast and depth", ::hdrDetailPhoto),
        )
        AiToolGroup.RESTORE -> listOf(
            AiEditAction("Restore faded", "Repair color cast and faded contrast", ::restoreFadedPhoto),
            AiEditAction("2× detail", "High-quality Lanczos upscale with sharpening", ::upscalePhoto),
            AiEditAction("Remove noise", "Clean grain and color speckles", ::denoisePhoto),
            AiEditAction("White balance", "Repair aged or incorrect color casts", ::whiteBalancePhoto),
            AiEditAction("Clarity", "Restore definition to a soft capture", ::improveClarityPhoto),
            AiEditAction("Smart sharpen", "Recover edge definition", ::smartSharpenPhoto),
        )
        AiToolGroup.PORTRAIT -> listOf(
            AiEditAction("Background blur", "Neural person mask with natural depth", ::aiPortraitBackgroundBlur, neural = true),
            AiEditAction("Studio light", "Neural subject relighting", ::aiPortraitSpotlight, neural = true),
            AiEditAction("Color pop", "Neural subject isolation", ::aiPortraitColorPop, neural = true),
            AiEditAction("Clean backdrop", "Neural background replacement", ::aiPortraitStudioBackground, neural = true),
            AiEditAction("Natural smooth", "Subject-aware skin treatment", ::aiPortraitSmoothPhoto, neural = true),
            AiEditAction("Warm subject", "Subject-aware selective grading", ::aiPortraitWarmPhoto, neural = true),
            AiEditAction("Night portrait", "Neural subject and scene relighting", ::aiPortraitNightPhoto, neural = true),
        )
        AiToolGroup.DOCUMENT -> listOf(
            AiEditAction("Clean B&W", "High-contrast page for printing or OCR", ::cleanDocumentPhoto),
            AiEditAction("Clean color", "Flatten shadows while preserving ink color", ::cleanColorDocumentPhoto),
            AiEditAction("Text clarity", "Sharpen fine printed detail", ::smartSharpenPhoto),
            AiEditAction("Uneven light", "Recover a page photographed in dim light", ::recoverLowLightPhoto),
            AiEditAction("Remove shadows", "Flatten page lighting and camera shadows", ::removeDocumentShadowsPhoto),
            AiEditAction("Ink boost", "Strong local threshold for faint printing", ::inkBoostDocumentPhoto),
            AiEditAction("Grayscale", "Neutral grayscale without hard thresholding", ::grayscaleDocumentPhoto),
        )
        AiToolGroup.CREATIVE -> listOf(
            AiEditAction("Cinematic", "Teal shadows and warm highlights", ::cinematicPhoto),
            AiEditAction("Matte film", "Lift blacks for a soft film finish", ::mattePhoto),
            AiEditAction("Noir", "High-contrast monochrome treatment", ::noirPhoto),
            AiEditAction("Graphic novel", "Edge-preserving illustrated rendering", ::comicPhoto),
            AiEditAction("Pencil sketch", "Monochrome hand-drawn rendering", ::pencilSketchPhoto),
            AiEditAction("Watercolor", "Painterly full-resolution stylization", ::watercolorPhoto),
            AiEditAction("Pixel art", "Purposeful block-pixel treatment", ::pixelatePhoto),
        )
    }
    val thumbnail = remember(preview) { preview?.scaledEditorPreview(256) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("AI & local tools", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Neural actions are labelled; all processing stays on device", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.background(VaultBlue.copy(alpha = .18f), RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
            ) { Text("ON DEVICE", color = VaultBlue, style = MaterialTheme.typography.labelSmall) }
        }
        Card(
            onClick = onSelectAnything,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222C42)),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(Modifier.size(38.dp).background(VaultBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Select anything", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text("Tap any object, then erase, relight, cut out or replace its background", color = Color(0xFFD5DDF2), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
                Text("OPEN", color = VaultBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(AiToolGroup.entries) { group ->
                FilterChip(
                    selected = selectedGroup == group,
                    onClick = { onGroupSelected(group) },
                    label = { Text(group.label) },
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(actions, key = { it.title }) { action ->
                AiActionPreviewCard(
                    source = thumbnail,
                    action = action,
                    selected = selectedAction?.title == action.title,
                    onClick = { onActionSelected(action) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selectedAction?.description ?: "Choose a live preview; nothing is applied until you confirm",
                color = VaultSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(Modifier.width(10.dp))
            Button(enabled = selectedAction != null, onClick = onApply) {
                Text(if (selectedAction == null) "Select" else "Apply")
            }
        }
    }
}

@Composable
private fun AiActionPreviewCard(
    source: Bitmap?,
    action: AiEditAction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rendered by produceState<Bitmap?>(initialValue = source, source, action.title) {
        value = if (source == null) null else withContext(Dispatchers.Default) {
            runCatching { action.transform(source) }.getOrNull() ?: source
        }
    }
    Card(
        onClick = onClick,
        modifier = Modifier.width(148.dp).height(108.dp).border(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) VaultBlue else Color(0xFF3B3B42),
            shape = RoundedCornerShape(18.dp),
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF29292E)),
    ) {
        Box(Modifier.fillMaxSize()) {
            rendered?.let { bitmap ->
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: CircularProgressIndicator(
                color = VaultBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center).size(24.dp),
            )
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0x10000000), Color(0xEE101014)),
                    ),
                ),
            )
            if (action.neural) Text(
                "NEURAL",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                    .background(VaultBlue.copy(alpha = .88f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 9.dp, vertical = 7.dp)) {
                Text(action.title, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(action.description, color = Color(0xFFD2D2D8), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

internal fun Bitmap.scaledEditorPreview(maxDimension: Int): Bitmap {
    val longest = maxOf(width, height).coerceAtLeast(1)
    if (longest <= maxDimension) return this
    val scale = maxDimension.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
}

@Composable
private fun EditorRailButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier.size(46.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = if (selected) EditorYellow else Color.White, modifier = Modifier.size(29.dp)) }
        Text(label, color = if (selected) EditorYellow else Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/** Borderless One UI editor action; selection is communicated by tint, not a Material chip. */
@Composable
private fun EditorActionButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(82.dp).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(58.dp).background(if (selected) Color.White else Color(0xFF242427), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(28.dp))
        }
        Text(
            label,
            color = if (selected) EditorYellow else Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToneActionButton(tone: ToneControl, selected: Boolean, onClick: () -> Unit) {
    val icon = when (tone) {
        ToneControl.LIGHT_BALANCE -> Icons.Outlined.LightMode
        ToneControl.BRIGHTNESS -> Icons.Outlined.Brightness6
        ToneControl.EXPOSURE -> Icons.Outlined.Exposure
        ToneControl.CONTRAST -> Icons.Outlined.Contrast
        ToneControl.HIGHLIGHTS -> Icons.Outlined.WbSunny
        ToneControl.SHADOWS -> Icons.Outlined.DarkMode
        ToneControl.BLACK_POINT -> Icons.Outlined.DarkMode
        ToneControl.WHITE_POINT -> Icons.Outlined.WbSunny
        ToneControl.SATURATION -> Icons.Outlined.InvertColors
        ToneControl.TINT -> Icons.Outlined.Colorize
        ToneControl.TEMPERATURE -> Icons.Outlined.DeviceThermostat
    }
    Column(
        Modifier.width(82.dp).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(58.dp).background(if (selected) Color.White else Color(0xFF242427), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, tone.label, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(27.dp))
        }
        Text(tone.label, color = if (selected) EditorYellow else Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun PhotoTuneSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$label  ${if (range.start < -2f) "%+.0f".format(value) else "%.2f".format(value)}", color = Color.White, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onValue,
            onValueChangeFinished = onFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = EditorYellow,
                activeTrackColor = EditorYellow,
                inactiveTrackColor = Color(0xFF48484D),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhotoFilterPreview(source: Bitmap?, choice: FilterChoice, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(76.dp).border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0xFF55555C), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (source != null) Image(
                source.asImageBitmap(),
                choice.label,
                Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Crop,
                colorFilter = choice.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) },
            )
        }
        Text(choice.label, color = if (selected) EditorYellow else Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GeometryAdjustmentScreen(
    source: Bitmap,
    mode: GeometryMode,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var amount by remember(mode) { mutableFloatStateOf(0f) }
    var interacting by remember(mode) { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val smallSource = remember(source) { source.scaledEditorPreview(720) }
    val preview by produceState(initialValue = amount to smallSource, smallSource, mode, amount) {
        val requestedAmount = amount
        val rendered = withContext(Dispatchers.Default) {
            runCatching {
                when (mode) {
                    GeometryMode.STRAIGHTEN -> straightenPhoto(smallSource, requestedAmount)
                    GeometryMode.HORIZONTAL -> perspectivePhoto(smallSource, horizontal = requestedAmount)
                    GeometryMode.VERTICAL -> perspectivePhoto(smallSource, vertical = requestedAmount)
                }
            }.getOrElse { smallSource }
        }
        value = requestedAmount to rendered
    }
    BackHandler(onBack = onCancel)
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Cancel ${mode.label}", tint = Color.White) }
                Text(mode.label, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { amount = 0f }) { Text("Reset") }
                TextButton(enabled = !applying, onClick = {
                    scope.launch {
                        applying = true
                        runCatching {
                            withContext(Dispatchers.Default) {
                                when (mode) {
                                    GeometryMode.STRAIGHTEN -> straightenPhoto(source, amount)
                                    GeometryMode.HORIZONTAL -> perspectivePhoto(source, horizontal = amount)
                                    GeometryMode.VERTICAL -> perspectivePhoto(source, vertical = amount)
                                }
                            }
                        }.onSuccess(onDone).onFailure { error = it.message ?: "The adjustment could not be applied" }
                        applying = false
                    }
                }) { Text("Done") }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (mode == GeometryMode.STRAIGHTEN) "%+.1f°".format(amount) else "%+.0f".format(amount * 100f),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = amount,
                    onValueChange = { value ->
                        interacting = true
                        amount = value
                    },
                    onValueChangeFinished = { interacting = false },
                    valueRange = mode.range,
                    colors = SliderDefaults.colors(
                        thumbColor = EditorYellow,
                        activeTrackColor = EditorYellow,
                        inactiveTrackColor = Color(0xFF48484D),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Hold the photo to compare with the original", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black).pointerInput(source, preview) {
                detectTapGestures(onPress = {
                    val reveal = scope.launch { kotlinx.coroutines.delay(120); comparing = true }
                    tryAwaitRelease()
                    reveal.cancel()
                    comparing = false
                })
            },
            contentAlignment = Alignment.Center,
        ) {
            val exactPreviewReady = kotlin.math.abs(preview.first - amount) < .0001f
            val useInteractionSurface = !comparing && (interacting || !exactPreviewReady)
            Image(
                (if (comparing || useInteractionSurface) smallSource else preview.second).asImageBitmap(),
                if (comparing) "Original" else mode.label,
                Modifier.fillMaxSize().graphicsLayer {
                    // Direct manipulation must never wait for the CPU perspective renderer.
                    // This inexpensive GPU transform follows the thumb every frame; the exact
                    // OpenCV result replaces it only when the latest replacement render is ready.
                    if (useInteractionSurface) {
                        rotationZ = if (mode == GeometryMode.STRAIGHTEN) amount else 0f
                        rotationY = if (mode == GeometryMode.HORIZONTAL) amount * 52f else 0f
                        rotationX = if (mode == GeometryMode.VERTICAL) -amount * 52f else 0f
                        scaleX = if (mode == GeometryMode.STRAIGHTEN) 1.06f else 1.02f
                        scaleY = scaleX
                        cameraDistance = 32f
                    }
                },
                contentScale = ContentScale.Fit,
            )
            if (applying) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EditorYellow)
                }
            }
        }
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, title = { Text(mode.label) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } })
    }
}

@Composable
private fun DrawControls(
    color: Int,
    size: Float,
    opacity: Float,
    onColor: (Int) -> Unit,
    onSize: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onPen: () -> Unit,
    onHighlighter: () -> Unit,
    onEraser: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            EditorActionButton(Icons.Outlined.Brush, "Pen", onClick = onPen)
            EditorActionButton(Icons.Outlined.Colorize, "Highlighter", onClick = onHighlighter)
            EditorActionButton(Icons.Outlined.AutoFixHigh, "Eraser", onClick = onEraser)
            EditorActionButton(Icons.Outlined.Undo, "Undo", onClick = onUndo)
            EditorActionButton(Icons.Outlined.Redo, "Redo", onClick = onRedo)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorChooser(color, onColor)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", color = Color.White, modifier = Modifier.size(width = 54.dp, height = 28.dp))
            Slider(
                size,
                onSize,
                valueRange = 2f..72f,
                colors = SliderDefaults.colors(thumbColor = EditorYellow, activeTrackColor = EditorYellow, inactiveTrackColor = Color(0xFF48484D)),
                modifier = Modifier.weight(1f),
            )
            Text("Opacity", color = Color.White, modifier = Modifier.padding(start = 8.dp))
            Slider(
                opacity,
                onOpacity,
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = EditorYellow, activeTrackColor = EditorYellow, inactiveTrackColor = Color(0xFF48484D)),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColorChooser(selected: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        editorColors.forEach { value ->
            Box(
                Modifier.size(if (selected == value) 34.dp else 29.dp)
                    .background(Color(value), CircleShape)
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == value) Box(Modifier.size(10.dp).background(if (value == AndroidColor.WHITE) VaultBlue else Color.White, CircleShape))
            }
        }
    }
}
