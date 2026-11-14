package com.litelauncher.tv

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.litelauncher.tv.data.KEY_TAKE_OVER_HOME
import com.litelauncher.tv.data.PREFS_NAME

/**
 * Optional helper, off until enabled in the TV's accessibility settings.
 *
 * Google TV keeps its own home screen as the default and gives no setting to change it, so
 * this service does three things:
 *  - opens Lite Launcher when Home is pressed on the remote;
 *  - opens it when the service starts, which includes when the TV boots;
 *  - opens it whenever the Google TV home screen comes to the front (after Back out of an
 *    app, on wake, and so on). This part can be switched off in Launcher settings.
 */
class HomeKeyService : AccessibilityService() {

    private var lastRedirect = 0L
    private var burstStart = 0L
    private var burstCount = 0

    private val takeOverHome: Boolean
        get() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_TAKE_OVER_HOME, true)

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (takeOverHome) openLauncher()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP) openLauncher()
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() !in STOCK_LAUNCHERS) return
        if (!takeOverHome) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirect < 1500) return
        // Safety valve: if we keep getting bounced back, stop trying for a while
        // rather than fight the system in a loop.
        if (now - burstStart > 20_000) {
            burstStart = now
            burstCount = 0
        }
        if (++burstCount > 5) return
        lastRedirect = now
        openLauncher()
    }

    override fun onInterrupt() = Unit

    private fun openLauncher() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { startActivity(intent) }
    }

    private companion object {
        val STOCK_LAUNCHERS = setOf(
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
        )
    }
}
