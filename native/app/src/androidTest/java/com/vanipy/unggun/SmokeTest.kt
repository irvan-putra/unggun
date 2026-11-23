package com.vanipy.unggun

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke: launch, bulk-add two synthetic repos through the add sheet,
 * see them in the list, open logs and see the add recorded there.
 * Update-check parsing itself is pinned by JVM unit tests against recorded
 * GitHub fixtures; the live GitHub API is not part of this smoke.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    private val pkg = "com.vanipy.unggun"
    private val timeout = 15_000L

    /** The POST_NOTIFICATIONS runtime dialog on API 33+; accept any known button form. */
    private fun dismissNotificationPermission(device: UiDevice) {
        val selectors: List<BySelector> = listOf(
            By.res("com.android.permissioncontroller:id/permission_allow_button"),
            By.res("com.android.packageinstaller:id/permission_allow_button"),
            By.text("Allow"),
            By.text("ALLOW"),
            By.text("While using the app"),
        )
        val deadline = System.currentTimeMillis() + 8_000
        while (System.currentTimeMillis() < deadline) {
            for (sel in selectors) {
                device.findObject(sel)?.let {
                    it.click()
                    device.wait(Until.gone(sel), 2_000)
                    return
                }
            }
            Thread.sleep(250)
        }
    }

    @Test
    fun addAppsAndSeeThemInListAndLogs() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)!!.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)
        assertTrue("app window did not appear", device.wait(Until.hasObject(By.pkg(pkg)), timeout))

        dismissNotificationPermission(device)

        assertTrue("FAB missing", device.wait(Until.hasObject(By.desc("add apps")), timeout))
        device.findObject(By.desc("add apps")).click()
        assertTrue("add sheet missing", device.wait(Until.hasObject(By.text("add github apps")), timeout))

        val input = device.findObject(By.clazz("android.widget.EditText"))
        assertNotNull("url input missing", input)
        input.text = "https://github.com/ImranR98/Obtainium\nhttps://github.com/irvan-putra/tengok"
        device.findObject(By.text("add")).click()

        // both rows visible in the list
        assertTrue("Obtainium row missing", device.wait(Until.hasObject(By.text("ImranR98/Obtainium")), timeout))
        assertTrue("tengok row missing", device.wait(Until.hasObject(By.text("irvan-putra/tengok")), timeout))

        // logs record the add
        device.findObject(By.desc("More options")).click()
        assertTrue(device.wait(Until.hasObject(By.text("logs")), timeout))
        device.findObject(By.text("logs")).click()
        assertTrue("log entry missing", device.wait(Until.hasObject(By.textContains("added 2 app")), timeout))
    }
}
