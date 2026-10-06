package com.danyal.vaultgallery.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureVaultCleanupTest {
    @Test
    fun activeTransferTempIsNeverTreatedAsAbandoned() {
        val now = 10L * SECURE_TEMP_FILE_RETENTION_MS

        assertFalse(isAbandonedSecureTemp(now, now))
        assertFalse(isAbandonedSecureTemp(now - SECURE_TEMP_FILE_RETENTION_MS + 1L, now))
    }

    @Test
    fun onlyTempFilesOlderThanTheRetentionWindowAreAbandoned() {
        val now = 10L * SECURE_TEMP_FILE_RETENTION_MS

        assertFalse(isAbandonedSecureTemp(now - SECURE_TEMP_FILE_RETENTION_MS, now))
        assertTrue(isAbandonedSecureTemp(now - SECURE_TEMP_FILE_RETENTION_MS - 1L, now))
    }
}
