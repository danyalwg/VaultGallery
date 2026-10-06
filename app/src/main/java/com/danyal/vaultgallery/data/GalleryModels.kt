package com.danyal.vaultgallery.data

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO }

enum class AlbumKind {
    PHYSICAL,
    SMART,
    WHATSAPP_ROOT,
    WHATSAPP_BUSINESS_ROOT,
    WHATSAPP_CHAT,
    WHATSAPP_BUSINESS_CHAT,
}

data class GalleryMedia(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val kind: MediaKind,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateTakenMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val isFavourite: Boolean,
    val isTrashed: Boolean = false,
    val relativePath: String = "",
)

data class GalleryAlbum(
    val bucketId: Long,
    val name: String,
    val cover: GalleryMedia,
    val count: Int,
    val bucketIds: Set<Long> = setOf(bucketId),
    /** Explicit membership for virtual albums. Physical albums continue to use [bucketIds]. */
    val mediaIds: Set<Long>? = null,
    /** Conversation order for a virtual album; null for physical MediaStore albums. */
    val orderedMedia: List<GalleryMedia>? = null,
    val kind: AlbumKind = AlbumKind.PHYSICAL,
    val subtitle: String? = null,
)
