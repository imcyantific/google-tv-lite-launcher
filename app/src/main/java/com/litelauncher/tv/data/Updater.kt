package com.litelauncher.tv.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.litelauncher.tv.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String)

class UpdateException(message: String) : Exception(message)

/** Checks the project's GitHub Releases for a newer APK, downloads it and starts the installer. */
object Updater {

    private const val MAX_BYTES = 100L * 1024 * 1024

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** The newest published release, or an [UpdateException] explaining why it couldn't be read. */
    fun latest(): UpdateInfo {
        val connection = open("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest", "application/vnd.github+json")
        try {
            when (connection.responseCode) {
                200 -> Unit
                401, 403, 404 -> throw UpdateException(
                    "Couldn't read releases. If the GitHub repository is private, see \"In-app updates\" in the README.",
                )
                else -> throw UpdateException("GitHub answered with error ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    return UpdateInfo(json.getString("tag_name").removePrefix("v"), asset.getString("url"))
                }
            }
            throw UpdateException("The latest release has no APK attached")
        } finally {
            connection.disconnect()
        }
    }

    /** Compares versions like 1.4 and 1.10 number by number. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun download(info: UpdateInfo, context: Context): File {
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() }
        val target = File(folder, "LiteLauncher-${info.version}.apk")

        var connection = open(info.downloadUrl, "application/octet-stream")
        try {
            // GitHub redirects to its file storage. Follow by hand so the access
            // token (if any) is never sent to the second server.
            var hops = 0
            while (connection.responseCode in 301..308 && hops++ < 5) {
                val next = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                connection = (URL(next).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                }
            }
            if (connection.responseCode != 200) throw UpdateException("Download failed (${connection.responseCode})")
            var total = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) throw UpdateException("Download is unexpectedly large")
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    /** Hands the APK to Android's installer, which asks the user to confirm. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun open(url: String, accept: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (BuildConfig.UPDATE_TOKEN.isNotEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_TOKEN}")
        }
        return connection
    }
}
