package com.danyal.vaultgallery

import org.junit.Assert.assertTrue
import org.junit.Test

class ExactDuplicateRecommendationTest {
    @Test fun favouriteWinsOverFolderHeuristic() {
        assertTrue(compareDuplicateKeeperSignals(true, false, 200, false, true, 100) > 0)
    }

    @Test fun cameraWinsWhenNeitherIsFavourite() {
        assertTrue(compareDuplicateKeeperSignals(false, true, 200, false, false, 100) > 0)
    }

    @Test fun olderCopyWinsWhenProvenanceMatches() {
        assertTrue(compareDuplicateKeeperSignals(false, false, 100, false, false, 200) > 0)
    }
}
