package com.danyal.vaultgallery

import com.danyal.vaultgallery.data.MediaKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAlbumMatcherTest {
    private val now = 2_000_000_000_000L
    private val video = SmartAlbumMatcher.Candidate(
        id = 7, name = "Birthday party.mp4",
        kind = MediaKind.VIDEO, durationMs = 70_000,
        sizeBytes = 75L * 1_048_576L, dateTakenMs = now - 2L * 86_400_000L,
        bucketName = "Camera", isFavourite = true,
        relativePath = "DCIM/Camera/",
    )

    @Test fun combinesRulesWithoutMovingMedia() {
        val rule = SmartAlbumRule(
            name = "Recent favourite videos",
            mediaType = SmartMediaType.VIDEOS,
            folderContains = "camera",
            filenameContains = "birthday",
            favouritesOnly = true,
            withinDays = 7,
            minimumDurationSeconds = 60,
            minimumSizeMb = 50,
        )
        assertTrue(SmartAlbumMatcher.matches(video, rule, now))
        assertFalse(SmartAlbumMatcher.matches(video.copy(isFavourite = false), rule, now))
        assertFalse(SmartAlbumMatcher.matches(video.copy(dateTakenMs = now - 8L * 86_400_000L), rule, now))
    }

    @Test fun indexedTextRequiresIndexHit() {
        val rule = SmartAlbumRule(name = "Receipts", indexedText = "receipt")
        assertFalse(SmartAlbumMatcher.matches(video, rule, now, emptySet()))
        assertTrue(SmartAlbumMatcher.matches(video, rule, now, setOf(video.id)))
    }
}
