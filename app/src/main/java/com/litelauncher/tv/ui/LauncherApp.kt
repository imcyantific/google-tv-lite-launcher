package com.litelauncher.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.litelauncher.tv.LauncherViewModel
import com.litelauncher.tv.Screen
import com.litelauncher.tv.data.AppEntry
import com.litelauncher.tv.data.AppRepository
import kotlinx.coroutines.delay

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val accent = Accents.getOrElse(vm.settings.accent) { Accents.first() }.color

    MaterialTheme(colorScheme = darkColorScheme(primary = accent)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize()) {
                Wallpaper(vm)
                IdleWatcher(vm)
                AnimatedVisibility(visible = !vm.ambient, enter = fadeIn(tween(300)), exit = fadeOut(tween(800))) {
                    when (vm.screen) {
                        Screen.Home -> HomeScreen(vm, accent)
                        Screen.AllApps -> AllAppsScreen(vm, accent)
                        Screen.Settings -> SettingsScreen(vm, accent)
                    }
                }
            }
        }
    }
}

/**
 * Wallpaper-only mode. With the option on, the screen is kept awake while the launcher is in
 * front (so the TV's own screensaver and sleep timer don't start), and after the chosen idle
 * time on the home screen everything except the wallpaper fades out.
 */
@Composable
private fun IdleWatcher(vm: LauncherViewModel) {
    val seconds = vm.settings.idleHideSeconds
    val view = LocalView.current

    DisposableEffect(seconds > 0) {
        view.keepScreenOn = seconds > 0
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(seconds, vm.activityTick, vm.screen, vm.active) {
        if (seconds <= 0 || !vm.active || vm.screen != Screen.Home) return@LaunchedEffect
        delay(seconds * 1000L)
        vm.enterAmbient()
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
