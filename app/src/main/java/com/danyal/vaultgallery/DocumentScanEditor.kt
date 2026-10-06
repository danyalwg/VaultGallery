package com.danyal.vaultgallery

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private val DocumentScanYellow = Color(0xFFFFD60A)

private enum class DocumentTone(val label: String) { ORIGINAL("Original"), COLOR("Clean color"), BLACK_WHITE("B&W") }

private data class DocumentQuad(val points: List<Offset>) {
    fun replacing(index: Int, point: Offset): DocumentQuad {
        val candidate = points.toMutableList().also { it[index] = point }
        return if (isValidDocumentQuad(candidate)) copy(points = candidate) else this
    }

    companion object {
        val Full = DocumentQuad(listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f)))
        val Inset = DocumentQuad(listOf(Offset(.045f, .045f), Offset(.955f, .045f), Offset(.955f, .955f), Offset(.045f, .955f)))
    }
}

/** Rejects crossed, concave, collapsed and near-zero page quadrilaterals while a handle moves. */
private fun isValidDocumentQuad(points: List<Offset>): Boolean {
    if (points.size != 4 || points.any { !it.x.isFinite() || !it.y.isFinite() || it.x !in 0f..1f || it.y !in 0f..1f }) return false
    val crossProducts = points.indices.map { index ->
        val a = points[index]
        val b = points[(index + 1) % 4]
        val c = points[(index + 2) % 4]
        (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
    }
    val consistentlyClockwise = crossProducts.all { it > .0005f }
    val consistentlyCounterClockwise = crossProducts.all { it < -.0005f }
    if (!consistentlyClockwise && !consistentlyCounterClockwise) return false
    val twiceArea = points.indices.sumOf { index ->
        val a = points[index]
        val b = points[(index + 1) % 4]
        (a.x * b.y - b.x * a.y).toDouble()
    }
    if (kotlin.math.abs(twiceArea) < .01) return false
    return points.indices.all { index ->
        val a = points[index]
        val b = points[(index + 1) % 4]
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()) >= .025
    }
}

/**
 * Scans the photo already open in Gallery. This is deliberately separate from the camera-based
 * ML Kit scanner: Scan document means perspective correction of the current media, with visible
 * auto-detected corners that remain fully adjustable by hand.
 */
