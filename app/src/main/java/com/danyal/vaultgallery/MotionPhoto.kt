package com.danyal.vaultgallery

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

internal data class MotionPhotoSegment(val offset: Long, val length: Long)
internal data class ExtractedMotionPhoto(val file: File, val durationMs: Long)

private val legacyMotionOffset = Regex(
    "(?:GCamera|Camera):(?:MicroVideoOffset|MotionPhotoOffset)\\s*=\\s*[\"'](\\d+)[\"']",
    RegexOption.IGNORE_CASE,
)
private val containerItem = Regex("<Container:Item\\b[^>]*>", RegexOption.IGNORE_CASE)
private val itemSemantic = Regex("Item:Semantic\\s*=\\s*[\"']MotionPhoto[\"']", RegexOption.IGNORE_CASE)
private val itemLength = Regex("Item:Length\\s*=\\s*[\"'](\\d+)[\"']", RegexOption.IGNORE_CASE)

/** Parses both Google Camera v1 offsets and the newer Motion Photo container item length. */
internal fun parseMotionPhotoXmp(fileSize: Long, xmp: String): MotionPhotoSegment? {
    if (fileSize <= 0L) return null
    val bytesFromEnd = legacyMotionOffset.find(xmp)?.groupValues?.getOrNull(1)?.toLongOrNull()
        ?: containerItem.findAll(xmp).mapNotNull { match ->
            val tag = match.value
            if (!itemSemantic.containsMatchIn(tag)) null
            else itemLength.find(tag)?.groupValues?.getOrNull(1)?.toLongOrNull()
        }.firstOrNull()
        ?: return null
    if (bytesFromEnd < 16L || bytesFromEnd >= fileSize) return null
    return MotionPhotoSegment(fileSize - bytesFromEnd, bytesFromEnd)
}

internal fun detectMotionPhoto(context: Context, uri: Uri): MotionPhotoSegment? {
    val resolver = context.contentResolver
    val declaredSize = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
    } ?: -1L
    resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        val fileSize = descriptor.statSize.takeIf { it > 0L } ?: declaredSize
        if (fileSize <= 0L) return null
        FileInputStream(descriptor.fileDescriptor).use { input ->
            val prefixSize = minOf(fileSize, 768L * 1024L).toInt()
            val prefix = ByteArray(prefixSize)
            var read = 0
            while (read < prefix.size) {
                val count = input.read(prefix, read, prefix.size - read)
                if (count <= 0) break
                read += count
            }
            val xmp = String(prefix, 0, read, StandardCharsets.ISO_8859_1)
            parseMotionPhotoXmp(fileSize, xmp)?.takeIf { hasMp4Header(input, it.offset) }?.let { return it }

            // Samsung/older vendor files may omit standard XMP but still append a valid MP4.
            // Probe a small trailer for a motion marker before scanning a larger tail. Ordinary
            // JPEGs therefore cost a bounded ~1 MB read instead of up to 32 MB every time the
            // viewer opens, which is especially important for the encrypted gallery.
            val probeSize = minOf(fileSize, 256L * 1024L).toInt()
            input.channel.position(fileSize - probeSize)
            val probe = ByteArray(probeSize)
            var probeRead = 0
            while (probeRead < probe.size) {
                val count = input.read(probe, probeRead, probe.size - probeRead)
                if (count <= 0) break
                probeRead += count
            }
            val trailer = String(probe, 0, probeRead, StandardCharsets.ISO_8859_1)
            if (!trailer.contains("MotionPhoto", ignoreCase = true) &&
                !trailer.contains("MicroVideo", ignoreCase = true) &&
                !trailer.contains("MotionPhoto_Data", ignoreCase = true)
            ) return null
            // Scan only a bounded tail and require an ISO-BMFF ftyp box at the candidate start.
            val tailSize = minOf(fileSize, 32L * 1024L * 1024L).toInt()
            val tailStart = fileSize - tailSize
            input.channel.position(tailStart)
            val tail = ByteArray(tailSize)
            var tailRead = 0
            while (tailRead < tail.size) {
                val count = input.read(tail, tailRead, tail.size - tailRead)
                if (count <= 0) break
                tailRead += count
            }
            for (index in 4 until tailRead - 8) {
                if (tail[index] == 'f'.code.toByte() && tail[index + 1] == 't'.code.toByte() &&
                    tail[index + 2] == 'y'.code.toByte() && tail[index + 3] == 'p'.code.toByte()
                ) {
                    val start = tailStart + index - 4L
                    val length = fileSize - start
                    if (length >= 16L) return MotionPhotoSegment(start, length)
                }
            }
        }
    }
    return null
}

