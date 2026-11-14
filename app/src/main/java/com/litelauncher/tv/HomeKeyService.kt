package com.litelauncher.tv

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.litelauncher.tv.data.KEY_TAKE_OVER_HOME
import com.litelauncher.tv.data.PREFS_NAME

/**
 * Optional helper, off until enabled in the TV's accessibility settings.
 *
 * Google TV keeps its own home screen as the default and gives no setting to change it, so
 * this service does three things:
 *  - opens Lite Launcher when Home is pressed on the remote;
 *  - opens it when the service starts, which includes when the TV boots;
 *  - opens it whenever the Google TV home screen is in front: after Back out of an app, and
 *    when the TV wakes from sleep. This part can be switched off in Launcher settings.
 */
class HomeKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastWindowPackage: String? = null
    private var lastRedirect = 0L
    private var burstStart = 0L
    private var burstCount = 0

    private val takeOverHome: Boolean
        get() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_TAKE_OVER_HOME, true)

    private val stockHomeInFront: Boolean
        get() = lastWindowPackage in STOCK_LAUNCHERS

    /** Opens the launcher if the Google TV home is (still) the screen in front. */
    private val recheck = Runnable { if (takeOverHome && stockHomeInFront) redirect() }

    /**
     * Waking the TV can put the Google TV home in front without telling us, or a moment after
     * we already reacted. So after wake-up, look again a few times while things settle.
     */
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // A wake-up is a fresh start: forget the loop guard from before sleep.
            burstCount = 0
            WAKE_CHECKS_MS.forEach { handler.postDelayed(recheck, it) }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_DREAMING_STOPPED)
        }
        ContextCompat.registerReceiver(this, wakeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (takeOverHome) openLauncher()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(wakeReceiver) }
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP) openLauncher()
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        lastWindowPackage = event.packageName?.toString() ?: return
        if (!stockHomeInFront || !takeOverHome) return

        redirect()
        // The system sometimes brings its home back right after we switch away (most often
        // during wake-up), so check once more shortly afterwards instead of assuming it worked.
        handler.removeCallbacks(recheck)
        handler.postDelayed(recheck, RETRY_MS)
    }

    override fun onInterrupt() = Unit

    private fun redirect() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirect < MIN_GAP_MS) {
            handler.removeCallbacks(recheck)
            handler.postDelayed(recheck, RETRY_MS)
            return
        }
        // Safety valve: if we keep getting bounced back, stop trying for a while
        // rather than fight the system in a loop.
        if (now - burstStart > BURST_WINDOW_MS) {
            burstStart = now
            burstCount = 0
        }
        if (++burstCount > MAX_BURST) return
        lastRedirect = now
        openLauncher()
    }

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
        val WAKE_CHECKS_MS = listOf(400L, 1500L, 3500L, 7000L)
        const val RETRY_MS = 900L
        const val MIN_GAP_MS = 700L
        const val BURST_WINDOW_MS = 20_000L
        const val MAX_BURST = 8
    }
}
