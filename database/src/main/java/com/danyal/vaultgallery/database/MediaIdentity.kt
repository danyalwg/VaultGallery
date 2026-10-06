package com.danyal.vaultgallery.database

import java.security.MessageDigest

/** Stable identity independent of a MediaStore row id, which may change after a rescan or restore. */
@JvmInline
value class StableMediaId(val value: String)

data class MediaIdentitySeed(
    val volume: String,
    val relativePath: String,
    val displayName: String,
    val sizeBytes: Long,
    val capturedAtMillis: Long,
)

fun MediaIdentitySeed.stableId(): StableMediaId {
    val canonical = listOf(volume, relativePath, displayName, sizeBytes.toString(), capturedAtMillis.toString())
        .joinToString("\u0000") { it.trim().lowercase() }
    val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
    return StableMediaId(digest.joinToString("") { "%02x".format(it) })
}

data class PrivateMediaMetadata(
    val id: StableMediaId,
    val tags: Set<String> = emptySet(),
    val rating: Int = 0,
    val favourite: Boolean = false,
    val ocrText: String = "",
    val transcript: String = "",
    val camera: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

interface PrivateMetadataStore {
    suspend fun get(id: StableMediaId): PrivateMediaMetadata?
    suspend fun put(metadata: PrivateMediaMetadata)
    suspend fun reconcile(current: Collection<MediaIdentitySeed>): Map<StableMediaId, StableMediaId>
}
