package com.danyal.vaultgallery

import android.content.Context
import com.danyal.vaultgallery.data.GalleryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * A byte-for-byte duplicate set. The first item is retained by default in the UI; every later
 * item represents reclaimable storage. No thumbnail, filename, date or dimension heuristic is
 * allowed to make a file destructive-action eligible.
 */
internal data class ExactDuplicateGroup(
    val digest: String,
    val sizeBytes: Long,
    val items: List<GalleryMedia>,
) {
    val reclaimableBytes: Long get() = sizeBytes * (items.size - 1).coerceAtLeast(0)
    val recommendedKeeper: GalleryMedia get() = items.maxWithOrNull(duplicateKeeperComparator) ?: items.first()
    val suggestedRemovalIds: Set<Long> get() = items.asSequence().filterNot { it.id == recommendedKeeper.id }.mapTo(linkedSetOf()) { it.id }
}

/**
 * Exact duplicates contain identical encoded bytes, so "quality" is equal. The safest keeper is
 * instead the copy with the strongest user intent and provenance: favourite first, then Camera,
 * then the oldest copy (usually the original rather than a later download/copy).
 */
internal val duplicateKeeperComparator: Comparator<GalleryMedia> =
    Comparator { first, second ->
        compareDuplicateKeeperSignals(
            first.isFavourite,
            first.bucketName.equals("Camera", true) || first.relativePath.contains("DCIM/Camera", true),
            first.dateTakenMs,
            second.isFavourite,
            second.bucketName.equals("Camera", true) || second.relativePath.contains("DCIM/Camera", true),
            second.dateTakenMs,
        )
    }

internal fun compareDuplicateKeeperSignals(
    firstFavourite: Boolean,
    firstCamera: Boolean,
    firstDateTakenMs: Long,
    secondFavourite: Boolean,
    secondCamera: Boolean,
    secondDateTakenMs: Long,
): Int {
    compareValues(firstFavourite, secondFavourite).takeIf { it != 0 }?.let { return it }
    compareValues(firstCamera, secondCamera).takeIf { it != 0 }?.let { return it }
    // With equal provenance, the oldest copy is most likely the original.
    return secondDateTakenMs.compareTo(firstDateTakenMs)
}

/**
 * Uses file size only to avoid hashing unique files, then streams candidate bytes through
 * SHA-256. Streaming keeps the scan bounded even for multi-gigabyte videos.
 */
internal suspend fun findExactDuplicateGroups(
    context: Context,
    media: List<GalleryMedia>,
): List<ExactDuplicateGroup> = withContext(Dispatchers.IO) {
    media.asSequence()
        .filter { it.sizeBytes > 0L }
        .groupBy(GalleryMedia::sizeBytes)
        .values
        .asSequence()
        .filter { it.size > 1 }
        .flatMap { candidates ->
            candidates.asSequence().mapNotNull { item ->
                coroutineContext.ensureActive()
                val digest = runCatching {
                    context.contentResolver.openInputStream(item.uri)?.use(::sha256Hex)
                }.getOrNull() ?: return@mapNotNull null
                (item.sizeBytes to digest) to item
            }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapNotNull { (identity, items) ->
            items.takeIf { it.size > 1 }?.let {
                ExactDuplicateGroup(identity.second, identity.first, it.sortedByDescending(GalleryMedia::dateTakenMs))
            }
        }
        .sortedByDescending(ExactDuplicateGroup::reclaimableBytes)
        .toList()
}

internal fun sha256Hex(input: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read > 0) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
