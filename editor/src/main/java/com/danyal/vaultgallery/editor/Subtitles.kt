package com.danyal.vaultgallery.editor

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
) {
    init {
        require(startMs >= 0L)
        require(endMs > startMs)
        require(text.isNotBlank())
    }
}

fun parseSrt(contents: String): List<SubtitleCue> {
    val timing = Regex("(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})")
    return contents.replace("\r\n", "\n").trim().split(Regex("\n\\s*\n"))
        .mapNotNull { block ->
            val lines = block.lines().filter(String::isNotBlank)
            val timeIndex = lines.indexOfFirst { timing.containsMatchIn(it) }
            if (timeIndex < 0) return@mapNotNull null
            val match = timing.find(lines[timeIndex]) ?: return@mapNotNull null
            val start = timestampMs(match.groupValues, 1)
            val end = timestampMs(match.groupValues, 5)
            val text = lines.drop(timeIndex + 1).joinToString("\n").trim()
            runCatching { SubtitleCue(startMs = start, endMs = end, text = text) }.getOrNull()
        }
        .sortedWith(compareBy(SubtitleCue::startMs, SubtitleCue::endMs))
}

fun serializeSrt(cues: List<SubtitleCue>): String = cues.sortedWith(compareBy(SubtitleCue::startMs, SubtitleCue::endMs))
    .mapIndexed { index, cue ->
        "${index + 1}\n${formatSrtTime(cue.startMs)} --> ${formatSrtTime(cue.endMs)}\n${cue.text.trim()}"
    }.joinToString("\n\n", postfix = "\n")

private fun timestampMs(groups: List<String>, start: Int): Long =
    groups[start].toLong() * 3_600_000L + groups[start + 1].toLong() * 60_000L +
        groups[start + 2].toLong() * 1_000L + groups[start + 3].toLong()

private fun formatSrtTime(value: Long): String {
    val safe = value.coerceAtLeast(0L)
    val hours = safe / 3_600_000L
    val minutes = safe / 60_000L % 60L
    val seconds = safe / 1_000L % 60L
    val millis = safe % 1_000L
    return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
}
