package com.danyal.vaultgallery

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaDecodeSafetyTest {
    @Test fun acceptsHighResolutionPhonePhoto() {
        assertNull(imageDecodeSafetyIssue(16_320, 12_240, 45L * 1024 * 1024, "image/jpeg"))
    }

    @Test fun rejectsPixelBombHeaders() {
        assertNotNull(imageDecodeSafetyIssue(32_000, 32_000, 128 * 1024, "image/png"))
    }

    @Test fun rejectsNonImagePayload() {
        assertNotNull(imageDecodeSafetyIssue(1920, 1080, 1_000, "application/zip"))
    }
}
