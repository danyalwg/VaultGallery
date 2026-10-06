package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionToolsTest {
    @Test
    fun invertOnlyReturnsAvailableUnselectedKeys() {
        assertEquals(setOf(1L, 3L), invertedSelection(listOf(1L, 2L, 3L), setOf(2L, 99L)))
    }

    @Test
    fun rangeIncludesEveryItemBetweenVisualEndpoints() {
        assertEquals(
            linkedSetOf("b", "c", "d", "e"),
            selectionRange(listOf("a", "b", "c", "d", "e", "f"), setOf("b", "e")),
        )
    }

    @Test
    fun rangeUsesOuterEndpointsWhenMoreThanTwoItemsAreSelected() {
        assertEquals(
            linkedSetOf(2, 3, 4, 5, 6),
            selectionRange((1..7).toList(), setOf(2, 4, 6)),
        )
    }
}
