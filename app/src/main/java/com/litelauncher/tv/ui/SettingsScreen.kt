package com.litelauncher.tv.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.litelauncher.tv.LauncherViewModel
import com.litelauncher.tv.Screen
import com.litelauncher.tv.data.AppRepository
import com.litelauncher.tv.data.MediaFiles
import com.litelauncher.tv.data.WALLPAPER_CUSTOM
import com.litelauncher.tv.data.WALLPAPER_SLIDESHOW
import com.litelauncher.tv.data.WALLPAPER_VIDEO

private enum class SettingsDialog { None, Accent, Wallpaper, TileSize, Hidden }

private val SlideSpeeds = listOf(10 to "10 seconds", 30 to "30 seconds", 60 to "1 minute", 300 to "5 minutes")

@Composable
fun SettingsScreen(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val settings = vm.settings
    val firstRow = remember { FocusRequester() }
    var dialog by remember { mutableStateOf(SettingsDialog.None) }

    var browse by remember { mutableStateOf<BrowseMode?>(null) }
    var pendingBrowse by remember { mutableStateOf<BrowseMode?>(null) }

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (MediaFiles.hasAccess(context)) {
            browse = pendingBrowse
        } else {
            Toast.makeText(
                context,
                "Allow access to photos and videos to pick wallpapers from this device",
                Toast.LENGTH_LONG,
            ).show()
        }
        pendingBrowse = null
    }

    fun openBrowser(mode: BrowseMode) {
        dialog = SettingsDialog.None
        if (MediaFiles.hasAccess(context)) {
            browse = mode
        } else {
            pendingBrowse = mode
            permissionRequest.launch(MediaFiles.requiredPermissions())
        }
    }

    BackHandler { vm.screen = Screen.Home }
    LaunchedEffect(Unit) { firstRow.requestFocusWhenReady() }

    val wallpaperName = when (settings.wallpaper) {
        WALLPAPER_CUSTOM -> "Custom image"
        WALLPAPER_SLIDESHOW -> "Slideshow (${vm.slideshowFiles.size} images)"
        WALLPAPER_VIDEO -> "Video"
        else -> WallpaperPresets.getOrElse(settings.wallpaper) { WallpaperPresets.first() }.name
    }
    val speedIndex = SlideSpeeds.indexOfFirst { it.first == settings.slideSeconds }.coerceAtLeast(0)

    val rows = listOf(
        MenuOption("Android settings", "Network, display, accounts") {
            AppRepository.openSystemSettings(context)
        },
        MenuOption("Accent colour", Accents.getOrElse(settings.accent) { Accents.first() }.name) {
            dialog = SettingsDialog.Accent
        },
        MenuOption("Wallpaper", wallpaperName) { dialog = SettingsDialog.Wallpaper },
        MenuOption("Slideshow speed", SlideSpeeds[speedIndex].second) {
            val next = SlideSpeeds[(speedIndex + 1) % SlideSpeeds.size].first
            vm.update { it.copy(slideSeconds = next) }
        },
        MenuOption("Tile size", tileSizeName(settings.tileWidth)) { dialog = SettingsDialog.TileSize },
        MenuOption("App names under tiles", if (settings.showLabels) "On" else "Off") {
            vm.update { it.copy(showLabels = !it.showLabels) }
        },
        MenuOption("Clock", if (settings.use24h) "24-hour" else "12-hour") {
            vm.update { it.copy(use24h = !it.use24h) }
        },
        MenuOption("Weather city", settings.city.ifEmpty { "Off" }) {
            promptText(
                context = context,
                title = "Weather city",
                initial = settings.city.substringBefore(","),
                hint = "City name (leave empty to turn weather off)",
            ) { vm.setCity(it) }
        },
        MenuOption("Temperature unit", if (settings.fahrenheit) "°F" else "°C") {
            vm.setFahrenheit(!settings.fahrenheit)
        },
        MenuOption("Hidden apps", vm.hiddenApps.size.toString()) { dialog = SettingsDialog.Hidden },
        MenuOption("Home button helper", "Open accessibility settings") {
            if (!AppRepository.openAccessibilitySettings(context)) {
                Toast.makeText(context, "Open Settings > System > Accessibility", Toast.LENGTH_LONG).show()
            }
        },
        MenuOption("Replace Google TV home", if (settings.takeOverHome) "On (needs helper)" else "Off") {
            vm.update { it.copy(takeOverHome = !it.takeOverHome) }
        },
        MenuOption("Check for updates", if (vm.updateBusy) "Working…" else "Version ${vm.appVersion}") {
            vm.checkForUpdate()
        },
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(start = ScreenPadding, top = 32.dp),
    ) {
        Text(text = "Launcher settings", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier.width(520.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(rows) { index, row ->
                OptionRow(
                    title = row.title,
                    value = row.value,
                    accent = accent,
                    onClick = row.onClick,
                    modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.None -> Unit

        SettingsDialog.Accent -> MenuDialog(
            title = "Accent colour",
            accent = accent,
            initialIndex = settings.accent,
            onDismiss = { dialog = SettingsDialog.None },
            options = Accents.mapIndexed { index, item ->
                MenuOption(item.name) {
                    vm.update { it.copy(accent = index) }
                    dialog = SettingsDialog.None
                }
            },
        )

        SettingsDialog.Wallpaper -> MenuDialog(
            title = "Wallpaper",
            accent = accent,
            initialIndex = when (settings.wallpaper) {
                WALLPAPER_CUSTOM -> WallpaperPresets.size
                WALLPAPER_SLIDESHOW -> WallpaperPresets.size + 1
                WALLPAPER_VIDEO -> WallpaperPresets.size + 2
                else -> settings.wallpaper
            },
            onDismiss = { dialog = SettingsDialog.None },
            options = WallpaperPresets.mapIndexed { index, item ->
                MenuOption(item.name) {
                    vm.setWallpaperPreset(index)
                    dialog = SettingsDialog.None
                }
            } + MenuOption("Image from this device…") {
                openBrowser(BrowseMode.Image)
            } + MenuOption("Slideshow from a folder…") {
                openBrowser(BrowseMode.Folder)
            } + MenuOption("Video from this device…") {
                openBrowser(BrowseMode.Video)
            } + MenuOption("Image from a link…") {
                dialog = SettingsDialog.None
                promptText(
                    context = context,
                    title = "Wallpaper image link",
                    initial = settings.wallpaperUrl,
                    hint = "https://… (direct link to a JPG or PNG)",
                    isUrl = true,
                ) { vm.setWallpaperFromUrl(it) }
            },
        )

        SettingsDialog.TileSize -> MenuDialog(
            title = "Tile size",
            subtitle = "Now: ${tileSizeName(settings.tileWidth)}, ${settings.tileWidth} wide",
            accent = accent,
            initialIndex = TilePresets.indexOfFirst { it.second == settings.tileWidth }.coerceAtLeast(0),
            onDismiss = { dialog = SettingsDialog.None },
            options = TilePresets.map { (name, width) ->
                MenuOption(name, width.toString()) {
                    vm.update { it.copy(tileWidth = width) }
                    dialog = SettingsDialog.None
                }
            } + MenuOption("A little smaller", "-$TileWidthStep") {
                vm.update { it.copy(tileWidth = (it.tileWidth - TileWidthStep).coerceAtLeast(TileWidthMin)) }
            } + MenuOption("A little bigger", "+$TileWidthStep") {
                vm.update { it.copy(tileWidth = (it.tileWidth + TileWidthStep).coerceAtMost(TileWidthMax)) }
            },
        )

        SettingsDialog.Hidden -> MenuDialog(
            title = "Hidden apps",
            accent = accent,
            emptyText = "No hidden apps. Hold OK on an app and choose \"Hide from launcher\".",
            onDismiss = { dialog = SettingsDialog.None },
            options = vm.hiddenApps.map { app ->
                MenuOption(app.label, "Show again") { vm.setHidden(app.packageName, false) }
            },
        )
    }

    vm.availableUpdate?.let { info ->
        MenuDialog(
            title = "Version ${info.version} is available",
            subtitle = "You have ${vm.appVersion}",
            accent = accent,
            onDismiss = { vm.availableUpdate = null },
            options = listOf(
                MenuOption("Download and install") { vm.installUpdate(info) },
                MenuOption("Not now") { vm.availableUpdate = null },
            ),
        )
    }

    browse?.let { mode ->
        FileBrowserDialog(
            mode = mode,
            accent = accent,
            onPickFile = { file ->
                browse = null
                if (mode == BrowseMode.Video) vm.setVideoWallpaper(file) else vm.setWallpaperFromFile(Uri.fromFile(file))
            },
            onPickFolder = { folder ->
                browse = null
                vm.setSlideshowFolder(folder)
            },
            onDismiss = { browse = null },
        )
    }
}
