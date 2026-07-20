package com.danyal.vaultgallery.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GalleryLogicTest {
    @Test fun durationFormatsMinutesAndSeconds() {
        assertEquals("0:00", GalleryLogic.durationLabel(-1))
        assertEquals("1:05", GalleryLogic.durationLabel(65_900))
        assertEquals("120:00", GalleryLogic.durationLabel(7_200_000))
    }

    @Test fun albumNamesNormalizeWithoutMovingFiles() {
        assertEquals("family trip", GalleryLogic.normalizedAlbumName("  Family   Trip "))
    }

    @Test fun selectionToggleIsStable() {
        val selected = GalleryLogic.toggleSelection(emptySet(), 42)
        assertTrue(42 in selected)
        assertFalse(42 in GalleryLogic.toggleSelection(selected, 42))
    }

    @Test fun dateGroupingIsDeterministicForLocale() {
        assertEquals("1 Jan", GalleryLogic.dateGroup(0, Locale.ENGLISH))
    }
}
