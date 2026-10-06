package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMediaCapabilitiesTest {
    private fun capabilities(
        decoders: Set<String> = setOf("video/avc", "video/hevc"),
        encoders: Set<String> = setOf("video/avc"),
        freeBytes: Long = 2L * 1024 * 1024 * 1024,
    ) = DeviceMediaCapabilities(
        decoders = decoders,
        encoders = encoders,
        hardwareDecoders = emptySet(),
        hardwareEncoders = emptySet(),
        hdrDecoderTypes = emptySet(),
        totalStorageBytes = 4L * 1024 * 1024 * 1024,
        freeStorageBytes = freeBytes,
        memoryClassMb = 512,
        largeMemoryClassMb = 1024,
        glEsVersion = "3.2",
        aiBackends = emptyList(),
        modelPacks = emptyList(),
        persistentVaultAvailable = true,
    )

    @Test fun supportedVideoCanExport() {
        assertNull(capabilities().videoExportIssue("video/hevc"))
    }

    @Test fun containerMimeDoesNotCauseFalseDecoderRejection() {
        assertNull(capabilities().videoExportIssue("video/mp4"))
        assertNull(capabilities().videoExportIssue("video/webm"))
        assertNull(capabilities().videoExportIssue("video/quicktime"))
    }

    @Test fun missingSourceDecoderIsExplained() {
        assertEquals(
            "This device has no decoder for video/av01. The original video is unchanged.",
            capabilities().videoExportIssue("video/av01"),
        )
    }

    @Test fun missingEncoderIsExplained() {
        assertEquals(
            "This device has no H.264 encoder, so it cannot safely export this edit. The original video is unchanged.",
            capabilities(encoders = emptySet()).videoExportIssue("video/avc"),
        )
    }

    @Test fun lowStorageIsExplained() {
        assertEquals(
            "At least 256 MB of free storage is required to start a video export.",
            capabilities(freeBytes = 128L * 1024 * 1024).videoExportIssue("video/avc"),
        )
    }
}
