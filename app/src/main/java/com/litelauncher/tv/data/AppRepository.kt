package com.litelauncher.tv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val packageName: String,
    val label: String,
    val image: ImageBitmap,
    val isBanner: Boolean,
)

object AppRepository {

    private val preferredFavourites = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.plexapp.android",
        "com.stremio.one",
        "org.xbmc.kodi",
        "com.spotify.tv.android",
    )

    /** TV apps first, then phone-style apps that only have a normal launcher entry. */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)

        for (category in categories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, 0)) {
                val activity = info.activityInfo ?: continue
                val pkg = activity.packageName
                if (pkg == context.packageName || pkg in found) continue
                runCatching {
                    val banner = activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm)
                    val bitmap = if (banner != null) {
                        banner.toBitmap(320, 180)
                    } else {
                        info.loadIcon(pm).toBitmap(96, 96)
                    }
                    found[pkg] = AppEntry(
                        packageName = pkg,
                        label = info.loadLabel(pm).toString(),
                        image = bitmap.asImageBitmap(),
                        isBanner = banner != null,
                    )
                }
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun defaultFavourites(apps: List<AppEntry>): List<String> {
        val installed = apps.map { it.packageName }
        val picked = preferredFavourites.filter { it in installed }.toMutableList()
        for (pkg in installed) {
            if (picked.size >= 6) break
            if (pkg !in picked) picked += pkg
        }
        return picked
    }

    fun launch(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(intent) }
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun uninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openSystemSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
