package com.litelauncher.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.litelauncher.tv.LauncherViewModel
import com.litelauncher.tv.Screen
import com.litelauncher.tv.data.AppEntry
import com.litelauncher.tv.data.AppRepository

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val accent = Accents.getOrElse(vm.settings.accent) { Accents.first() }.color

    MaterialTheme(colorScheme = darkColorScheme(primary = accent)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize()) {
                Wallpaper(vm)
                when (vm.screen) {
                    Screen.Home -> HomeScreen(vm, accent)
                    Screen.AllApps -> AllAppsScreen(vm, accent)
                    Screen.Settings -> SettingsScreen(vm, accent)
                }
            }
        }
    }
}

/** Long-press menu shared by the home row and the All apps grid. */
@Composable
fun AppMenu(vm: LauncherViewModel, app: AppEntry, accent: Color, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pkg = app.packageName
    val favourite = vm.isFavourite(pkg)
    val position = vm.settings.favourites.indexOf(pkg)

    val options = buildList {
        add(MenuOption("Open") { onDismiss(); AppRepository.launch(context, pkg) })
        add(
            MenuOption(if (favourite) "Remove from home row" else "Add to home row") {
                vm.toggleFavourite(pkg)
                onDismiss()
            },
        )
        if (favourite && position > 0) {
            add(MenuOption("Move left") { vm.moveFavourite(pkg, -1) })
        }
        if (favourite && position < vm.settings.favourites.lastIndex) {
            add(MenuOption("Move right") { vm.moveFavourite(pkg, 1) })
        }
        add(MenuOption("Hide from launcher") { vm.setHidden(pkg, true); onDismiss() })
        add(MenuOption("App info") { onDismiss(); AppRepository.openAppInfo(context, pkg) })
        add(MenuOption("Uninstall") { onDismiss(); AppRepository.uninstall(context, pkg) })
    }
    MenuDialog(title = app.label, options = options, accent = accent, onDismiss = onDismiss)
}
