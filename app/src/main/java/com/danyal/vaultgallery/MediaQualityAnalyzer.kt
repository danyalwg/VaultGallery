package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max

internal enum class QualityReviewReason(val label: String) {
    BLURRY("Blurry"),
    DARK("Dark"),
    POSSIBLE_ACCIDENTAL("Possible accidents"),
    OVERSIZED("Large files"),
    LOW_RESOLUTION("Low resolution"),
    BURST("Bursts"),
}

internal data class MediaQualityFinding(
    val media: GalleryMedia,
    val reasons: Set<QualityReviewReason>,
    val sharpness: Double?,
    val meanLuma: Double?,
)

internal data class BurstSignal(
    val id: Long,
    val bucketId: Long,
    val width: Int,
    val height: Int,
    val dateTakenMs: Long,
    val sharpness: Double?,
    val meanLuma: Double?,
)

internal data class BurstReviewGroup(val items: List<GalleryMedia>, val recommendedBestId: Long)

internal data class MediaQualityReport(val findings: List<MediaQualityFinding>, val burstGroups: List<BurstReviewGroup> = emptyList()) {
    fun mediaFor(reason: QualityReviewReason): List<GalleryMedia> = if (reason == QualityReviewReason.BURST) {
        burstGroups.flatMap(BurstReviewGroup::items)
    } else findings.asSequence().filter { reason in it.reasons }.map(MediaQualityFinding::media).toList()

    val bestBurstIds: Set<Long> get() = burstGroups.mapTo(hashSetOf(), BurstReviewGroup::recommendedBestId)
}

/**
 * Conservative, local review suggestions. These signals never make an item eligible for an
 * automatic destructive action; the user must inspect and select every deletion explicitly.
 */
internal suspend fun analyzeMediaQuality(
    context: Context,
    media: List<GalleryMedia>,
    onProgress: ((Int, Int) -> Unit)? = null,
): MediaQualityReport = withContext(Dispatchers.IO) {
    val findings = ArrayList<MediaQualityFinding>()
    val analyzed = ArrayList<MediaQualityFinding>()
    media.forEachIndexed { index, item ->
        coroutineContext.ensureActive()
        var sharpness: Double? = null
        var meanLuma: Double? = null
        if (item.kind == MediaKind.IMAGE) {
            val thumbnail = decodeAnalysisThumbnail(context, item)
            if (thumbnail != null) {
                try {
                    val metrics = imageQualityMetrics(thumbnail)
                    sharpness = metrics.first
                    meanLuma = metrics.second
                    // Deliberately conservative thresholds: this is a review aid, not a verdict.
                } finally {
                    thumbnail.recycle()
                }
            }
        }
        val reasons = qualityReviewReasons(item.width, item.height, item.sizeBytes, item.kind, sharpness, meanLuma)
        val finding = MediaQualityFinding(item, reasons, sharpness, meanLuma)
        analyzed += finding
        if (reasons.isNotEmpty()) findings += finding
        onProgress?.invoke(index + 1, media.size)
    }
    val byId = analyzed.associateBy { it.media.id }
    val bursts = detectBurstGroups(analyzed.filter { it.media.kind == MediaKind.IMAGE }.map { finding ->
        BurstSignal(
            finding.media.id, finding.media.bucketId, finding.media.width, finding.media.height,
            finding.media.dateTakenMs, finding.sharpness, finding.meanLuma,
        )
    }).map { group ->
        BurstReviewGroup(group.mapNotNull { byId[it.id]?.media }, recommendedBestId = bestBurstSignal(group).id)
    }
    MediaQualityReport(findings, bursts)
}

