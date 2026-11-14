package com.litelauncher.tv.ui

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.litelauncher.tv.LauncherViewModel
import com.litelauncher.tv.data.WALLPAPER_CUSTOM
import com.litelauncher.tv.data.WALLPAPER_SLIDESHOW
import com.litelauncher.tv.data.WALLPAPER_VIDEO
import com.litelauncher.tv.data.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun Wallpaper(vm: LauncherViewModel) {
    val settings = vm.settings
    val preset = WallpaperPresets.getOrElse(settings.wallpaper) { WallpaperPresets.first() }
    // The darkening layers exist to keep text readable; in wallpaper-only mode they fade away.
    val shade by animateFloatAsState(if (vm.ambient) 0f else 1f, tween(800), label = "shade")

    // The gradient is always underneath, so there is something to show while media loads.
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(preset.colors)),
    )

    val showsMedia = when (settings.wallpaper) {
        WALLPAPER_CUSTOM -> {
            vm.wallpaperImage?.let { FullScreenImage(it) }
            true
        }
        WALLPAPER_SLIDESHOW -> {
            Slideshow(files = vm.slideshowFiles, seconds = settings.slideSeconds, active = vm.active)
            true
        }
        WALLPAPER_VIDEO -> {
            // The player only exists while the launcher is on screen, so the video
            // decoder is free for whichever app you open.
            if (vm.active && settings.wallpaperPath.isNotEmpty()) {
                key(settings.wallpaperPath) {
                    VideoWallpaper(path = settings.wallpaperPath, onError = vm::onVideoError)
                }
            }
            true
        }
        else -> false
    }

    if (showsMedia) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f * shade)),
        )
    }
    // Darkens the lower half so tiles and labels stay readable on any wallpaper.
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.65f * shade),
                ),
            ),
    )
}

@Composable
private fun FullScreenImage(image: ImageBitmap) {
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

/** Shows each image in the folder in turn, fading between them. */
@Composable
private fun Slideshow(files: List<File>, seconds: Int, active: Boolean) {
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var index by remember(files) { mutableIntStateOf(0) }

    LaunchedEffect(files, seconds, active) {
        if (!active || files.isEmpty()) return@LaunchedEffect
        while (true) {
            val file = files[index % files.size]
            val decoded = withContext(Dispatchers.IO) {
                runCatching { Wallpapers.decode(file)?.asImageBitmap() }.getOrNull()
            }
            if (decoded != null) current = decoded
            if (files.size == 1) break
            delay(seconds.coerceAtLeast(5) * 1000L)
            index = (index + 1) % files.size
        }
    }

    Crossfade(targetState = current, animationSpec = tween(1200), label = "slideshow") { image ->
        if (image != null) FullScreenImage(image)
    }
}

/** Muted, looping video scaled to fill the screen. Uses Android's built-in player. */
@Composable
private fun VideoWallpaper(path: String, onError: () -> Unit) {
    val reportError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            TextureView(context).apply {
                val view = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var player: MediaPlayer? = null

                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                        val created = MediaPlayer()
                        player = created
                        runCatching {
                            created.setDataSource(path)
                            created.setSurface(Surface(texture))
                            created.isLooping = true
                            created.setVolume(0f, 0f)
                            created.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                fillView(view, videoWidth, videoHeight)
                            }
                            created.setOnPreparedListener { it.start() }
                            created.setOnErrorListener { _, _, _ ->
                                reportError()
                                true
                            }
                            created.prepareAsync()
                        }.onFailure { reportError() }
                    }

                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit

                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                        player?.release()
                        player = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                }
            }
        },
    )
}

/** Scales the video so it covers the whole view without stretching (centre crop). */
private fun fillView(view: TextureView, videoWidth: Int, videoHeight: Int) {
    val width = view.width.toFloat()
    val height = view.height.toFloat()
    if (width <= 0f || height <= 0f || videoWidth <= 0 || videoHeight <= 0) return
    val scale = maxOf(width / videoWidth, height / videoHeight)
    val matrix = Matrix()
    matrix.setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f)
    view.setTransform(matrix)
}
