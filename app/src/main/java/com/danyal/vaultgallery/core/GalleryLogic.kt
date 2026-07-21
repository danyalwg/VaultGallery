package com.danyal.vaultgallery.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GalleryLogic {
    fun durationLabel(durationMs: Long): String {
        val seconds = (durationMs / 1000).coerceAtLeast(0)
        return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }

    fun normalizedAlbumName(name: String): String = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun toggleSelection(selected: Set<Long>, id: Long): Set<Long> = selected.toMutableSet().apply {
        if (!add(id)) remove(id)
    }

    fun slideSelection(
        orderedIds: List<Long>,
        base: Set<Long>,
        anchorId: Long,
        currentId: Long,
        selecting: Boolean,
    ): Set<Long> {
        val start = orderedIds.indexOf(anchorId)
        val end = orderedIds.indexOf(currentId)
        if (start < 0 || end < 0) return base
        val range = orderedIds.subList(minOf(start, end), maxOf(start, end) + 1).toSet()
        return if (selecting) base + range else base - range
    }

    fun dateGroup(timestampMs: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("d MMM", locale).format(Date(timestampMs.coerceAtLeast(0)))
}
