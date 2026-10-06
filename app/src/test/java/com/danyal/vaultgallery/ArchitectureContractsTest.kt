package com.danyal.vaultgallery

import com.danyal.vaultgallery.database.MediaIdentitySeed
import com.danyal.vaultgallery.database.stableId
import com.danyal.vaultgallery.editor.Keyframe
import com.danyal.vaultgallery.editor.PhotoEditProject
import com.danyal.vaultgallery.editor.PhotoOperation
import com.danyal.vaultgallery.editor.TransformKeyframes
import com.danyal.vaultgallery.search.InMemoryVectorIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureContractsTest {
    @Test fun stableIdentityDoesNotDependOnMediaStoreRowId() {
        val seed = MediaIdentitySeed("external_primary", "DCIM/Camera", "IMG_1.jpg", 42, 1000)
        assertEquals(seed.stableId(), seed.copy().stableId())
        assertNotEquals(seed.stableId(), seed.copy(displayName = "IMG_2.jpg").stableId())
    }

    @Test fun photoOperationGraphTruncatesRedoBranch() {
        val first = PhotoOperation.Tone(exposure = .2f)
        val second = PhotoOperation.Hsl(saturation = .3f)
        val replacement = PhotoOperation.Tone(contrast = .4f)
        val project = PhotoEditProject(source = "content://photo").append(first).append(second).undo().append(replacement)
        assertEquals(listOf(first, replacement), project.activeOperations)
        assertEquals(2, project.operations.size)
    }

    @Test fun transformGraphSupportsEveryRequestedKeyframeChannel() {
        val keyframes = TransformKeyframes(
            positionX = listOf(Keyframe(0, 0f)), positionY = listOf(Keyframe(0, 0f)),
            scale = listOf(Keyframe(0, 1f)), rotation = listOf(Keyframe(0, 0f)),
            opacity = listOf(Keyframe(0, 1f)), effectStrength = listOf(Keyframe(0, .5f)),
        )
        assertTrue(listOf(keyframes.positionX, keyframes.positionY, keyframes.scale, keyframes.rotation, keyframes.opacity, keyframes.effectStrength).all { it.isNotEmpty() })
    }

    @Test fun vectorIndexRanksCosineSimilarity() = runBlocking {
        val index = InMemoryVectorIndex()
        index.upsert("same", floatArrayOf(1f, 0f))
        index.upsert("other", floatArrayOf(0f, 1f))
        assertEquals("same", index.nearest(floatArrayOf(1f, 0f), 1).single().id)
    }
}
