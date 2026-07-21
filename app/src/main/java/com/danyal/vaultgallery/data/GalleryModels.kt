package com.danyal.vaultgallery.data

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO }

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
)

data class GalleryAlbum(
    val bucketId: Long,
    val name: String,
    val cover: GalleryMedia,
    val count: Int,
    val bucketIds: Set<Long> = setOf(bucketId),
)
