package com.litelauncher.tv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Accent(val name: String, val color: Color)

data class WallpaperPreset(val name: String, val colors: List<Color>)

// Add or change entries here to customise the choices shown in Settings.
val Accents = listOf(
    Accent("Cyan", Color(0xFF35D0E0)),
    Accent("Blue", Color(0xFF5B9BFF)),
    Accent("Purple", Color(0xFFA98BFF)),
    Accent("Pink", Color(0xFFFF7EB6)),
    Accent("Orange", Color(0xFFFFA24A)),
    Accent("Green", Color(0xFF5ED98A)),
    Accent("White", Color(0xFFFFFFFF)),
)

val WallpaperPresets = listOf(
    WallpaperPreset("Midnight", listOf(Color(0xFF0E1A2E), Color(0xFF05070B))),
    WallpaperPreset("Aurora", listOf(Color(0xFF0E3B43), Color(0xFF1B1446))),
    WallpaperPreset("Sunset", listOf(Color(0xFF3A1C4A), Color(0xFF6B2D2A))),
    WallpaperPreset("Forest", listOf(Color(0xFF123324), Color(0xFF06100C))),
    WallpaperPreset("Graphite", listOf(Color(0xFF23272E), Color(0xFF101215))),
    WallpaperPreset("Black", listOf(Color.Black, Color.Black)),
)

// Tile widths in dp. Height always follows at 16:9.
val TilePresets = listOf(
    "Extra small" to 110,
    "Small" to 150,
    "Medium" to 190,
    "Large" to 230,
    "Extra large" to 270,
)
const val TileWidthMin = 90
const val TileWidthMax = 320
const val TileWidthStep = 10

fun tileSizeName(width: Int): String =
    TilePresets.firstOrNull { it.second == width }?.first ?: "Custom ($width)"

val TileSpacing: Dp = 16.dp

/** Room inside the row's edges so the focused tile's zoom and border aren't clipped. */
val RowBleed: Dp = 14.dp

val TileColor = Color(0xFF1C2027)
val PanelColor = Color(0xFF171A20)
val ScreenPadding = 48.dp
