package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.media.MediaCodecList
import android.os.Build
import androidx.heifwriter.AvifWriter
import androidx.heifwriter.HeifWriter
import java.io.File
import java.io.FileDescriptor
import java.io.RandomAccessFile

/** Device-backed HEIC/AVIF encoder. It never substitutes JPEG bytes under a modern extension. */
internal object ModernImageEncoder {
    private const val ENCODE_TIMEOUT_MS = 30_000L

    fun isSupported(format: PhotoOutputFormat): Boolean = when (format) {
        PhotoOutputFormat.HEIC -> Build.VERSION.SDK_INT >= 29 && hasEncoder("video/hevc")
        PhotoOutputFormat.AVIF -> Build.VERSION.SDK_INT >= 30 && hasEncoder("video/av01")
        else -> true
    }

    private fun hasEncoder(mimeType: String): Boolean = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
        }
    }.getOrDefault(false)

    fun encode(bitmap: Bitmap, format: PhotoOutputFormat, quality: Int, descriptor: FileDescriptor) {
        require(format == PhotoOutputFormat.HEIC || format == PhotoOutputFormat.AVIF)
        check(isSupported(format)) { "${format.label} export is not supported by this device" }
        when (format) {
            PhotoOutputFormat.HEIC -> HeifWriter.Builder(
                descriptor,
                bitmap.width,
                bitmap.height,
                HeifWriter.INPUT_MODE_BITMAP,
            ).setQuality(quality.coerceIn(1, 100))
                .setMaxImages(1)
                .build()
                .use { writer ->
                    writer.start()
                    writer.addBitmap(bitmap)
                    writer.stop(ENCODE_TIMEOUT_MS)
                }
            PhotoOutputFormat.AVIF -> AvifWriter.Builder(
                descriptor,
                bitmap.width,
                bitmap.height,
                AvifWriter.INPUT_MODE_BITMAP,
            ).setQuality(quality.coerceIn(1, 100))
                .setMaxImages(1)
                .build()
                .use { writer ->
                    writer.start()
                    writer.addBitmap(bitmap)
                    writer.stop(ENCODE_TIMEOUT_MS)
                }
            else -> error("Unsupported modern image format")
        }
    }

    fun encodeToPrivateTemporaryFile(
        directory: File,
        bitmap: Bitmap,
        format: PhotoOutputFormat,
        quality: Int,
    ): File {
        val output = File.createTempFile("edited-image-", ".${format.extension}", directory)
        try {
            RandomAccessFile(output, "rw").use { file ->
                file.setLength(0)
                encode(bitmap, format, quality, file.fd)
                file.fd.sync()
            }
            return output
        } catch (error: Throwable) {
            securelyDelete(output)
            throw error
        }
    }

    /** Best-effort overwrite before deletion for plaintext vault intermediates. */
    fun securelyDelete(file: File) {
        runCatching {
            if (file.exists()) RandomAccessFile(file, "rw").use { target ->
                val zeros = ByteArray(64 * 1024)
                var remaining = target.length()
                target.seek(0)
                while (remaining > 0) {
                    val count = minOf(remaining, zeros.size.toLong()).toInt()
                    target.write(zeros, 0, count)
                    remaining -= count
                }
                target.fd.sync()
            }
        }
        file.delete()
    }
}
