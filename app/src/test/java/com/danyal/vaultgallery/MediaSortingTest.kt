package com.danyal.vaultgallery

import com.danyal.vaultgallery.security.SecureItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSortingTest {
    private val photo = SecureItem("photo", "B.jpg", "image/jpeg", 200, 900, durationMs = 0)
    private val shortVideo = SecureItem("short", "A.mp4", "video/mp4", 100, 2_000, durationMs = 2_000)
    private val longVideo = SecureItem("long", "C.mp4", "video/mp4", 300, 1_000, durationMs = 9_000)
    private val items = listOf(photo, shortVideo, longVideo)

    @Test fun invalidPersistedSortFallsBackSafely() {
        assertEquals(MediaSort.NEWEST, mediaSortFrom("removed-sort-mode"))
    }

    @Test fun secureAndPublicSortVocabularyIncludesUsefulSizeAndDurationOrders() {
        assertEquals(listOf("photo", "long", "short"), items.sortedSecureByPreference(MediaSort.SMALLEST).map { it.id })
        assertEquals(listOf("long", "short", "photo"), items.sortedSecureByPreference(MediaSort.LONGEST).map { it.id })
        assertEquals(listOf("photo", "long", "short"), items.sortedSecureByPreference(MediaSort.MEDIA_TYPE).map { it.id })
    }

    @Test fun nameAndDateOrdersAreDeterministic() {
        assertEquals(listOf("short", "photo", "long"), items.sortedSecureByPreference(MediaSort.NAME_AZ).map { it.id })
        assertEquals(listOf("long", "photo", "short"), items.sortedSecureByPreference(MediaSort.NEWEST).map { it.id })
    }
}
