package com.danyal.vaultgallery

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaTransferInstrumentedTest {
    @Test
    fun moveKeepsTheOriginalMediaIdentityAndCopyCreatesANewItem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val original = insertTestImage("VaultTransfer_${System.nanoTime()}.png", "Pictures/VaultTransferSource/")
        val created = ArrayList<Uri>().apply { add(original) }
        try {
            val originalId = original.lastPathSegment
            val media = GalleryMedia(
                id = originalId!!.toLong(), uri = original, name = queryName(original), mimeType = "image/png", kind = MediaKind.IMAGE,
                width = 8, height = 8, durationMs = 0, sizeBytes = 0, dateTakenMs = System.currentTimeMillis(),
                bucketId = 0, bucketName = "VaultTransferSource", isFavourite = false, relativePath = "Pictures/VaultTransferSource/",
            )

            val moveId = MediaTransferCoordinator.enqueuePublic(context, listOf(media), "VaultTransferMoved", move = true)
            assertEquals(WorkInfo.State.SUCCEEDED, await(moveId).state)
            assertEquals(originalId, original.lastPathSegment)
            assertEquals("Pictures/VaultTransferMoved/", queryPath(original))

            val before = queryMatching(media.name)
            val copyId = MediaTransferCoordinator.enqueuePublic(context, listOf(media), "VaultTransferCopied", move = false)
            assertEquals(WorkInfo.State.SUCCEEDED, await(copyId).state)
            val after = queryMatching(media.name)
            assertEquals(before.size + 1, after.size)
            val copy = after.firstOrNull { it !in before }
            assertNotNull(copy)
            created += requireNotNull(copy)
            assertEquals("Pictures/VaultTransferCopied/", queryPath(copy))

            val channel = context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel("media-transfers")
            assertNotNull(channel)
            assertTrue(channel.name.isNotBlank())
        } finally {
            created.forEach { resolver.delete(it, null, null) }
        }
    }

    private fun insertTestImage(name: String, relativePath: String): Uri {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: error("Could not create transfer test media")
        resolver.openOutputStream(uri, "w")!!.use { output ->
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
            finally { bitmap.recycle() }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun await(id: java.util.UUID): WorkInfo {
        val manager = WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        repeat(120) {
            val info = requireNotNull(manager.getWorkInfoById(id).get(5, TimeUnit.SECONDS))
            if (info.state.isFinished) return info
            Thread.sleep(250)
        }
        error("Transfer did not finish")
    }

    private fun queryName(uri: Uri): String = queryValue(uri, MediaStore.MediaColumns.DISPLAY_NAME)
    private fun queryPath(uri: Uri): String = queryValue(uri, MediaStore.MediaColumns.RELATIVE_PATH)

    private fun queryValue(uri: Uri, column: String): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
            .query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                check(cursor.moveToFirst())
                cursor.getString(0)
            } ?: error("Test media was not found")
    }

    private fun queryMatching(name: String): List<Uri> {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(name),
            null,
        )?.use { cursor -> buildList { while (cursor.moveToNext()) add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0).toString())) } }
            .orEmpty()
    }
}
