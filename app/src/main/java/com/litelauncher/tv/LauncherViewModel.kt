package com.litelauncher.tv

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.litelauncher.tv.data.AppEntry
import com.litelauncher.tv.data.AppRepository
import com.litelauncher.tv.data.LauncherSettings
import com.litelauncher.tv.data.MediaFiles
import com.litelauncher.tv.data.NetworkMonitor
import com.litelauncher.tv.data.NetworkStatus
import com.litelauncher.tv.data.Prefs
import com.litelauncher.tv.data.WALLPAPER_CUSTOM
import com.litelauncher.tv.data.WALLPAPER_SLIDESHOW
import com.litelauncher.tv.data.WALLPAPER_VIDEO
import com.litelauncher.tv.data.Wallpapers
import com.litelauncher.tv.data.Weather
import com.litelauncher.tv.data.WeatherNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { Home, AllApps, Settings }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val wallpaperFile = File(app.filesDir, "wallpaper.img")
    private var loadJob: Job? = null
    private var weatherFetchedAt = 0L

    var screen by mutableStateOf(Screen.Home)
    var resumeCount by mutableIntStateOf(0)

    /** True while the launcher is on screen; slideshow and video only run then. */
    var active by mutableStateOf(false)
    var settings by mutableStateOf(prefs.load())
        private set
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var weather by mutableStateOf<WeatherNow?>(null)
        private set
    var wallpaperImage by mutableStateOf<ImageBitmap?>(null)
        private set
    var slideshowFiles by mutableStateOf<List<File>>(emptyList())
        private set
    var network by mutableStateOf(NetworkStatus())
        private set

    private val networkMonitor = NetworkMonitor(app) { network = it }

    val visibleApps: List<AppEntry>
        get() = apps.filter { it.packageName !in settings.hidden }

    val favouriteApps: List<AppEntry>
        get() {
            val byPackage = apps.associateBy { it.packageName }
            return settings.favourites
                .filter { it !in settings.hidden }
                .mapNotNull { byPackage[it] }
        }

    val hiddenApps: List<AppEntry>
        get() = apps.filter { it.packageName in settings.hidden }

    init {
        networkMonitor.start()
        refreshApps()
        if (settings.wallpaper == WALLPAPER_CUSTOM) loadWallpaperFile()
        if (settings.wallpaper == WALLPAPER_SLIDESHOW) loadSlideshow()
    }

    fun update(change: (LauncherSettings) -> LauncherSettings) {
        settings = change(settings)
        prefs.save(settings)
    }

    fun refreshApps() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { AppRepository.load(getApplication()) }
            apps = loaded
            if (!settings.seeded && loaded.isNotEmpty()) {
                update { it.copy(favourites = AppRepository.defaultFavourites(loaded), seeded = true) }
            }
        }
    }

    fun isFavourite(packageName: String) = packageName in settings.favourites

    fun toggleFavourite(packageName: String) = update {
        if (packageName in it.favourites) it.copy(favourites = it.favourites - packageName)
        else it.copy(favourites = it.favourites + packageName)
    }

    fun moveFavourite(packageName: String, delta: Int) = update {
        val list = it.favourites.toMutableList()
        val from = list.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to !in list.indices) return@update it
        list.removeAt(from)
        list.add(to, packageName)
        it.copy(favourites = list)
    }

    fun setHidden(packageName: String, hidden: Boolean) = update {
        it.copy(hidden = if (hidden) it.hidden + packageName else it.hidden - packageName)
    }

    fun refreshWeather(force: Boolean = false) {
        val current = settings
        if (!current.hasWeatherLocation) {
            weather = null
            return
        }
        val age = SystemClock.elapsedRealtime() - weatherFetchedAt
        if (!force && weather != null && age < 30 * 60 * 1000) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { Weather.current(current.lat, current.lon, current.fahrenheit) }.getOrNull()
            }
            if (result != null) {
                weather = result
                weatherFetchedAt = SystemClock.elapsedRealtime()
            }
        }
    }

    fun setCity(query: String) {
        if (query.isBlank()) {
            update { it.copy(city = "", lat = Float.NaN, lon = Float.NaN) }
            weather = null
            return
        }
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { runCatching { Weather.findCity(query) }.getOrNull() }
            if (found == null) {
                toast("Couldn't find \"$query\"")
            } else {
                update { it.copy(city = found.name, lat = found.lat, lon = found.lon) }
                refreshWeather(force = true)
            }
        }
    }

    fun setFahrenheit(enabled: Boolean) {
        update { it.copy(fahrenheit = enabled) }
        refreshWeather(force = true)
    }

    fun setWallpaperFromUrl(url: String) {
        val clean = url.trim()
        if (clean.isEmpty()) return
        toast("Downloading wallpaper…")
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { Wallpapers.download(clean, wallpaperFile) }.isSuccess
            }
            if (ok) {
                update { it.copy(wallpaper = WALLPAPER_CUSTOM, wallpaperUrl = clean) }
                loadWallpaperFile()
            } else {
                toast("Couldn't load that image. Use a direct https link to a JPG or PNG.")
            }
        }
    }

    fun setWallpaperFromFile(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: error("Can't open file")
                    Wallpapers.save(stream, wallpaperFile)
                }.isSuccess
            }
            if (ok) {
                update { it.copy(wallpaper = WALLPAPER_CUSTOM) }
                loadWallpaperFile()
            } else {
                toast("Couldn't use that file. Pick a JPG or PNG image.")
            }
        }
    }

    fun setSlideshowFolder(folder: File) {
        update { it.copy(wallpaper = WALLPAPER_SLIDESHOW, wallpaperPath = folder.path) }
        loadSlideshow()
    }

    fun setVideoWallpaper(file: File) {
        update { it.copy(wallpaper = WALLPAPER_VIDEO, wallpaperPath = file.path) }
    }

    fun onVideoError() {
        toast("Couldn't play that video. MP4 (H.264) works best.")
    }

    private fun loadSlideshow() {
        val folder = File(settings.wallpaperPath)
        viewModelScope.launch {
            slideshowFiles = withContext(Dispatchers.IO) { MediaFiles.imagesIn(folder) }
        }
    }

    fun setWallpaperPreset(index: Int) {
        update { it.copy(wallpaper = index) }
        wallpaperImage = null
    }

    private fun loadWallpaperFile() {
        viewModelScope.launch {
            wallpaperImage = withContext(Dispatchers.IO) { Wallpapers.decode(wallpaperFile)?.asImageBitmap() }
        }
    }

    override fun onCleared() {
        networkMonitor.stop()
    }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }
}
