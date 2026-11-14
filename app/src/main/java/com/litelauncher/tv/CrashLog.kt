package com.litelauncher.tv

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Remembers why the app last crashed. A TV shows no error when an app dies, so the report is
 * saved to a file and shown the next time the launcher opens.
 */
object CrashLog {

    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val version = runCatching {
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName
                }.getOrNull()
                file(app).writeText(
                    "Lite Launcher $version, $time, thread ${thread.name}\n" + Log.getStackTraceString(error),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Returns the saved report, if any, and forgets it. */
    fun take(context: Context): String? = runCatching {
        val saved = file(context)
        if (!saved.exists()) return null
        val text = saved.readText()
        saved.delete()
        text.ifBlank { null }
    }.getOrNull()
}

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
