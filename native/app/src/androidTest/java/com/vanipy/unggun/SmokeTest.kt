package com.vanipy.unggun

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
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
    private val timeout = 10_000L

    @Test
    fun addAppsAndSeeThemInListAndLogs() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)!!.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)
        assertTrue(device.wait(Until.hasObject(By.pkg(pkg)), timeout))

        // notification permission dialog may appear on API 33+
        device.findObject(By.textContains("ALLOW"))?.click()
        device.wait(Until.hasObject(By.desc("add apps")), timeout)

        device.findObject(By.desc("add apps")).click()
        assertTrue(device.wait(Until.hasObject(By.text("add github apps")), timeout))

        val input = device.findObject(By.clazz("android.widget.EditText"))
        assertNotNull(input)
        input.text = "https://github.com/ImranR98/Obtainium\nhttps://github.com/irvan-putra/tengok"
        device.findObject(By.text("add")).click()

        // both rows visible in the list
        assertTrue(device.wait(Until.hasObject(By.text("ImranR98/Obtainium")), timeout))
        assertTrue(device.wait(Until.hasObject(By.text("irvan-putra/tengok")), timeout))

        // logs record the add
        device.findObject(By.desc("More options"))?.click()
        device.wait(Until.hasObject(By.text("logs")), timeout)
        device.findObject(By.text("logs")).click()
        assertTrue(device.wait(Until.hasObject(By.textContains("added 2 app")), timeout))
    }
}
