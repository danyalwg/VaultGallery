package com.danyal.vaultgallery

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyal.vaultgallery.search.SqliteVectorIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqliteVectorIndexInstrumentedTest {
    @Test
    fun vectorsPersistAndRankByCosineSimilarity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val namespace = "test-${System.nanoTime()}"
        SqliteVectorIndex(context, namespace).use { index ->
            index.upsert("near", floatArrayOf(1f, 0f, 0f))
            index.upsert("far", floatArrayOf(0f, 1f, 0f))
            assertTrue(index.contains("near"))
            assertEquals("near", index.nearest(floatArrayOf(.95f, .05f, 0f), 1).single().id)
            index.remove("near")
            index.remove("far")
            assertFalse(index.contains("near"))
            assertFalse(index.contains("far"))
        }
    }
}
