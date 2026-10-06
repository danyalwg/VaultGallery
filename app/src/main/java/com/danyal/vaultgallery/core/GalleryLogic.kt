package com.danyal.vaultgallery.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GalleryLogic {
    fun fileSizeLabel(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "%.1f KB".format(Locale.ROOT, bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(Locale.ROOT, bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(Locale.ROOT, bytes / (1024.0 * 1024.0 * 1024.0))
    }

    fun durationLabel(durationMs: Long): String {
        val seconds = (durationMs / 1000).coerceAtLeast(0)
        return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }

    fun normalizedAlbumName(name: String): String = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun toggleSelection(selected: Set<Long>, id: Long): Set<Long> = selected.toMutableSet().apply {
        if (!add(id)) remove(id)
    }

    fun <T> slideSelection(
        orderedIds: List<T>,
        base: Set<T>,
        anchorId: T,
        currentId: T,
        selecting: Boolean,
    ): Set<T> {
        val start = orderedIds.indexOf(anchorId)
        val end = orderedIds.indexOf(currentId)
        if (start < 0 || end < 0) return base
        val range = orderedIds.subList(minOf(start, end), maxOf(start, end) + 1).toSet()
        return if (selecting) base + range else base - range
    }

    fun dateGroup(timestampMs: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("d MMM", locale).format(Date(timestampMs.coerceAtLeast(0)))
}
