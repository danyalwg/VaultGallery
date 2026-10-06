package com.danyal.vaultgallery

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3MovieComposerInstrumentedTest {
    private fun media(id: Long, kind: MediaKind, duration: Long = 0) = GalleryMedia(
        id, Uri.parse("content://test/$id"), "item$id", if (kind == MediaKind.VIDEO) "video/mp4" else "image/jpeg", kind,
        1920, 1080, duration, 1, 1, 1, "Camera", false,
    )

    @Test fun compositionContainsSequentialMixedMediaAndLoopedAudioTrack() {
        val plan = buildMovieComposition(
            listOf(media(1, MediaKind.IMAGE), media(2, MediaKind.VIDEO, 4_000)),
            MovieOptions(secondsPerImage = 2, maxVideoSeconds = 3, backgroundAudioUri = Uri.parse("content://test/audio")),
        )
        assertEquals(5_000, plan.durationMs)
        assertEquals(2, plan.composition.sequences.size)
        assertEquals(2, plan.composition.sequences.first().editedMediaItems.size)
        assertTrue(plan.composition.sequences.last().isLooping)
    }
}