internal fun detectBurstGroups(signals: List<BurstSignal>, maximumGapMs: Long = 1_500L): List<List<BurstSignal>> {
    val groups = ArrayList<List<BurstSignal>>()
    signals.groupBy { Triple(it.bucketId, it.width, it.height) }.values.forEach { bucket ->
        val sorted = bucket.sortedBy(BurstSignal::dateTakenMs)
        var run = ArrayList<BurstSignal>()
        sorted.forEach { signal ->
            if (run.isNotEmpty() && signal.dateTakenMs - run.last().dateTakenMs > maximumGapMs) {
                if (run.size >= 3) groups += run.toList()
                run = ArrayList()
            }
            run += signal
        }
        if (run.size >= 3) groups += run.toList()
    }
    return groups.sortedByDescending { it.size }
}

internal fun bestBurstSignal(group: List<BurstSignal>): BurstSignal = group.maxByOrNull { signal ->
    val exposureScore = signal.meanLuma?.let { 100.0 - kotlin.math.abs(it - 118.0) } ?: 0.0
    (signal.sharpness ?: 0.0) * 2.0 + exposureScore
} ?: error("Burst group is empty")

internal fun qualityReviewReasons(
    width: Int,
    height: Int,
    sizeBytes: Long,
    kind: MediaKind,
    sharpness: Double?,
    meanLuma: Double?,
): Set<QualityReviewReason> = buildSet {
    val pixels = width.toLong().coerceAtLeast(0L) * height.toLong().coerceAtLeast(0L)
    if (pixels in 1 until 1_000_000L || max(width, height) in 1 until 1080) add(QualityReviewReason.LOW_RESOLUTION)
    val oversized = when (kind) {
        MediaKind.IMAGE -> sizeBytes >= 25L * 1024L * 1024L
        MediaKind.VIDEO -> sizeBytes >= 1024L * 1024L * 1024L
    }
    if (oversized) add(QualityReviewReason.OVERSIZED)
    if (sharpness != null && sharpness < 42.0) add(QualityReviewReason.BLURRY)
    if (meanLuma != null && meanLuma < 28.0) add(QualityReviewReason.DARK)
    if (sharpness != null && meanLuma != null && sharpness < 24.0 && meanLuma < 20.0) add(QualityReviewReason.POSSIBLE_ACCIDENTAL)
}

private fun decodeAnalysisThumbnail(context: Context, item: GalleryMedia): Bitmap? = runCatching {
    val largest = max(item.width, item.height).coerceAtLeast(1)
    var sample = 1
    while (largest / sample > 256) sample *= 2
    context.contentResolver.openInputStream(item.uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }
}.getOrNull()

/** Returns variance-of-Laplacian sharpness and mean Rec.601 luma. */
internal fun imageQualityMetrics(source: Bitmap): Pair<Double, Double> {
    val scale = minOf(1f, 128f / max(source.width, source.height).coerceAtLeast(1))
    val width = (source.width * scale).toInt().coerceAtLeast(3)
    val height = (source.height * scale).toInt().coerceAtLeast(3)
    val bitmap = if (width != source.width || height != source.height) Bitmap.createScaledBitmap(source, width, height, true) else source
    return try {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val luma = DoubleArray(pixels.size)
        var lumaSum = 0.0
        pixels.forEachIndexed { index, color ->
            val value = 0.299 * ((color ushr 16) and 0xff) + 0.587 * ((color ushr 8) and 0xff) + 0.114 * (color and 0xff)
            luma[index] = value
            lumaSum += value
        }
        var lapSum = 0.0
        var lapSquaredSum = 0.0
        var count = 0
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            val center = y * width + x
            val laplacian = 4.0 * luma[center] - luma[center - 1] - luma[center + 1] - luma[center - width] - luma[center + width]
            lapSum += laplacian
            lapSquaredSum += laplacian * laplacian
            count++
        }
        val meanLap = if (count == 0) 0.0 else lapSum / count
        val variance = if (count == 0) 0.0 else (lapSquaredSum / count - meanLap * meanLap).coerceAtLeast(0.0)
        variance to (lumaSum / luma.size.coerceAtLeast(1))
    } finally {
        if (bitmap !== source) bitmap.recycle()
    }
}
