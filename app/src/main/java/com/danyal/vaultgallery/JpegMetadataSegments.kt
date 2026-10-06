package com.danyal.vaultgallery

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal data class JpegMetadataSegment(val marker: Int, val bytes: ByteArray, val identity: String)

/** Reads only the JPEG header. Pixel entropy data is never retained in memory. */
internal fun extractPreservableJpegMetadata(input: InputStream, maximumBytes: Int = 8 * 1024 * 1024): List<JpegMetadataSegment> {
    val source = input.buffered()
    if (source.read() != 0xff || source.read() != 0xd8) return emptyList()
    val result = ArrayList<JpegMetadataSegment>()
    var retained = 0
    while (true) {
        var prefix = source.read()
        while (prefix == 0xff) prefix = source.read()
        if (prefix < 0 || prefix == 0xda || prefix == 0xd9) break
        if (prefix in 0xd0..0xd7 || prefix == 0x01) continue
        val high = source.read(); val low = source.read()
        if (high < 0 || low < 0) break
        val length = (high shl 8) or low
        if (length < 2 || length > maximumBytes) break
        val payload = source.readExactOrNull(length - 2) ?: break
        val preservable = prefix in 0xe1..0xef || prefix == 0xfe
        val isExif = prefix == 0xe1 && payload.startsWithAscii("Exif\u0000\u0000")
        if (preservable && !isExif) {
            val bytes = ByteArray(payload.size + 4).also { segment ->
                segment[0] = 0xff.toByte(); segment[1] = prefix.toByte()
                segment[2] = high.toByte(); segment[3] = low.toByte()
                payload.copyInto(segment, 4)
            }
            retained += bytes.size
            if (retained > maximumBytes) break
            result += JpegMetadataSegment(prefix, bytes, metadataIdentity(prefix, payload))
        }
    }
    return result
}

internal fun writeJpegWithPreservedMetadata(
    encoded: InputStream,
    destination: OutputStream,
    preserved: List<JpegMetadataSegment>,
) {
    val source = encoded.buffered()
    require(source.read() == 0xff && source.read() == 0xd8) { "Encoded image is not JPEG" }
    destination.write(byteArrayOf(0xff.toByte(), 0xd8.toByte()))
    preserved.forEach { destination.write(it.bytes) }
    val identities = preserved.mapTo(HashSet()) { it.identity }
    while (true) {
        var marker = source.read()
        require(marker >= 0) { "Truncated JPEG" }
        while (marker == 0xff) marker = source.read()
        require(marker >= 0) { "Truncated JPEG" }
        if (marker == 0xd9) {
            destination.write(byteArrayOf(0xff.toByte(), 0xd9.toByte()))
            break
        }
        if (marker in 0xd0..0xd7 || marker == 0x01) {
            destination.write(byteArrayOf(0xff.toByte(), marker.toByte()))
            continue
        }
        val high = source.read(); val low = source.read()
        require(high >= 0 && low >= 0) { "Truncated JPEG segment" }
        val length = (high shl 8) or low
        require(length >= 2) { "Invalid JPEG segment" }
        val payload = source.readExactOrNull(length - 2) ?: error("Truncated JPEG segment")
        val duplicate = (marker in 0xe1..0xef || marker == 0xfe) && metadataIdentity(marker, payload) in identities
        if (!duplicate) {
            destination.write(byteArrayOf(0xff.toByte(), marker.toByte(), high.toByte(), low.toByte()))
            destination.write(payload)
        }
        if (marker == 0xda) {
            source.copyTo(destination)
            break
        }
    }
}

internal fun mergeJpegMetadata(encoded: ByteArray, preserved: List<JpegMetadataSegment>): ByteArray {
    if (preserved.isEmpty()) return encoded
    val output = ByteArrayOutputStream(encoded.size + preserved.sumOf { it.bytes.size })
    writeJpegWithPreservedMetadata(ByteArrayInputStream(encoded), output, preserved)
    return output.toByteArray()
}

private fun metadataIdentity(marker: Int, payload: ByteArray): String = when {
    marker == 0xe1 && payload.startsWithAscii("http://ns.adobe.com/xap/1.0/") -> "xmp"
    marker == 0xe1 && payload.startsWithAscii("http://ns.adobe.com/xmp/extension/") -> "xmp-extension"
    marker == 0xe2 && payload.startsWithAscii("ICC_PROFILE") -> "icc"
    marker == 0xed && payload.startsWithAscii("Photoshop 3.0") -> "photoshop-iptc"
    marker == 0xfe -> "comment"
    else -> "marker-$marker"
}

private fun ByteArray.startsWithAscii(value: String): Boolean {
    val target = value.toByteArray(Charsets.ISO_8859_1)
    return size >= target.size && target.indices.all { this[it] == target[it] }
}

private fun InputStream.readExactOrNull(count: Int): ByteArray? {
    val output = ByteArray(count)
    var offset = 0
    while (offset < count) {
        val read = read(output, offset, count - offset)
        if (read < 0) return null
        offset += read
    }
    return output
}