@Composable
internal fun DocumentScanEditor(
    title: String,
    bitmap: Bitmap?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var quad by remember(bitmap) { mutableStateOf(DocumentQuad.Inset) }
    var tone by remember { mutableStateOf(DocumentTone.COLOR) }
    var detecting by remember(bitmap) { mutableStateOf(bitmap != null) }
    var rendering by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember(bitmap) { androidx.compose.runtime.mutableIntStateOf(-1) }
    val latestQuad by rememberUpdatedState(quad)

    fun autoDetect() {
        val source = bitmap ?: return
        scope.launch {
            detecting = true
            val result = withContext(Dispatchers.Default) { detectDocumentQuad(source) }
            quad = result ?: DocumentQuad.Inset
            message = if (result == null) "No clear page edge found — drag the four corners." else null
            detecting = false
        }
    }

    LaunchedEffect(bitmap) { if (bitmap != null) autoDetect() }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
                Column {
                    Text("Scan document", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(title, color = VaultSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = bitmap != null && !detecting && !rendering && !saving,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DocumentScanYellow, contentColor = Color.Black),
                    onClick = {
                    val source = bitmap ?: return@Button
                    scope.launch {
                        rendering = true
                        runCatching {
                            withContext(Dispatchers.Default) { perspectiveDocument(source, quad, tone) }
                        }.onSuccess(onSave).onFailure { message = it.message ?: "Document scan failed" }
                        rendering = false
                    }
                }) { Text("Save copy") }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(VaultRaised).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentTone.entries.forEach { choice ->
                        FilterChip(
                            selected = tone == choice,
                            onClick = { tone = choice },
                            label = { Text(choice.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF242427),
                                labelColor = Color.White,
                                selectedContainerColor = DocumentScanYellow,
                                selectedLabelColor = Color.Black,
                            ),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = bitmap != null && !detecting, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = DocumentScanYellow), onClick = ::autoDetect) {
                        Icon(Icons.Outlined.AutoFixHigh, null)
                        Text(" Detect edges")
                    }
                    TextButton(enabled = bitmap != null && !detecting, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = DocumentScanYellow), onClick = { quad = DocumentQuad.Full; message = null }) {
                        Icon(Icons.Outlined.CropFree, null)
                        Text(" Full image")
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Drag any yellow corner", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black).onSizeChanged { viewport = it },
            contentAlignment = Alignment.Center,
        ) {
            val source = bitmap
            if (source != null) {
                Image(
                    source.asImageBitmap(),
                    title,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    // Tone selection is a GPU preview and therefore changes in the same frame.
                    // Perspective correction and full cleaning still run once at Save copy.
                    colorFilter = documentPreviewFilter(tone),
                )
                Canvas(Modifier.fillMaxSize().pointerInput(source, viewport) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val rect = fittedImageRect(viewport, source.width, source.height)
                            activeHandle = latestQuad.points.indices.minByOrNull { index ->
                                val point = latestQuad.points[index].toScreen(rect)
                                hypot((point.x - position.x).toDouble(), (point.y - position.y).toDouble())
                            } ?: -1
                        },
                        onDragCancel = { activeHandle = -1 },
                        onDragEnd = { activeHandle = -1 },
                    ) { change, _ ->
                        if (activeHandle >= 0) {
                            change.consume()
                            val rect = fittedImageRect(viewport, source.width, source.height)
                            quad = latestQuad.replacing(activeHandle, change.position.toNormalized(rect))
                        }
                    }
                }) {
                    val rect = fittedImageRect(viewport, source.width, source.height)
                    val corners = quad.points.map { it.toScreen(rect) }
                    val path = Path().apply {
                        moveTo(corners[0].x, corners[0].y)
                        corners.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(path, Color(0x33000000))
                    drawPath(path, Color(0xFFFFC107), style = Stroke(width = 3.dp.toPx()))
                    corners.forEach { corner ->
                        drawCircle(Color.Black.copy(alpha = .62f), 15.dp.toPx(), corner)
                        drawCircle(Color(0xFFFFC107), 10.dp.toPx(), corner)
                        drawCircle(Color.White, 4.dp.toPx(), corner)
                    }
                }
                if (activeHandle in quad.points.indices && viewport != IntSize.Zero) {
                    val density = LocalDensity.current
                    val imageRect = fittedImageRect(viewport, source.width, source.height)
                    val corner = quad.points[activeHandle].toScreen(imageRect)
                    val loupePx = with(density) { 116.dp.toPx() }
                    val gapPx = with(density) { 28.dp.toPx() }
                    val loupeX = (corner.x - loupePx / 2f).coerceIn(6f, (viewport.width - loupePx - 6f).coerceAtLeast(6f))
                    val loupeY = if (corner.y > loupePx + gapPx + 12f) {
                        corner.y - loupePx - gapPx
                    } else {
                        (corner.y + gapPx).coerceAtMost((viewport.height - loupePx - 6f).coerceAtLeast(6f))
                    }
                    DocumentCornerLoupe(
                        source = source,
                        normalizedPoint = quad.points[activeHandle],
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(loupeX.toInt(), loupeY.toInt()) }
                            .zIndex(20f),
                    )
                }
            }
            if (bitmap == null || detecting || rendering || saving) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = DocumentScanYellow)
                        Text(
                            when { bitmap == null -> "Loading photo…"; detecting -> "Finding page edges…"; else -> "Straightening full-resolution page…" },
                            color = Color.White,
                        )
                    }
                }
            }
            message?.let { Text(it, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).background(Color(0xCC35353A)).padding(10.dp)) }
        }
    }
}

private fun documentPreviewFilter(tone: DocumentTone): ColorFilter? = when (tone) {
    DocumentTone.ORIGINAL -> null
    DocumentTone.COLOR -> {
        val contrast = 1.09f
        val offset = 128f * (1f - contrast) + 7f
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            contrast * 1.04f, 0f, 0f, 0f, offset,
            0f, contrast * 1.02f, 0f, 0f, offset,
            0f, 0f, contrast * .98f, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )))
    }
    DocumentTone.BLACK_WHITE -> {
        val contrast = 1.22f
        val offset = 128f * (1f - contrast) + 10f
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            .2126f * contrast, .7152f * contrast, .0722f * contrast, 0f, offset,
            .2126f * contrast, .7152f * contrast, .0722f * contrast, 0f, offset,
            .2126f * contrast, .7152f * contrast, .0722f * contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )))
    }
}

/** A live inspection loupe that follows the active corner while the finger stays on the handle. */
@Composable
private fun DocumentCornerLoupe(source: Bitmap, normalizedPoint: Offset, modifier: Modifier = Modifier) {
    val image = remember(source) { source.asImageBitmap() }
    Canvas(
        modifier.size(116.dp)
            .border(3.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
            .padding(3.dp)
            .background(Color.Black, androidx.compose.foundation.shape.CircleShape)
            .clip(androidx.compose.foundation.shape.CircleShape),
    ) {
        val cropWidth = (source.width * .16f).toInt().coerceIn(32, source.width.coerceAtLeast(32))
        val cropHeight = (source.height * .16f).toInt().coerceIn(32, source.height.coerceAtLeast(32))
        val centerX = (normalizedPoint.x * source.width).toInt()
        val centerY = (normalizedPoint.y * source.height).toInt()
        val left = (centerX - cropWidth / 2).coerceIn(0, (source.width - cropWidth).coerceAtLeast(0))
        val top = (centerY - cropHeight / 2).coerceIn(0, (source.height - cropHeight).coerceAtLeast(0))
        drawImage(
            image = image,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(cropWidth.coerceAtMost(source.width), cropHeight.coerceAtMost(source.height)),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
        )
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xAA000000), 10.dp.toPx(), center)
        drawCircle(Color(0xFFFFC107), 8.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        drawLine(Color.White, Offset(center.x - 18.dp.toPx(), center.y), Offset(center.x + 18.dp.toPx(), center.y), 1.5.dp.toPx())
        drawLine(Color.White, Offset(center.x, center.y - 18.dp.toPx()), Offset(center.x, center.y + 18.dp.toPx()), 1.5.dp.toPx())
    }
}

