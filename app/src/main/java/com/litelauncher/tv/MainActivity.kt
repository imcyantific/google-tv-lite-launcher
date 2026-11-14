package com.litelauncher.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.litelauncher.tv.ui.LauncherApp

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherApp(viewModel) }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStart() {
        super.onStart()
        viewModel.active = true
    }

    override fun onStop() {
        viewModel.active = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeCount++
        viewModel.onUserActivity()
        viewModel.refreshWeather()
    }

    private var wakingKey = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // In wallpaper-only mode the first button press just brings the screen back.
        // That whole press (down and up) is swallowed so it doesn't also open an app.
        if (viewModel.ambient && event.action == KeyEvent.ACTION_DOWN) {
            wakingKey = true
            viewModel.onUserActivity()
            return true
        }
        if (wakingKey) {
            if (event.action == KeyEvent.ACTION_UP) wakingKey = false
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) viewModel.onUserActivity()
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onUserActivity()
        // Pressing Home while in All apps or Settings returns to the main row.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.screen = Screen.Home
            viewModel.homeResetCount++
        }
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