private fun hasMp4Header(input: FileInputStream, offset: Long): Boolean {
    if (offset < 0L) return false
    input.channel.position(offset)
    val header = ByteArray(12)
    var read = 0
    while (read < header.size) {
        val count = input.read(header, read, header.size - read)
        if (count <= 0) break
        read += count
    }
    return read >= 8 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
        header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
}

internal fun extractMotionPhoto(context: Context, uri: Uri, cacheKey: String): ExtractedMotionPhoto? {
    val segment = detectMotionPhoto(context, uri) ?: return null
    val root = File(context.cacheDir, "motion-photo-cache").apply { mkdirs() }
    val output = File(root, "${cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")}.mp4")
    context.contentResolver.openInputStream(uri)?.use { input ->
        var remainingSkip = segment.offset
        while (remainingSkip > 0L) {
            val skipped = input.skip(remainingSkip)
            if (skipped > 0L) remainingSkip -= skipped
            else if (input.read() >= 0) remainingSkip-- else return null
        }
        FileOutputStream(output).use { sink ->
            val buffer = ByteArray(128 * 1024)
            var remaining = segment.length
            while (remaining > 0L) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (count <= 0) break
                sink.write(buffer, 0, count)
                remaining -= count
            }
            if (remaining != 0L) {
                output.delete()
                return null
            }
        }
    } ?: return null
    val duration = runCatching {
        MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(output.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        }
    }.getOrDefault(0L)
    return ExtractedMotionPhoto(output, duration)
}

internal fun extractMotionPhoto(context: Context, source: File, cacheKey: String): ExtractedMotionPhoto? {
    val segment = detectMotionPhoto(source) ?: return null
    val root = File(context.cacheDir, "motion-photo-cache").apply { mkdirs() }
    val output = File(root, "${cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")}.mp4")
    RandomAccessFile(source, "r").use { input ->
        input.seek(segment.offset)
        FileOutputStream(output).use { sink ->
            val buffer = ByteArray(128 * 1024)
            var remaining = segment.length
            while (remaining > 0L) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (count <= 0) break
                sink.write(buffer, 0, count)
                remaining -= count
            }
            if (remaining != 0L) {
                output.delete()
                return null
            }
        }
    }
    return ExtractedMotionPhoto(output, motionDuration(output))
}

private fun detectMotionPhoto(source: File): MotionPhotoSegment? {
    if (!source.isFile || source.length() <= 0L) return null
    val fileSize = source.length()
    RandomAccessFile(source, "r").use { input ->
        val prefix = ByteArray(minOf(fileSize, 768L * 1024L).toInt())
        input.readFully(prefix)
        val xmp = String(prefix, StandardCharsets.ISO_8859_1)
        parseMotionPhotoXmp(fileSize, xmp)?.takeIf { segment ->
            input.seek(segment.offset + 4L)
            val header = ByteArray(4)
            input.read(header) == 4 && header.contentEquals(byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()))
        }?.let { return it }
        val probeSize = minOf(fileSize, 256L * 1024L).toInt()
        input.seek(fileSize - probeSize)
        val probe = ByteArray(probeSize)
        input.readFully(probe)
        val trailer = String(probe, StandardCharsets.ISO_8859_1)
        if (!trailer.contains("MotionPhoto", ignoreCase = true) &&
            !trailer.contains("MicroVideo", ignoreCase = true) &&
            !trailer.contains("MotionPhoto_Data", ignoreCase = true)
        ) return null
        val tailSize = minOf(fileSize, 32L * 1024L * 1024L).toInt()
        val tailStart = fileSize - tailSize
        input.seek(tailStart)
        val tail = ByteArray(tailSize)
        input.readFully(tail)
        for (index in 4 until tail.size - 8) {
            if (tail[index] == 'f'.code.toByte() && tail[index + 1] == 't'.code.toByte() &&
                tail[index + 2] == 'y'.code.toByte() && tail[index + 3] == 'p'.code.toByte()
            ) {
                val start = tailStart + index - 4L
                return MotionPhotoSegment(start, fileSize - start)
            }
        }
    }
    return null
}

private fun motionDuration(file: File): Long = runCatching {
    MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }
}.getOrDefault(0L)
