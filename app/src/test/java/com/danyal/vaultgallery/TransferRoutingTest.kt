package com.danyal.vaultgallery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferRoutingTest {
    @Test
    fun progressNotificationsAreThrottledWithoutLosingLaterUpdates() {
        val throttle = TransferProgressThrottle(intervalMs = 750L)

        assertTrue(throttle.shouldPublish(1_000L))
        assertFalse(throttle.shouldPublish(1_200L))
        assertFalse(throttle.shouldPublish(1_749L))
        assertTrue(throttle.shouldPublish(1_750L))
        assertFalse(throttle.shouldPublish(2_000L))
        assertTrue(throttle.shouldPublish(2_500L))
    }

    @Test
    fun internalStorageErrorsBecomeActionablePartialTransferMessages() {
        val message = transferFailureMessage(
            rawMessage = "/data/user/0/app/files/item.tmp: open failed: ENOENT",
            completed = 8,
            total = 26,
        )

        assertTrue(message.startsWith("Stopped after 8 of 26 items."))
        assertTrue(message.contains("Completed items are safe"))
        assertFalse(message.contains("/data/user/"))
        assertFalse(message.contains("ENOENT"))
    }

    @Test
    fun mediaMove_excludesSameAlbumOnlyWhenEveryItemHasOneSourceAlbum() {
        assertTrue(
            shouldExcludeTransferDestination(
                destination = "Camera",
                sourceAlbums = setOf("Camera"),
                move = true,
                sourceKind = TransferSourceKind.MEDIA,
            ),
        )
        assertFalse(
            shouldExcludeTransferDestination(
                destination = "Camera",
                sourceAlbums = setOf("Camera", "Downloads"),
                move = true,
                sourceKind = TransferSourceKind.MEDIA,
            ),
        )
    }

    @Test
    fun albumMove_excludesEverySelectedSourceFolder() {
        val selectedFolders = setOf("Camera", "Downloads")

        assertTrue(
            shouldExcludeTransferDestination(
                destination = "Camera",
                sourceAlbums = selectedFolders,
                move = true,
                sourceKind = TransferSourceKind.ALBUMS,
            ),
        )
        assertTrue(
            shouldExcludeTransferDestination(
                destination = "Downloads",
                sourceAlbums = selectedFolders,
                move = true,
                sourceKind = TransferSourceKind.ALBUMS,
            ),
        )
        assertFalse(
            shouldExcludeTransferDestination(
                destination = "Pictures",
                sourceAlbums = selectedFolders,
                move = true,
                sourceKind = TransferSourceKind.ALBUMS,
            ),
        )
    }

    @Test
    fun copiesDoNotHideDestinationsForEitherSourceKind() {
        TransferSourceKind.entries.forEach { sourceKind ->
            assertFalse(
                shouldExcludeTransferDestination(
                    destination = "Camera",
                    sourceAlbums = setOf("Camera"),
                    move = false,
                    sourceKind = sourceKind,
                ),
            )
        }
    }

    @Test
    fun destinationMatchingIsCaseInsensitive() {
        assertTrue(
            shouldExcludeTransferDestination(
                destination = "camera",
                sourceAlbums = setOf("CAMERA"),
                move = true,
                sourceKind = TransferSourceKind.ALBUMS,
            ),
        )
    }

    @Test
    fun mediaStoreRelativePathVerificationIgnoresOnlyHarmlessFormatting() {
        assertTrue(sameRelativePath("Pictures/Camera/", "pictures\\camera"))
        assertFalse(sameRelativePath("Pictures/Camera", "Pictures/Screenshots"))
        assertFalse(sameRelativePath("Pictures/Camera Backup", "Pictures/Camera"))
    }
}