private fun fittedImageRect(viewport: IntSize, bitmapWidth: Int, bitmapHeight: Int): Rect {
    if (viewport.width <= 0 || viewport.height <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) return Rect.Zero
    val scale = min(viewport.width.toFloat() / bitmapWidth, viewport.height.toFloat() / bitmapHeight)
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (viewport.width - width) / 2f
    val top = (viewport.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun Offset.toScreen(rect: Rect) = Offset(rect.left + x * rect.width, rect.top + y * rect.height)

private fun Offset.toNormalized(rect: Rect) = Offset(
    ((x - rect.left) / rect.width.coerceAtLeast(1f)).coerceIn(0f, 1f),
    ((y - rect.top) / rect.height.coerceAtLeast(1f)).coerceIn(0f, 1f),
)

private fun detectDocumentQuad(source: Bitmap): DocumentQuad? {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    val input = Mat()
    Utils.bitmapToMat(source, input)
    val maximum = max(input.cols(), input.rows()).coerceAtLeast(1)
    val ratio = min(1.0, 1_400.0 / maximum)
    val resized = Mat()
    Imgproc.resize(input, resized, Size(input.cols() * ratio, input.rows() * ratio))
    input.release()
    val gray = Mat()
    val edges = Mat()
    Imgproc.cvtColor(resized, gray, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
    Imgproc.Canny(gray, edges, 55.0, 165.0)
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel)
    kernel.release()
    gray.release()
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
    hierarchy.release()
    edges.release()
    val imageArea = resized.cols().toDouble() * resized.rows()
    var detected: Array<Point>? = null
    for (contour in contours.sortedByDescending(Imgproc::contourArea)) {
        if (Imgproc.contourArea(contour) < imageArea * .10) break
        val curve = MatOfPoint2f(*contour.toArray())
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(curve, approx, Imgproc.arcLength(curve, true) * .02, true)
        val points = approx.toArray()
        val convex = if (points.size == 4) Imgproc.isContourConvex(MatOfPoint(*points)) else false
        curve.release()
        approx.release()
        if (points.size == 4 && convex) {
            detected = points
            break
        }
    }
    contours.forEach(Mat::release)
    val width = resized.cols().toFloat().coerceAtLeast(1f)
    val height = resized.rows().toFloat().coerceAtLeast(1f)
    resized.release()
    return detected?.let(::orderedQuad)?.let { points ->
        DocumentQuad(points.map { Offset((it.x / width).toFloat().coerceIn(0f, 1f), (it.y / height).toFloat().coerceIn(0f, 1f)) })
    }
}

private fun orderedQuad(points: Array<Point>): List<Point> {
    val topLeft = points.minBy { it.x + it.y }
    val bottomRight = points.maxBy { it.x + it.y }
    val topRight = points.maxBy { it.x - it.y }
    val bottomLeft = points.minBy { it.x - it.y }
    return listOf(topLeft, topRight, bottomRight, bottomLeft)
}

private suspend fun perspectiveDocument(source: Bitmap, quad: DocumentQuad, tone: DocumentTone): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    val points = quad.points.map { Point((it.x * (source.width - 1)).toDouble(), (it.y * (source.height - 1)).toDouble()) }
    val width = max(distance(points[0], points[1]), distance(points[3], points[2])).toInt().coerceIn(64, 8_192)
    val height = max(distance(points[0], points[3]), distance(points[1], points[2])).toInt().coerceIn(64, 8_192)
    val input = Mat()
    val output = Mat(height, width, CvType.CV_8UC4)
    val sourcePoints = MatOfPoint2f(*points.toTypedArray())
    val targetPoints = MatOfPoint2f(
        Point(0.0, 0.0), Point((width - 1).toDouble(), 0.0),
        Point((width - 1).toDouble(), (height - 1).toDouble()), Point(0.0, (height - 1).toDouble()),
    )
    Utils.bitmapToMat(source, input)
    val transform = Imgproc.getPerspectiveTransform(sourcePoints, targetPoints)
    Imgproc.warpPerspective(input, output, transform, Size(width.toDouble(), height.toDouble()), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)
    val warped = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { Utils.matToBitmap(output, it) }
    input.release(); output.release(); sourcePoints.release(); targetPoints.release(); transform.release()
    return when (tone) {
        DocumentTone.ORIGINAL -> warped
        DocumentTone.COLOR -> cleanColorDocumentPhoto(warped).also { if (it !== warped) warped.recycle() }
        DocumentTone.BLACK_WHITE -> cleanDocumentPhoto(warped).also { if (it !== warped) warped.recycle() }
    }
}

private fun distance(a: Point, b: Point) = hypot(a.x - b.x, a.y - b.y)
