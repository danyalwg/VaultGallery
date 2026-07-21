package com.danyal.vaultgallery.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {
    fun hasAnyAccess(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 34 -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            Build.VERSION.SDK_INT >= 33 -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            else -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun loadMedia(includeTrashed: Boolean = false): List<GalleryMedia> = withContext(Dispatchers.IO) {
        if (!hasAnyAccess()) return@withContext emptyList()
        buildList {
            addAll(queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.IMAGE, includeTrashed))
            addAll(queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO, includeTrashed))
        }.sortedWith(compareByDescending<GalleryMedia> { it.dateTakenMs }.thenByDescending { it.id })
    }

    private fun queryCollection(collection: Uri, kind: MediaKind, includeTrashed: Boolean): List<GalleryMedia> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.Video.VideoColumns.DURATION)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.Images.ImageColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.Images.ImageColumns.BUCKET_ID)
            add(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= 30) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()
        val selection = if (Build.VERSION.SDK_INT >= 30 && !includeTrashed) "${MediaStore.MediaColumns.IS_TRASHED}=0" else null
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val cursorResult = if (Build.VERSION.SDK_INT >= 30 && includeTrashed) {
            context.contentResolver.query(
                collection,
                projection,
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                },
                null,
            )
        } else {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)
        }
        val result = ArrayList<GalleryMedia>()
        cursorResult?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            val favouriteColumn = if (Build.VERSION.SDK_INT >= 30) cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE) else -1
            val trashedColumn = if (Build.VERSION.SDK_INT >= 30) cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED) else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val taken = cursor.getLong(takenColumn).takeIf { it > 0 } ?: cursor.getLong(addedColumn) * 1000L
                result += GalleryMedia(
                    id = if (kind == MediaKind.VIDEO) -id else id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = cursor.getString(nameColumn).orEmpty().ifBlank { "Untitled" },
                    mimeType = cursor.getString(mimeColumn).orEmpty(),
                    kind = kind,
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    durationMs = cursor.getLong(durationColumn),
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateTakenMs = taken,
                    bucketId = cursor.getLong(bucketIdColumn),
                    bucketName = cursor.getString(bucketNameColumn).orEmpty().ifBlank { "Pictures" },
                    isFavourite = favouriteColumn >= 0 && cursor.getInt(favouriteColumn) == 1,
                    isTrashed = trashedColumn >= 0 && cursor.getInt(trashedColumn) == 1,
                )
            }
        }
        return result
    }

    fun albums(media: List<GalleryMedia>): List<GalleryAlbum> = media
        .groupBy { it.bucketId to it.bucketName }
        .mapNotNull { (key, items) -> items.firstOrNull()?.let { GalleryAlbum(key.first, key.second, it, items.size) } }
        .sortedWith(compareByDescending<GalleryAlbum> { it.count }.thenBy { it.name.lowercase() })
}
