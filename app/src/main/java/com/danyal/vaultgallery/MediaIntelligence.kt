package com.danyal.vaultgallery

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class RecognizedDocument(
    val text: String,
    val blockCount: Int,
    val words: List<RecognizedWord> = emptyList(),
)

internal data class RecognizedWord(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private val recognizedDocumentCache = object : LruCache<String, RecognizedDocument>(64) {}

internal fun RecognizedDocument.hasReliableText(): Boolean =
    blockCount > 0 && text.count(Char::isLetterOrDigit) >= 8

/** Runs the bundled, on-device Latin-script OCR model. No image or recognized text leaves the phone. */
internal suspend fun recognizeDocumentText(context: Context, uri: Uri, cacheKey: String = uri.toString()): RecognizedDocument {
    synchronized(recognizedDocumentCache) { recognizedDocumentCache.get(cacheKey)?.let { return it } }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val original = recognizeInput(recognizer, InputImage.fromFilePath(context, uri))
        // A contrast-normalized pass fixes the common phone-photo cases where shadows, paper
        // texture or a dim background make the first OCR result incomplete. Keep whichever pass
        // contains more useful text so cleanup can never make a good result worse.
        // A second OpenCV pass is expensive. Run it only when the fast original pass is weak;
        // clear documents should never make every viewer page pay for duplicate recognition.
        val enhanced = if (original.text.count(Char::isLetterOrDigit) < 32 || original.blockCount < 2) {
            withContext(Dispatchers.Default) {
                decodeOcrBitmap(context, uri)?.let { bitmap ->
                    try {
                        val cleaned = cleanDocumentPhoto(bitmap)
                        try { recognizeInput(recognizer, InputImage.fromBitmap(cleaned, 0)) }
                        finally { cleaned.recycle() }
                    } finally { bitmap.recycle() }
                }
            }
        } else null
        val result = listOfNotNull(original, enhanced).maxByOrNull { it.text.count(Char::isLetterOrDigit) + it.blockCount * 8 }
            ?: RecognizedDocument("", 0)
        synchronized(recognizedDocumentCache) { recognizedDocumentCache.put(cacheKey, result) }
        if (cacheKey.startsWith("public:")) {
            cacheKey.substringAfter("public:").substringBefore(':').toLongOrNull()?.let { mediaId ->
                runCatching { GallerySearchIndex(context).use { it.updateOcr(mediaId, result.text) } }
            }
        }
        result
    } finally {
        recognizer.close()
    }
}

/** Secure-gallery OCR overload. The caller supplies an in-memory decoded frame so recognition and
 * the optional cleanup pass never require a plaintext file or shareable URI. */
internal suspend fun recognizeDocumentText(bitmap: android.graphics.Bitmap, cacheKey: String): RecognizedDocument {
    synchronized(recognizedDocumentCache) { recognizedDocumentCache.get(cacheKey)?.let { return it } }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val original = recognizeInput(recognizer, InputImage.fromBitmap(bitmap, 0))
        val enhanced = if (original.text.count(Char::isLetterOrDigit) < 32 || original.blockCount < 2) {
            withContext(Dispatchers.Default) {
                val cleaned = cleanDocumentPhoto(bitmap)
                try { recognizeInput(recognizer, InputImage.fromBitmap(cleaned, 0)) }
                finally { if (cleaned !== bitmap) cleaned.recycle() }
            }
        } else null
        val result = listOfNotNull(original, enhanced).maxByOrNull { it.text.count(Char::isLetterOrDigit) + it.blockCount * 8 }
            ?: RecognizedDocument("", 0)
        synchronized(recognizedDocumentCache) { recognizedDocumentCache.put(cacheKey, result) }
        result
    } finally {
        recognizer.close()
    }
}

private suspend fun recognizeInput(
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    image: InputImage,
): RecognizedDocument = suspendCancellableCoroutine { continuation ->
    recognizer.process(image)
        .addOnSuccessListener { result ->
            if (continuation.isActive) {
                val width = image.width.coerceAtLeast(1).toFloat()
                val height = image.height.coerceAtLeast(1).toFloat()
                val words = result.textBlocks.flatMap { block -> block.lines }.flatMap { line -> line.elements }.mapNotNull { element ->
                    element.boundingBox?.let { bounds ->
                        RecognizedWord(
                            text = element.text,
                            left = (bounds.left / width).coerceIn(0f, 1f),
                            top = (bounds.top / height).coerceIn(0f, 1f),
                            right = (bounds.right / width).coerceIn(0f, 1f),
                            bottom = (bounds.bottom / height).coerceIn(0f, 1f),
                        )
                    }
                }
                continuation.resume(RecognizedDocument(result.text.trim(), result.textBlocks.size, words))
            }
        }
        .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
}

