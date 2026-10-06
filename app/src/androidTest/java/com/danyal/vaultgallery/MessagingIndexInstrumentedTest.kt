package com.danyal.vaultgallery

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagingIndexInstrumentedTest {
    @Test
    fun crypt15BackupBuildsConversationIndexWithoutTouchingMedia() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val key = InstrumentationRegistry.getArguments().getString("backupKey").orEmpty()
        assumeTrue("Pass a 64-character backupKey instrumentation argument", key.matches(Regex("[0-9a-fA-F]{64}")))
        MessagingBackupKeyStore.save(context, MessagingSource.WHATSAPP, key)
        MessagingIndexCoordinator.enqueue(context, MessagingSource.WHATSAPP)

        val manager = WorkManager.getInstance(context)
        val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3)
        var state: WorkInfo.State? = null
        while (System.currentTimeMillis() < deadline) {
            val current = manager.getWorkInfosByTag(MessagingIndexCoordinator.WORK_TAG).get(15, TimeUnit.SECONDS)
                .filter { it.tags.contains(MessagingIndexWorker::class.java.name) }
                .maxByOrNull { it.runAttemptCount }
            state = current?.state
            if (state?.isFinished == true) break
            Thread.sleep(1_000)
        }
        assertEquals(WorkInfo.State.SUCCEEDED, state)
        val index = MessagingAlbumRepository(context).indexFile(MessagingSource.WHATSAPP)
        assertTrue(index.isFile)
        assertTrue(index.length() > 1_000L)
    }
}
