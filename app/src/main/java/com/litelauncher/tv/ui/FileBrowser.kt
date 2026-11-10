package com.litelauncher.tv.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.litelauncher.tv.data.FolderListing
import com.litelauncher.tv.data.MediaFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class BrowseMode(val title: String) {
    Image("Pick an image"),
    Folder("Pick a folder of images"),
    Video("Pick a video"),
}

/**
 * A small built-in file browser, driven with the D-pad: OK opens a folder or picks a file,
 * Back goes up one level and closes the browser at the top.
 */
@Composable
fun FileBrowserDialog(
    mode: BrowseMode,
    accent: Color,
    onPickFile: (File) -> Unit,
    onPickFolder: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val volumes = remember { MediaFiles.volumes(context) }
    var folder by remember { mutableStateOf(volumes.singleOrNull()?.root) }
    var listing by remember { mutableStateOf<FolderListing?>(null) }

    LaunchedEffect(folder) {
        val current = folder
        listing = if (current == null) null else withContext(Dispatchers.IO) { MediaFiles.list(current) }
    }

    fun goUp() {
        val current = folder
        when {
            current == null -> onDismiss()
            volumes.any { it.root == current } -> if (volumes.size > 1) folder = null else onDismiss()
            else -> folder = current.parentFile
        }
    }

    val current = folder
    val loaded = listing?.takeIf { it.folder == current }
    val options = buildList {
        if (current == null) {
            volumes.forEach { volume -> add(MenuOption(volume.name) { folder = volume.root }) }
        } else if (loaded != null) {
            if (mode == BrowseMode.Folder && loaded.images.isNotEmpty()) {
                add(MenuOption("Use this folder", "${loaded.images.size} images") { onPickFolder(current) })
            }
            add(MenuOption("Up one level") { goUp() })
            loaded.folders.forEach { child -> add(MenuOption(child.name, "Folder") { folder = child }) }
            val files = when (mode) {
                BrowseMode.Image -> loaded.images
                BrowseMode.Video -> loaded.videos
                BrowseMode.Folder -> emptyList()
            }
            files.forEach { file -> add(MenuOption(file.name) { onPickFile(file) }) }
        }
        if ((current == null || loaded != null) && !MediaFiles.hasAllFilesAccess()) {
            add(
                MenuOption("Can't see your files?", "Allow all files access") {
                    if (!MediaFiles.openAllFilesAccessSettings(context)) {
                        Toast.makeText(
                            context,
                            "Open Settings > Apps > Special app access > All files access",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
        }
    }

    MenuDialog(
        title = mode.title,
        subtitle = current?.path ?: "Choose a storage",
        options = options,
        accent = accent,
        emptyText = "Loading…",
        focusKey = if (current == null) "volumes" else loaded,
        onDismiss = { goUp() },
    )
}