@Composable
internal fun OcrWordOverlay(
    document: RecognizedDocument,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier,
) {
    if (document.words.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return
    Canvas(modifier) {
        val scale = minOf(size.width / imageWidth.toFloat(), size.height / imageHeight.toFloat())
        val renderedWidth = imageWidth * scale
        val renderedHeight = imageHeight * scale
        val origin = Offset((size.width - renderedWidth) / 2f, (size.height - renderedHeight) / 2f)
        document.words.forEach { word ->
            val topLeft = Offset(origin.x + word.left * renderedWidth, origin.y + word.top * renderedHeight)
            val wordSize = Size(
                ((word.right - word.left) * renderedWidth).coerceAtLeast(2f),
                ((word.bottom - word.top) * renderedHeight).coerceAtLeast(2f),
            )
            drawRect(Color(0x55FFD60A), topLeft, wordSize)
            drawRect(Color(0xFFFFD60A), topLeft, wordSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }
    }
}

private fun decodeOcrBitmap(context: Context, uri: Uri): android.graphics.Bitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 2400) sample *= 2
    return context.contentResolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        })
    }
}

@Composable
internal fun SamsungTextRecognitionButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    IconButton(onClick = onClick, modifier = modifier) {
        Text(
            "T",
            color = Color.Black,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.background(Color(0xFFFFC928), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun RecognizedTextSheet(
    document: RecognizedDocument,
    onDismiss: () -> Unit,
    onScanDocument: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Text in image", style = MaterialTheme.typography.headlineSmall)
            Text("${document.blockCount} text block${if (document.blockCount == 1) "" else "s"} detected on device", color = Color.Gray)
            SelectionContainer {
                Text(
                    document.text.ifBlank { "No readable text was found." },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onScanDocument?.let { scan -> TextButton(onClick = scan) { Text("Scan document") } }
                TextButton(enabled = document.text.isNotBlank(), onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, document.text)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share recognized text"))
                }) { Text("Share") }
                TextButton(enabled = document.text.isNotBlank(), onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, document.text)
                        putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    }
                    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Translate text")) }
                        .onFailure { android.widget.Toast.makeText(context, "No text translation app is available", android.widget.Toast.LENGTH_SHORT).show() }
                }) { Text("Translate") }
                Button(enabled = document.text.isNotBlank(), onClick = {
                    clipboard.setText(AnnotatedString(document.text))
                    android.widget.Toast.makeText(context, "Text copied", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Copy all") }
            }
        }
    }
}

/** Persists scanner-owned temporary JPEGs into MediaStore so they survive the scanner activity. */
internal fun persistScannedPages(context: Context, pages: List<Uri>, onComplete: (Int) -> Unit = {}) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO).launch {
        var copied = 0
        pages.forEachIndexed { index, source ->
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}_${index + 1}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Vault Gallery/Scans")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val destination = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Could not create scanned image")
                try {
                    context.contentResolver.openInputStream(source)?.use { input ->
                        context.contentResolver.openOutputStream(destination, "w")?.use(input::copyTo)
                            ?: throw IOException("Could not write scanned image")
                    } ?: throw IOException("Could not read scanner result")
                    if (Build.VERSION.SDK_INT >= 29) context.contentResolver.update(
                        destination,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                    copied++
                } catch (error: Throwable) {
                    context.contentResolver.delete(destination, null, null)
                    throw error
                }
            }
        }
        withContext(Dispatchers.Main) { onComplete(copied) }
    }
}

/** Persists the scanner's multi-page PDF result as a user-owned download. */
internal fun persistScannedPdf(
    context: Context,
    source: Uri,
    pageCount: Int,
    onComplete: (Result<Uri>) -> Unit = {},
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO).launch {
        val result = runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}_${pageCount.coerceAtLeast(1)}p.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Vault Gallery/Scans")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else MediaStore.Files.getContentUri("external")
            val destination = context.contentResolver.insert(collection, values)
                ?: throw IOException("Could not create scanned PDF")
            try {
                context.contentResolver.openInputStream(source)?.use { input ->
                    context.contentResolver.openOutputStream(destination, "w")?.use(input::copyTo)
                        ?: throw IOException("Could not write scanned PDF")
                } ?: throw IOException("Could not read scanner PDF")
                if (Build.VERSION.SDK_INT >= 29) context.contentResolver.update(
                    destination,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
                destination
            } catch (error: Throwable) {
                context.contentResolver.delete(destination, null, null)
                throw error
            }
        }
        withContext(Dispatchers.Main) { onComplete(result) }
    }
}
