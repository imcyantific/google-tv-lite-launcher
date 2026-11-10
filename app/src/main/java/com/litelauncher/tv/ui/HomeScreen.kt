package com.litelauncher.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.litelauncher.tv.LauncherViewModel
import com.litelauncher.tv.R
import com.litelauncher.tv.Screen
import com.litelauncher.tv.data.AppEntry
import com.litelauncher.tv.data.AppRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val settings = vm.settings
    val favourites = vm.favouriteApps
    val tileWidth = TileWidths.getOrElse(settings.tileSize) { TileWidths[1] }
    val firstTile = remember { FocusRequester() }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    // A launcher is the bottom of the stack, so Back does nothing here.
    BackHandler {}

    LaunchedEffect(favourites.size) { firstTile.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            vm = vm,
            accent = accent,
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 32.dp),
        )
        Spacer(Modifier.weight(1f))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(favourites, key = { _, app -> app.packageName }) { index, app ->
                AppTile(
                    app = app,
                    width = tileWidth,
                    showLabel = settings.showLabels,
                    accent = accent,
                    onClick = { AppRepository.launch(context, app.packageName) },
                    onLongClick = { menuApp = app },
                    modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
            item(key = "all-apps-tile") {
                AllAppsTile(
                    width = tileWidth,
                    accent = accent,
                    onClick = { vm.screen = Screen.AllApps },
                    modifier = if (favourites.isEmpty()) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
        }
    }

    menuApp?.let { app ->
        AppMenu(vm = vm, app = app, accent = accent, onDismiss = { menuApp = null })
    }
}

@Composable
private fun TopBar(vm: LauncherViewModel, accent: Color, modifier: Modifier = Modifier) {
    val settings = vm.settings
    val now = rememberCurrentTime(vm.resumeCount)
    val timeText = now.format(DateTimeFormatter.ofPattern(if (settings.use24h) "HH:mm" else "h:mm a"))
    val dateText = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val weather = vm.weather

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column {
            Text(text = timeText, fontSize = 52.sp, fontWeight = FontWeight.Light)
            Text(text = dateText, fontSize = 17.sp, color = Color.White.copy(alpha = 0.8f))
        }
        Spacer(Modifier.weight(1f))
        if (weather != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 6.dp)) {
                Text(text = "${weather.temperature}°", fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text(
                    text = listOf(weather.summary, settings.city).filter { it.isNotEmpty() }.joinToString(" · "),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(24.dp))
        }
        NetworkIndicator(status = vm.network, modifier = Modifier.padding(top = 21.dp))
        Spacer(Modifier.width(18.dp))
        Surface(
            onClick = { vm.screen = Screen.Settings },
            modifier = Modifier
                .padding(top = 10.dp)
                .size(44.dp),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White,
                focusedContainerColor = accent,
                focusedContentColor = Color.Black,
                pressedContainerColor = accent,
                pressedContentColor = Color.Black,
            ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Launcher settings",
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Ticks once a minute, on the minute, and again whenever the launcher comes back on screen. */
@Composable
private fun rememberCurrentTime(refreshKey: Int): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(refreshKey) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
        }
    }
    return now
}
