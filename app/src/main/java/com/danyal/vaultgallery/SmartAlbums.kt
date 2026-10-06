package com.danyal.vaultgallery

import android.content.Context
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

internal enum class SmartMediaType { ALL, PHOTOS, VIDEOS }

/** A deliberately small, deterministic rule model. Smart albums never move source files. */
internal data class SmartAlbumRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mediaType: SmartMediaType = SmartMediaType.ALL,
    val folderContains: String = "",
    val filenameContains: String = "",
    val indexedText: String = "",
    val favouritesOnly: Boolean = false,
    val withinDays: Int = 0,
    val minimumDurationSeconds: Int = 0,
    val minimumSizeMb: Int = 0,
)

internal object SmartAlbumMatcher {
    internal data class Candidate(
        val id: Long,
        val name: String,
        val kind: MediaKind,
        val bucketName: String,
        val relativePath: String,
        val isFavourite: Boolean,
        val dateTakenMs: Long,
        val durationMs: Long,
        val sizeBytes: Long,
    )

    fun matches(
        media: GalleryMedia,
        rule: SmartAlbumRule,
        nowMs: Long = System.currentTimeMillis(),
        indexedMatches: Set<Long> = emptySet(),
    ): Boolean = matches(
        Candidate(media.id, media.name, media.kind, media.bucketName, media.relativePath, media.isFavourite, media.dateTakenMs, media.durationMs, media.sizeBytes),
        rule,
        nowMs,
        indexedMatches,
    )

    internal fun matches(
        media: Candidate,
        rule: SmartAlbumRule,
        nowMs: Long = System.currentTimeMillis(),
        indexedMatches: Set<Long> = emptySet(),
    ): Boolean {
        if (rule.mediaType == SmartMediaType.PHOTOS && media.kind != MediaKind.IMAGE) return false
        if (rule.mediaType == SmartMediaType.VIDEOS && media.kind != MediaKind.VIDEO) return false
        if (rule.folderContains.isNotBlank() &&
            !media.bucketName.contains(rule.folderContains.trim(), ignoreCase = true) &&
            !media.relativePath.contains(rule.folderContains.trim(), ignoreCase = true)
        ) return false
        if (rule.filenameContains.isNotBlank() && !media.name.contains(rule.filenameContains.trim(), ignoreCase = true)) return false
        if (rule.indexedText.isNotBlank() && media.id !in indexedMatches) return false
        if (rule.favouritesOnly && !media.isFavourite) return false
        if (rule.withinDays > 0) {
            val earliest = nowMs - rule.withinDays.toLong() * 24L * 60L * 60L * 1_000L
            if (media.dateTakenMs < earliest) return false
        }
        if (rule.minimumDurationSeconds > 0 && media.durationMs < rule.minimumDurationSeconds * 1_000L) return false
        if (rule.minimumSizeMb > 0 && media.sizeBytes < rule.minimumSizeMb * 1_048_576L) return false
        return true
    }
}

internal class SmartAlbumRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, 0)

    fun load(): List<SmartAlbumRule> = preferences.getStringSet(KEY_RULES, emptySet()).orEmpty()
        .mapNotNull(::decode)
        .sortedBy { it.name.lowercase() }

    fun save(rule: SmartAlbumRule) {
        val rules = load().filterNot { it.id == rule.id } + rule
        preferences.edit().putStringSet(KEY_RULES, rules.mapTo(LinkedHashSet(), ::encode)).apply()
    }

    fun delete(id: String) {
        preferences.edit().putStringSet(KEY_RULES, load().filterNot { it.id == id }.mapTo(LinkedHashSet(), ::encode)).apply()
    }

    private fun encode(rule: SmartAlbumRule): String = listOf(
        rule.id,
        rule.name,
        rule.mediaType.name,
        rule.folderContains,
        rule.filenameContains,
        rule.indexedText,
        rule.favouritesOnly.toString(),
        rule.withinDays.toString(),
        rule.minimumDurationSeconds.toString(),
        rule.minimumSizeMb.toString(),
    ).joinToString(SEPARATOR) { field ->
        Base64.getUrlEncoder().withoutPadding().encodeToString(field.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decode(encoded: String): SmartAlbumRule? = runCatching {
        val fields = encoded.split(SEPARATOR).map { field ->
            String(Base64.getUrlDecoder().decode(field), StandardCharsets.UTF_8)
        }
        if (fields.size != 10) return@runCatching null
        SmartAlbumRule(
            id = fields[0],
            name = fields[1].trim().take(60).ifBlank { return@runCatching null },
            mediaType = runCatching { SmartMediaType.valueOf(fields[2]) }.getOrDefault(SmartMediaType.ALL),
            folderContains = fields[3].take(80),
            filenameContains = fields[4].take(80),
            indexedText = fields[5].take(120),
            favouritesOnly = fields[6].toBooleanStrictOrNull() ?: false,
            withinDays = fields[7].toIntOrNull()?.coerceIn(0, 3_650) ?: 0,
            minimumDurationSeconds = fields[8].toIntOrNull()?.coerceIn(0, 86_400) ?: 0,
            minimumSizeMb = fields[9].toIntOrNull()?.coerceIn(0, 100_000) ?: 0,
        )
    }.getOrNull()

    private companion object {
        const val PREFERENCES = "smart-albums"
        const val KEY_RULES = "rules-v1"
        const val SEPARATOR = "."
    }
}
