package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.search.SqliteVectorIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Local, model-free visual retrieval. The descriptor combines a spatial luma grid, colour
 * histograms and edge energy; it is deliberately described as visual similarity, never semantic
 * AI. The index is accessed only through the replaceable search-module VectorIndex contract.
 */
internal suspend fun findVisuallySimilar(
    context: Context,
    source: GalleryMedia,
    candidates: List<GalleryMedia>,
    limit: Int = 80,
    onProgress: ((Int, Int) -> Unit)? = null,
): List<GalleryMedia> = withContext(Dispatchers.IO) {
    require(source.kind == MediaKind.IMAGE) { "Visual similarity currently supports photos" }
    val sourceBitmap = decodeSimilarityThumbnail(context, source) ?: return@withContext emptyList()
    val query = try { visualDescriptor(sourceBitmap) } finally { sourceBitmap.recycle() }
    // Increment the namespace whenever descriptor semantics change so stale vectors are never
    // compared against a new query representation.
    val index = SqliteVectorIndex(context, "public-visual-v2")
    try {
        val byId = HashMap<String, GalleryMedia>()
        val photos = candidates.filter { it.kind == MediaKind.IMAGE && it.id != source.id }
        photos.forEachIndexed { position, item ->
            coroutineContext.ensureActive()
            val id = "${item.id}:${item.dateTakenMs}:${item.sizeBytes}"
            if (!index.contains(id)) decodeSimilarityThumbnail(context, item)?.let { bitmap ->
                try {
                    index.upsert(id, visualDescriptor(bitmap))
                } finally { bitmap.recycle() }
            }
            byId[id] = item
            onProgress?.invoke(position + 1, photos.size)
        }
        index.nearest(query, limit.coerceIn(1, 200))
            .filter { it.score >= .72f }
            .mapNotNull { byId[it.id] }
    } finally { index.close() }
}

private fun decodeSimilarityThumbnail(context: Context, media: GalleryMedia): Bitmap? = runCatching {
    val largest = max(media.width, media.height).coerceAtLeast(1)
    var sample = 1
    while (largest / sample > 256) sample *= 2
    context.contentResolver.openInputStream(media.uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }
}.getOrNull()

/** 8×8 luma layout + 16-bin RGB histograms + 8×8 local edge energy. */
internal fun visualDescriptor(source: Bitmap): FloatArray {
    val bitmap = if (source.width == 32 && source.height == 32) source else Bitmap.createScaledBitmap(source, 32, 32, true)
    return try {
        val pixels = IntArray(32 * 32)
        bitmap.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        val descriptor = FloatArray(64 + 48 + 64)
        val luma = FloatArray(pixels.size)
        pixels.forEachIndexed { index, color ->
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            luma[index] = (.2126f * r + .7152f * g + .0722f * b) / 255f
            descriptor[64 + r / 16] += 1f
            descriptor[80 + g / 16] += 1f
            descriptor[96 + b / 16] += 1f
        }
        for (cellY in 0 until 8) for (cellX in 0 until 8) {
            var sum = 0f
            var edge = 0f
            for (y in cellY * 4 until cellY * 4 + 4) for (x in cellX * 4 until cellX * 4 + 4) {
                val value = luma[y * 32 + x]
                sum += value
                if (x < 31) edge += kotlin.math.abs(value - luma[y * 32 + x + 1])
                if (y < 31) edge += kotlin.math.abs(value - luma[(y + 1) * 32 + x])
            }
            val cell = cellY * 8 + cellX
            descriptor[cell] = sum / 16f
            descriptor[112 + cell] = edge / 24f
        }
        // Normalize each family independently. Raw histogram counts are several orders of
        // magnitude larger than spatial samples; normalizing only the concatenated vector made
        // two images with identical colours but different layouts appear almost identical.
        normalizeDescriptorSegment(descriptor, 0, 64)
        normalizeDescriptorSegment(descriptor, 64, 112)
        normalizeDescriptorSegment(descriptor, 112, 176)
        normalizeDescriptor(descriptor)
    } finally {
        if (bitmap !== source) bitmap.recycle()
    }
}

private fun normalizeDescriptorSegment(values: FloatArray, start: Int, end: Int) {
    var squared = 0.0
    for (index in start until end) squared += values[index] * values[index]
    val magnitude = sqrt(squared).toFloat().coerceAtLeast(1e-6f)
    for (index in start until end) values[index] /= magnitude
}

private fun normalizeDescriptor(values: FloatArray): FloatArray {
    var squared = 0.0
    values.forEach { squared += it * it }
    val magnitude = sqrt(squared).toFloat().coerceAtLeast(1e-6f)
    for (index in values.indices) values[index] /= magnitude
    return values
}
