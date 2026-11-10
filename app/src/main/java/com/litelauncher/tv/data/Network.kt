package com.litelauncher.tv.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class GeoResult(val name: String, val lat: Float, val lon: Float)

data class WeatherNow(val temperature: Int, val summary: String)

/** Weather from Open-Meteo: free, no API key. */
object Weather {

    fun findCity(query: String): GeoResult? {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(Http.getText("https://geocoding-api.open-meteo.com/v1/search?count=1&name=$encoded"))
        val first = json.optJSONArray("results")?.optJSONObject(0) ?: return null
        val country = first.optString("country_code")
        val name = first.getString("name") + if (country.isNotEmpty()) ", $country" else ""
        return GeoResult(name, first.getDouble("latitude").toFloat(), first.getDouble("longitude").toFloat())
    }

    fun current(lat: Float, lon: Float, fahrenheit: Boolean): WeatherNow {
        val unit = if (fahrenheit) "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code&temperature_unit=$unit"
        val current = JSONObject(Http.getText(url)).getJSONObject("current")
        return WeatherNow(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            summary = describe(current.getInt("weather_code")),
        )
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> ""
    }
}

object Wallpapers {

    private const val MAX_BYTES = 25L * 1024 * 1024

    fun download(url: String, target: File) {
        val connection = Http.open(url)
        try {
            save(connection.inputStream, target)
        } finally {
            connection.disconnect()
        }
    }

    /** Copies an image stream (from a link or a file picker) into the launcher's own storage. */
    fun save(source: InputStream, target: File) {
        val temp = File(target.parentFile, target.name + ".tmp")
        var total = 0L
        source.use { input ->
            temp.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) error("Image is too large")
                    output.write(buffer, 0, read)
                }
            }
        }
        if (decode(temp) == null) {
            temp.delete()
            error("Not an image")
        }
        if (!temp.renameTo(target)) error("Couldn't save image")
    }

    /** Decodes at roughly 1080p so a huge photo doesn't eat memory. */
    fun decode(file: File): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1920 && bounds.outHeight / (sample * 2) >= 1080) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}

object Http {

    fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        return connection
    }

    fun getText(url: String): String {
        val connection = open(url)
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
