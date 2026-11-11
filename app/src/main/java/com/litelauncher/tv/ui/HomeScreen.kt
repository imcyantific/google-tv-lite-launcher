package com.litelauncher.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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

/** Switches off the system's own scroll-to-focused-item so the row can scroll in whole tiles. */
private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val settings = vm.settings
    val favourites = vm.favouriteApps
    val tileWidth = settings.tileWidth.dp
    val total = favourites.size + 1 // favourites plus the "All apps" tile
    val listState = rememberLazyListState()
    val currentTile = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var windowStart by remember { mutableIntStateOf(0) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    // A launcher is the bottom of the stack, so Back does nothing here.
    BackHandler {}

    // Start on the first app: once apps have loaded, after the row changes, and on Home.
    LaunchedEffect(vm.appsLoaded, favourites.size, vm.homeResetCount) {
        if (!vm.appsLoaded) return@LaunchedEffect
        focusedIndex = 0
        windowStart = 0
        listState.scrollToItem(0)
        currentTile.requestFocusWhenReady()
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            vm = vm,
            accent = accent,
            rowFocus = currentTile,
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 32.dp),
        )
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            // The row is exactly as wide as the number of whole tiles that fit between the
            // side margins, and is centred, so no half tile ever shows at either edge.
            val available = maxWidth - ScreenPadding * 2
            val fits = ((available + TileSpacing) / (tileWidth + TileSpacing)).toInt().coerceAtLeast(1)
            val perPage = minOf(fits, total)
            val rowWidth = tileWidth * perPage + TileSpacing * (perPage - 1) + RowBleed * 2
            val lastStart = (total - perPage).coerceAtLeast(0)

            fun onTileFocused(index: Int) {
                focusedIndex = index
                windowStart = when {
                    index < windowStart -> index
                    index >= windowStart + perPage -> index - perPage + 1
                    else -> windowStart
                }.coerceIn(0, lastStart)
            }

            LaunchedEffect(windowStart, perPage, total) {
                listState.animateScrollToItem(windowStart.coerceIn(0, lastStart))
            }

            fun tileModifier(index: Int): Modifier {
                val base = Modifier.onFocusChanged { if (it.isFocused) onTileFocused(index) }
                return if (index == focusedIndex) base.focusRequester(currentTile) else base
            }

            CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
                LazyRow(
                    state = listState,
                    modifier = Modifier.width(rowWidth),
                    contentPadding = PaddingValues(horizontal = RowBleed, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(TileSpacing),
                ) {
                    itemsIndexed(favourites, key = { _, app -> app.packageName }) { index, app ->
                        AppTile(
                            app = app,
                            width = tileWidth,
                            showLabel = settings.showLabels,
                            accent = accent,
                            onClick = { AppRepository.launch(context, app.packageName) },
                            onLongClick = { menuApp = app },
                            modifier = tileModifier(index),
                        )
                    }
                    item(key = "all-apps-tile") {
                        AllAppsTile(
                            width = tileWidth,
                            accent = accent,
                            onClick = { vm.screen = Screen.AllApps },
                            modifier = tileModifier(favourites.size),
                        )
                    }
                }
            }
        }
    }

    menuApp?.let { app ->
        AppMenu(vm = vm, app = app, accent = accent, onDismiss = { menuApp = null })
    }
}

@Composable
private fun TopBar(
    vm: LauncherViewModel,
    accent: Color,
    rowFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
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
                .size(44.dp)
                // Down always returns to the tile you were on, not whatever sits below the gear.
                .focusProperties { down = rowFocus },
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
