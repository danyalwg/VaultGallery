package com.danyal.vaultgallery

import android.content.Context
import android.net.Uri
import com.drew.imaging.ImageMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ExtendedMetadataSection(val name: String, val values: List<Pair<String, String>>)

internal suspend fun readExtendedMetadata(context: Context, uri: Uri): List<ExtendedMetadataSection> = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
        ImageMetadataReader.readMetadata(input).directories.mapNotNull { directory ->
            val useful = directory.tags.mapNotNull { tag ->
                val description = runCatching { tag.description }.getOrNull()?.trim().orEmpty()
                if (description.isBlank() || description.length > 1_000) null
                else tag.tagName to description.take(300)
            }.distinctBy { it.first }.take(30)
            ExtendedMetadataSection(directory.name, useful).takeIf { useful.isNotEmpty() }
        }
    }.orEmpty()
}
