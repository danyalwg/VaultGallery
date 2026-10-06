package com.danyal.vaultgallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeEditorInteractionTest {
    @Test
    fun photoCropProducesEveryIntermediateDragState() {
        var crop = Rect(.1f, .1f, .8f, .8f)
        val frames = buildList {
            repeat(24) {
                crop = updateCropRect(
                    original = crop,
                    drag = CropDrag.MOVE,
                    delta = Offset(2f, 1f),
                    size = IntSize(1_000, 1_000),
                    imageWidth = 1_000,
                    imageHeight = 1_000,
                    lockedRatio = null,
                )
                add(crop)
            }
        }

        assertEquals(24, frames.distinct().size)
        assertNotEquals(frames.first(), frames.last())
        assertTrue(frames.zipWithNext().all { (before, after) -> after.left > before.left && after.top > before.top })
    }

    @Test
    fun videoCropProducesEveryIntermediateHandleState() {
        val initial = VideoCropRect(.1f, .1f, .9f, .9f)
        val frames = (1..30).map { step ->
            resizeVideoCrop(initial, VideoCropHandle.TOP_LEFT, step * .004f, step * .003f)
        }

        assertEquals(30, frames.distinct().size)
        assertTrue(frames.zipWithNext().all { (before, after) -> after.left > before.left && after.top > before.top })
        assertEquals(initial.right, frames.last().right, .0001f)
        assertEquals(initial.bottom, frames.last().bottom, .0001f)
    }

    @Test
    fun collageTransformAccumulatesWithoutReleaseCommit() {
        var placement = CollagePlacement(.2f, .2f, .6f, .6f)
        val frames = buildList {
            repeat(20) {
                placement = transformCollagePlacement(placement, .002f, -.001f, 1.004f, .7f)
                add(placement)
            }
        }

        assertEquals(20, frames.distinct().size)
        assertTrue(frames.last().rotation > frames.first().rotation)
        assertTrue(frames.last().right - frames.last().left > frames.first().right - frames.first().left)
    }
}
