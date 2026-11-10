package com.litelauncher.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.litelauncher.tv.data.AppEntry
import com.litelauncher.tv.data.AppRepository

@Composable
fun AllAppsScreen(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val apps = vm.visibleApps
    val tileWidth = TileWidths.getOrElse(vm.settings.tileSize) { TileWidths[1] }
    val firstTile = remember { FocusRequester() }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    BackHandler { vm.screen = Screen.Home }

    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) firstTile.requestFocusWhenReady()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Text(
            text = "All apps",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = ScreenPadding, top = 32.dp),
        )
        Text(
            text = "Hold OK on an app for options. A dot marks apps on your home row.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = ScreenPadding, top = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(tileWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                AppTile(
                    app = app,
                    width = tileWidth,
                    showLabel = vm.settings.showLabels,
                    accent = accent,
                    onClick = { AppRepository.launch(context, app.packageName) },
                    onLongClick = { menuApp = app },
                    marked = vm.isFavourite(app.packageName),
                    modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
        }
    }

    menuApp?.let { app ->
        AppMenu(vm = vm, app = app, accent = accent, onDismiss = { menuApp = null })
    }
}
