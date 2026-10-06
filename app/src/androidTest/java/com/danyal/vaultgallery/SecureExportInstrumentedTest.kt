package com.danyal.vaultgallery

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyal.vaultgallery.security.SecureVault
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureExportInstrumentedTest {
    @Test
    fun encryptedImportCanBeCopiedBackWithItsAlbumName() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolver = context.contentResolver
        val testId = UUID.randomUUID().toString()
        val isolatedRoot = File(context.cacheDir, "secure-vault-test-$testId")
        val vault = SecureVault(context, isolatedRoot, "secure-vault-auth-test-$testId")
        val key = vault.setup("1234".toCharArray())
        var source: android.net.Uri? = null
        var exported: android.net.Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "secure-export-test.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/VaultExportTestSource/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            source = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
            val encoded = ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
            bitmap.recycle()
            resolver.openOutputStream(source, "w")!!.use { it.write(encoded) }
            resolver.update(source, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)

            val stableId = UUID.randomUUID().toString()
            val secureItem = vault.importUri(key, resolver, source, preferredId = stableId)
            assertEquals("VaultExportTestSource", secureItem.albumName)
            val retriedImport = vault.importUri(key, resolver, source, preferredId = stableId)
            assertEquals(secureItem.id, retriedImport.id)
            assertEquals(1, vault.list(key, includeTrashed = true).count { it.id == stableId })
            exported = vault.exportToGallery(key, resolver, secureItem)

            val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE)
            resolver.query(exported, projection, null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Pictures/VaultExportTestSource/", cursor.getString(0))
                assertTrue(cursor.getLong(1) > 0L)
            }
            assertNotNull(resolver.openInputStream(exported)!!.use { android.graphics.BitmapFactory.decodeStream(it) })
            vault.changeSecret(key, "9876".toCharArray())
            val unlocked = vault.unlock("9876".toCharArray())
            assertTrue(unlocked.contentEquals(key))
            unlocked.fill(0)
        } finally {
            source?.let { resolver.delete(it, null, null) }
            exported?.let { resolver.delete(it, null, null) }
            runCatching { vault.reset(key) }
            context.getSharedPreferences("secure-vault-auth-test-$testId", android.content.Context.MODE_PRIVATE).edit().clear().commit()
            isolatedRoot.deleteRecursively()
            key.fill(0)
        }
    }
}
