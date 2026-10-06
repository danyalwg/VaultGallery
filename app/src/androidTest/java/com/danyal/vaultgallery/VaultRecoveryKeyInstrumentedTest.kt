package com.danyal.vaultgallery

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyal.vaultgallery.security.SecureVault
import com.danyal.vaultgallery.security.SecureStoragePolicy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultRecoveryKeyInstrumentedTest {
    @Test fun recoveryExportRoundTripsAndRejectsWrongPassphrase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = File(context.cacheDir, "recovery-key-test-${System.nanoTime()}").apply { mkdirs() }
        val vault = SecureVault(context, root, "recovery-key-test-${System.nanoTime()}")
        val master = vault.setup("4826".toCharArray())
        try {
            val payload = vault.exportRecoveryKey(master, "correct horse battery staple".toCharArray())
            val recovered = vault.openRecoveryKey(payload, "correct horse battery staple".toCharArray())
            try {
                assertTrue(MessageDigest.isEqual(master, recovered))
            } finally {
                recovered.fill(0)
            }
            val rejected = runCatching { vault.openRecoveryKey(payload, "wrong passphrase value".toCharArray()) }.isFailure
            assertTrue(rejected)
            payload.fill(0)
        } finally {
            master.fill(0)
            vault.closeMetadata()
            root.deleteRecursively()
        }
    }

    @Test fun encryptedBackupRestoresMediaAndMetadataWithoutReplacingExistingItems() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val suffix = System.nanoTime()
        val sourceRoot = File(context.cacheDir, "backup-source-$suffix").apply { mkdirs() }
        val restoredRoot = File(context.cacheDir, "backup-restored-$suffix").apply { mkdirs() }
        val sourceVault = SecureVault(context, sourceRoot, "backup-source-$suffix")
        val restoredVault = SecureVault(context, restoredRoot, "backup-restored-$suffix")
        val sourceMaster = sourceVault.setup("4826".toCharArray())
        val restoredMaster = restoredVault.setup("7391".toCharArray())
        val firstBytes = ByteArray(96 * 1024) { (it % 251).toByte() }
        val secondBytes = ByteArray(127 * 1024) { ((it * 7) % 253).toByte() }
        val firstFile = File(context.cacheDir, "backup-first-$suffix.jpg").apply { writeBytes(firstBytes) }
        val secondFile = File(context.cacheDir, "backup-second-$suffix.mp4").apply { writeBytes(secondBytes) }
        try {
            sourceVault.importGeneratedFile(
                sourceMaster, firstFile, "first.jpg", "image/jpeg", "Camera", 1200, 900,
                storagePolicy = SecureStoragePolicy.ENCRYPTED,
            )
            sourceVault.importGeneratedFile(
                sourceMaster, secondFile, "second.mp4", "video/mp4", "Trips", 1920, 1080, 4000,
                storagePolicy = SecureStoragePolicy.LOCKED_ONLY,
            )
            val encoded = ByteArrayOutputStream()
            val exported = sourceVault.exportEncryptedBackup(
                sourceMaster,
                "correct horse battery staple".toCharArray(),
                encoded,
            )
            assertTrue(exported.itemCount == 2)
            val wrongPassphraseRejected = runCatching {
                restoredVault.restoreEncryptedBackup(
                    restoredMaster,
                    "wrong passphrase value".toCharArray(),
                    ByteArrayInputStream(encoded.toByteArray()),
                )
            }.isFailure
            assertTrue(wrongPassphraseRejected)
            val restored = restoredVault.restoreEncryptedBackup(
                restoredMaster,
                "correct horse battery staple".toCharArray(),
                ByteArrayInputStream(encoded.toByteArray()),
            )
            assertTrue(restored.itemCount == 2)
            val items = restoredVault.list(restoredMaster, includeTrashed = true).associateBy { it.name }
            assertTrue(items.keys == setOf("first.jpg", "second.mp4"))
            listOf("first.jpg" to firstBytes, "second.mp4" to secondBytes).forEach { (name, expected) ->
                val actual = ByteArrayOutputStream()
                restoredVault.streamPlain(restoredMaster, requireNotNull(items[name])) { bytes, count -> actual.write(bytes, 0, count) }
                assertTrue(expected.contentEquals(actual.toByteArray()))
            }
        } finally {
            sourceMaster.fill(0)
            restoredMaster.fill(0)
            sourceVault.closeMetadata()
            restoredVault.closeMetadata()
            firstFile.delete()
            secondFile.delete()
            sourceRoot.deleteRecursively()
            restoredRoot.deleteRecursively()
        }
    }
}
