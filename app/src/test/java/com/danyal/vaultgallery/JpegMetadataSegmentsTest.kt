package com.danyal.vaultgallery

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JpegMetadataSegmentsTest {
    @Test
    fun preservesXmpAndIccButDropsStaleExif() {
        val source = jpeg(
            segment(0xe1, "Exif\u0000\u0000old-orientation".toByteArray(Charsets.ISO_8859_1)),
            segment(0xe1, "http://ns.adobe.com/xap/1.0/\u0000<xmp/>".toByteArray(Charsets.ISO_8859_1)),
            segment(0xe2, "ICC_PROFILE\u0000wide-gamut".toByteArray(Charsets.ISO_8859_1)),
        )
        val preserved = extractPreservableJpegMetadata(ByteArrayInputStream(source))
        assertEquals(2, preserved.size)
        assertFalse(preserved.any { it.bytes.toString(Charsets.ISO_8859_1).contains("old-orientation") })

        val encoded = jpeg(segment(0xe2, "ICC_PROFILE\u0000sRGB".toByteArray(Charsets.ISO_8859_1)))
        val merged = mergeJpegMetadata(encoded, preserved)
        val roundTrip = extractPreservableJpegMetadata(ByteArrayInputStream(merged))
        assertEquals(2, roundTrip.size)
        assertEquals(1, roundTrip.count { it.identity == "icc" })
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), merged.copyOfRange(merged.size - 4, merged.size))
    }

    private fun jpeg(vararg segments: ByteArray): ByteArray = buildList<Byte> {
        add(0xff.toByte()); add(0xd8.toByte())
        segments.forEach { addAll(it.toList()) }
        // Minimal SOS header followed by arbitrary entropy bytes. The parser only validates framing.
        addAll(segment(0xda, byteArrayOf(0, 0)).toList())
        addAll(byteArrayOf(1, 2, 3, 4).toList())
    }.toByteArray()

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(0xff.toByte(), marker.toByte(), (length ushr 8).toByte(), length.toByte()) + payload
    }
}
