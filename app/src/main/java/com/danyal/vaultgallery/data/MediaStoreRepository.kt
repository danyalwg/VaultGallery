package com.danyal.vaultgallery.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
        val collection = MediaStore.Files.getContentUri("external")
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
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
        val mediaSelection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selection = if (Build.VERSION.SDK_INT >= 30 && !includeTrashed) {
            "($mediaSelection) AND ${MediaStore.MediaColumns.IS_TRASHED}=0"
        } else mediaSelection
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val result = ArrayList<GalleryMedia>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            val favouriteColumn = if (Build.VERSION.SDK_INT >= 30) cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE) else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val type = cursor.getInt(typeColumn)
                val kind = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaKind.VIDEO else MediaKind.IMAGE
                val baseUri = if (kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val taken = cursor.getLong(takenColumn).takeIf { it > 0 } ?: cursor.getLong(addedColumn) * 1000L
                result += GalleryMedia(
                    id = id,
                    uri = ContentUris.withAppendedId(baseUri, id),
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
                )
            }
        }
        result
    }

    fun albums(media: List<GalleryMedia>): List<GalleryAlbum> = media
        .groupBy { it.bucketId to it.bucketName }
        .mapNotNull { (key, items) -> items.firstOrNull()?.let { GalleryAlbum(key.first, key.second, it, items.size) } }
        .sortedWith(compareByDescending<GalleryAlbum> { it.count }.thenBy { it.name.lowercase() })
}
