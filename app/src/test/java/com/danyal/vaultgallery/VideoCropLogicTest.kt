package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCropLogicTest {
    @Test
    fun squarePresetIsCenteredInLandscapeSource() {
        val crop = centeredVideoCrop(16f / 9f, 1f)
        assertEquals(0f, crop.top, .0001f)
        assertEquals(1f, crop.bottom, .0001f)
        assertEquals(9f / 16f, crop.right - crop.left, .0001f)
        assertEquals(crop.left, 1f - crop.right, .0001f)
    }

    @Test
    fun movingCropPreservesSizeAndStaysInsideFrame() {
        val original = VideoCropRect(.2f, .25f, .7f, .75f)
        val moved = resizeVideoCrop(original, VideoCropHandle.MOVE, .8f, -.8f)
        assertEquals(.5f, moved.right - moved.left, .0001f)
        assertEquals(.5f, moved.bottom - moved.top, .0001f)
        assertEquals(.5f, moved.left, .0001f)
        assertEquals(0f, moved.top, .0001f)
    }

    @Test
    fun cornerDragCannotInvertCropWindow() {
        val resized = resizeVideoCrop(VideoCropRect.Full, VideoCropHandle.TOP_LEFT, 2f, 2f)
        assertTrue(resized.right - resized.left >= .079f)
        assertTrue(resized.bottom - resized.top >= .079f)
        assertTrue(resized.left in 0f..1f && resized.top in 0f..1f)
    }

    @Test
    fun freeformCollageTransformMovesScalesAndRotatesInsideCanvas() {
        val changed = transformCollagePlacement(
            CollagePlacement(.2f, .2f, .6f, .6f),
            panX = .8f,
            panY = -.8f,
            zoom = .5f,
            rotation = 27f,
        )
        assertEquals(.2f, changed.right - changed.left, .0001f)
        assertEquals(.2f, changed.bottom - changed.top, .0001f)
        assertTrue(changed.left >= 0f && changed.top >= 0f && changed.right <= 1f && changed.bottom <= 1f)
        assertEquals(27f, changed.rotation, .0001f)
    }
}
