package com.litelauncher.tv

import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** Starts the launcher and the Home helper the way the TV does, and fails if either one crashes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television")
class StartupSmokeTest {

    private fun settle() {
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun launcherStartsAndHandlesKeys() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        settle()
        val activity = controller.get()
        for (code in listOf(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)) {
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            settle()
        }
        controller.newIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        settle()
        controller.pause().stop().destroy()
    }

    @Test
    fun crashReportScreenShowsThenContinues() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        java.io.File(app.filesDir, "last_crash.txt").writeText("Lite Launcher test\njava.lang.IllegalStateException: boom\n\tat a.b(c.kt:1)")
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        settle()
        val activity = controller.get()
        check(!java.io.File(app.filesDir, "last_crash.txt").exists()) { "crash report should be cleared once shown" }
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        settle()
        controller.pause().stop().destroy()
    }

    @Test
    fun helperServiceStarts() {
        val controller = Robolectric.buildService(HomeKeyService::class.java).create()
        val service = controller.get()
        val connected = android.accessibilityservice.AccessibilityService::class.java
            .getDeclaredMethod("onServiceConnected").apply { isAccessible = true }
        connected.invoke(service)
        settle()
        service.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
        settle()
        controller.destroy()
    }
}
