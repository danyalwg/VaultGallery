package com.danyal.vaultgallery

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemIntegrationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun assertVaultHandles(intent: Intent, description: String) {
        val matches = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(
            "$description did not resolve to Vault Gallery; found ${matches.map { it.activityInfo.packageName }}",
            matches.any { it.activityInfo.packageName == context.packageName },
        )
    }

    @Test
    fun advertisesAllSupportedSystemGalleryContracts() {
        assertVaultHandles(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
            "APP_GALLERY",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/jpeg"),
            "photo VIEW",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://media/external/video/media/1"), "video/mp4"),
            "video VIEW",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_EDIT).setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/jpeg"),
            "photo EDIT",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_EDIT).setDataAndType(Uri.parse("content://media/external/video/media/1"), "video/mp4"),
            "video EDIT",
        )
        assertVaultHandles(
            Intent(MediaStore.ACTION_REVIEW).setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/jpeg"),
            "camera photo REVIEW",
        )
        assertVaultHandles(
            Intent(MediaStore.ACTION_REVIEW).setDataAndType(Uri.parse("content://media/external/video/media/1"), "video/mp4"),
            "camera video REVIEW",
        )
        assertVaultHandles(
            Intent("com.android.camera.action.REVIEW").setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/jpeg"),
            "legacy camera photo REVIEW",
        )
        assertVaultHandles(Intent(Intent.ACTION_PICK).setType("image/*"), "legacy photo PICK")
        assertVaultHandles(Intent(Intent.ACTION_PICK).setType("video/*"), "legacy video PICK")
        assertVaultHandles(
            Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
            "legacy photo GET_CONTENT",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*"),
            "legacy video GET_CONTENT",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_VIEW).setType("vnd.android.cursor.dir/image"),
            "image collection VIEW",
        )
        assertVaultHandles(
            Intent(Intent.ACTION_VIEW).setType("vnd.android.cursor.dir/video"),
            "video collection VIEW",
        )
    }
}
