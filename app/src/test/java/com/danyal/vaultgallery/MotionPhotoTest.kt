package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPhotoTest {
    @Test
    fun parsesLegacyGoogleCameraOffsetFromEnd() {
        val xmp = """<rdf:Description GCamera:MicroVideo="1" GCamera:MicroVideoOffset="2400"/>"""
        assertEquals(MotionPhotoSegment(7600, 2400), parseMotionPhotoXmp(10_000, xmp))
    }

    @Test
    fun parsesContainerMotionPhotoItemRegardlessOfAttributeOrder() {
        val xmp = """<Container:Item Item:Length="4096" Item:Mime="video/mp4" Item:Semantic="MotionPhoto">"""
        assertEquals(MotionPhotoSegment(5904, 4096), parseMotionPhotoXmp(10_000, xmp))
    }

    @Test
    fun rejectsInvalidOrMissingSegments() {
        assertNull(parseMotionPhotoXmp(1_000, "<x:xmpmeta/>"))
        assertNull(parseMotionPhotoXmp(1_000, "GCamera:MicroVideoOffset=\"2000\""))
    }
}
