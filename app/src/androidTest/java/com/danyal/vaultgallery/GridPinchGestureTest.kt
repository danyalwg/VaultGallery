package com.danyal.vaultgallery

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class GridPinchGestureTest {
    @Test
    fun picturesGridRespondsToPinchAcrossSamsungDensityRange() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        // connectedAndroidTest installs a clean APK, so the gallery otherwise opens on
        // its permission gate and the test never reaches the grid it intends to exercise.
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_IMAGES)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_VIDEO)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val preferences = context.getSharedPreferences("gallery-settings", 0)
        val originalTab = preferences.getString("resume_public_tab", "PICTURES")
        val originalDestination = preferences.getString("resume_destination", "tab")
        preferences.edit()
            .putString("resume_public_tab", "PICTURES")
            .putString("resume_destination", "tab")
            .commit()
        context.startActivity(
            Intent(context, MainGalleryActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )

        val description = Regex("Pictures grid, (\\d+) columns")
        assertTrue(
            "Pictures grid did not expose its layout",
            device.wait(Until.hasObject(By.descContains("Pictures grid,")), 8_000),
        )
        fun currentColumns(): Int {
            repeat(20) {
                val count = runCatching {
                    device.findObject(By.descContains("Pictures grid,"))
                        ?.contentDescription
                        ?.let(description::find)
                        ?.groupValues?.get(1)?.toInt()
                }.getOrNull()
                if (count != null) return count
                Thread.sleep(100)
            }
            error("Pictures layout did not expose a stable column count")
        }
        val startingColumns = currentColumns()

        val content = device.wait(
            Until.findObject(By.desc(Pattern.compile("(?i).+\\.(jpg|jpeg|png|webp|mp4|mkv|mov)"))),
            8_000,
        )
        assertNotNull("A media tile was not available for the physical pinch", content)
        try {
            if (startingColumns < 12) {
                content.pinchClose(.75f, 600)
            } else {
                content.pinchOpen(.75f, 600)
            }
            device.waitForIdle()
            val changedColumns = currentColumns()
            assertNotEquals("Pinch did not change the gallery density", startingColumns, changedColumns)
            assertTrue("Pinch settled outside the supported 3-12 column range", changedColumns in 3..12)

            if (startingColumns < 12) {
                content.pinchOpen(.75f, 600)
            } else {
                content.pinchClose(.75f, 600)
            }
            device.waitForIdle()
        } finally {
            // The physical gesture is allowed to exercise real persistence, then the exact user
            // preference is restored so an acceptance test never changes their chosen layout.
            preferences.edit()
                .putInt("grid_columns", startingColumns)
                .putString("resume_public_tab", originalTab)
                .putString("resume_destination", originalDestination)
                .commit()
        }
    }
}
