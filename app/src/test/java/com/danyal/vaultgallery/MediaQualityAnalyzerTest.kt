package com.danyal.vaultgallery

import com.danyal.vaultgallery.data.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaQualityAnalyzerTest {
    @Test fun healthyPhotoIsNotSuggested() {
        assertTrue(
            qualityReviewReasons(
                width = 4032,
                height = 3024,
                sizeBytes = 6L * 1024 * 1024,
                kind = MediaKind.IMAGE,
                sharpness = 180.0,
                meanLuma = 112.0,
            ).isEmpty(),
        )
    }

    @Test fun severeDarkBlurIsReviewOnlyAccidentalSignal() {
        assertEquals(
            setOf(QualityReviewReason.BLURRY, QualityReviewReason.DARK, QualityReviewReason.POSSIBLE_ACCIDENTAL),
            qualityReviewReasons(4032, 3024, 5L * 1024 * 1024, MediaKind.IMAGE, 12.0, 11.0),
        )
    }

    @Test fun largeAndLowResolutionUseDifferentMediaThresholds() {
        val image = qualityReviewReasons(800, 600, 30L * 1024 * 1024, MediaKind.IMAGE, 100.0, 100.0)
        assertTrue(QualityReviewReason.LOW_RESOLUTION in image)
        assertTrue(QualityReviewReason.OVERSIZED in image)
        val video = qualityReviewReasons(1920, 1080, 900L * 1024 * 1024, MediaKind.VIDEO, null, null)
        assertTrue(QualityReviewReason.OVERSIZED !in video)
    }
}
