package com.litelauncher.tv.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

data class StorageVolume(val name: String, val root: File)

data class FolderListing(
    val folder: File,
    val folders: List<File>,
    val images: List<File>,
    val videos: List<File>,
)

/** Finding images and videos on the device for wallpapers. */
object MediaFiles {

    private val imageTypes = setOf("jpg", "jpeg", "png", "webp")
    private val videoTypes = setOf("mp4", "m4v", "webm", "mkv")

    fun isImage(file: File) = file.extension.lowercase() in imageTypes
    fun isVideo(file: File) = file.extension.lowercase() in videoTypes

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    fun hasAccess(context: Context): Boolean =
        hasAllFilesAccess() || requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Internal storage first, then any USB drive or SD card. */
    fun volumes(context: Context): List<StorageVolume> {
        val roots = context.getExternalFilesDirs(null)
            .filterNotNull()
            .map { it.path.substringBefore("/Android/") }
            .distinct()
            .map { File(it) }
        return roots.mapIndexed { index, root ->
            StorageVolume(if (index == 0) "Internal storage" else "USB / SD card (${root.name})", root)
        }
    }

    fun list(folder: File): FolderListing {
        val entries = folder.listFiles().orEmpty()
            .filter { !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
        return FolderListing(
            folder = folder,
            folders = entries.filter { it.isDirectory && it.name != "Android" },
            images = entries.filter { it.isFile && isImage(it) },
            videos = entries.filter { it.isFile && isVideo(it) },
        )
    }

    fun imagesIn(folder: File): List<File> =
        folder.listFiles().orEmpty()
            .filter { it.isFile && isImage(it) && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }

    /** Opens the system page where the user can grant "all files access". */
    fun openAllFilesAccessSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val forThisApp = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(forThisApp) }.isSuccess) return true
        val general = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(general) }.isSuccess
    }
}
