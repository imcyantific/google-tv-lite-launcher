package com.litelauncher.tv.data

import android.content.Context

const val WALLPAPER_CUSTOM = -1
const val WALLPAPER_SLIDESHOW = -2
const val WALLPAPER_VIDEO = -3

const val PREFS_NAME = "launcher"
const val KEY_TAKE_OVER_HOME = "takeOverHome"

data class LauncherSettings(
    val favourites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val seeded: Boolean = false,
    val accent: Int = 0,
    val wallpaper: Int = 0,
    val wallpaperUrl: String = "",
    val wallpaperPath: String = "",
    val slideSeconds: Int = 30,
    val takeOverHome: Boolean = true,
    val tileWidth: Int = 190,
    val showLabels: Boolean = true,
    val use24h: Boolean = false,
    val city: String = "",
    val lat: Float = Float.NaN,
    val lon: Float = Float.NaN,
    val fahrenheit: Boolean = false,
) {
    val hasWeatherLocation: Boolean get() = !lat.isNaN() && !lon.isNaN()
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LauncherSettings = LauncherSettings(
        favourites = sp.getString("favourites", "").orEmpty().split('|').filter { it.isNotBlank() },
        hidden = sp.getStringSet("hidden", emptySet()).orEmpty().toSet(),
        seeded = sp.getBoolean("seeded", false),
        accent = sp.getInt("accent", 0),
        wallpaper = sp.getInt("wallpaper", 0),
        wallpaperUrl = sp.getString("wallpaperUrl", "").orEmpty(),
        wallpaperPath = sp.getString("wallpaperPath", "").orEmpty(),
        slideSeconds = sp.getInt("slideSeconds", 30),
        takeOverHome = sp.getBoolean(KEY_TAKE_OVER_HOME, true),
        // Older versions stored a size index (0-2); convert it once.
        tileWidth = sp.getInt("tileWidth", listOf(150, 190, 230).getOrElse(sp.getInt("tileSize", 1)) { 190 }),
        showLabels = sp.getBoolean("showLabels", true),
        use24h = sp.getBoolean("use24h", false),
        city = sp.getString("city", "").orEmpty(),
        lat = sp.getFloat("lat", Float.NaN),
        lon = sp.getFloat("lon", Float.NaN),
        fahrenheit = sp.getBoolean("fahrenheit", false),
    )

    fun save(s: LauncherSettings) {
        sp.edit()
            .putString("favourites", s.favourites.joinToString("|"))
            .putStringSet("hidden", s.hidden)
            .putBoolean("seeded", s.seeded)
            .putInt("accent", s.accent)
            .putInt("wallpaper", s.wallpaper)
            .putString("wallpaperUrl", s.wallpaperUrl)
            .putString("wallpaperPath", s.wallpaperPath)
            .putInt("slideSeconds", s.slideSeconds)
            .putBoolean(KEY_TAKE_OVER_HOME, s.takeOverHome)
            .putInt("tileWidth", s.tileWidth)
            .putBoolean("showLabels", s.showLabels)
            .putBoolean("use24h", s.use24h)
            .putString("city", s.city)
            .putFloat("lat", s.lat)
            .putFloat("lon", s.lon)
            .putBoolean("fahrenheit", s.fahrenheit)
            .apply()
    }
}
