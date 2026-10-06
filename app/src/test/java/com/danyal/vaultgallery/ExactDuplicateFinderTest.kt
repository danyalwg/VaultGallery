package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class ExactDuplicateFinderTest {
    @Test
    fun identicalContentProducesIdenticalFingerprint() {
        val bytes = ByteArray(128 * 1024) { index -> (index * 31).toByte() }

        assertEquals(
            sha256Hex(ByteArrayInputStream(bytes)),
            sha256Hex(ByteArrayInputStream(bytes.copyOf())),
        )
    }

    @Test
    fun sameLengthDifferentContentIsNotDuplicate() {
        val first = ByteArray(4096) { 0x2a }
        val second = ByteArray(4096) { 0x2a }.also { it[it.lastIndex / 2] = 0x2b }

        assertNotEquals(
            sha256Hex(ByteArrayInputStream(first)),
            sha256Hex(ByteArrayInputStream(second)),
        )
    }

    @Test
    fun fingerprintMatchesKnownSha256Vector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex(ByteArrayInputStream("abc".toByteArray())),
        )
    }
}
