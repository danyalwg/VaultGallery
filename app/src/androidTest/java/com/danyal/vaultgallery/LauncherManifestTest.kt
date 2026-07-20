package com.danyal.vaultgallery

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherManifestTest {
    @Test
    fun bothGalleryExperiencesAreResolvable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageManager = context.packageManager
        val publicInfo = packageManager.getActivityInfo(ComponentName(context, MainGalleryActivity::class.java), 0)
        val secureInfo = packageManager.getActivityInfo(ComponentName(context, SecureGalleryActivity::class.java), 0)

        assertEquals("Gallery", publicInfo.loadLabel(packageManager).toString())
        assertEquals("Secure Gallery", secureInfo.loadLabel(packageManager).toString())
        assertNotNull(packageManager.resolveActivity(Intent(context, MainGalleryActivity::class.java), 0))
        assertNotNull(packageManager.resolveActivity(Intent(context, SecureGalleryActivity::class.java), 0))
    }
}
