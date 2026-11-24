package com.fanqieyu.deepfocus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

enum class WeatherType {
    SUNNY,
    CLOUDY,
    OVERCAST,
    RAIN,
    STORM,
    SNOW,
}

data class WeatherUiState(
    val cityName: String = "定位中...",
    val temperatureC: Int? = null,
    val feelsLikeC: Int? = null,
    val condition: String = "获取天气中",
    val humidity: Int? = null,
    val windDesc: String = "",
    val weatherType: WeatherType = WeatherType.CLOUDY,
    val isLoading: Boolean = true,
    val locationSource: String = "",
)

object LocationWeatherManager {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun fetchWeather(context: Context): WeatherUiState = withContext(Dispatchers.IO) {
        val geoPoint = getSystemLocation(context) ?: fetchIpLocation()
        if (geoPoint == null) {
            return@withContext WeatherUiState(
                cityName = "定位未获取",
                temperatureC = null,
                condition = "点击刷新定位与天气",
                isLoading = false,
                locationSource = "离线",
            )
        }
        val (lat, lon, fallbackCity, source) = geoPoint
        val resolvedCity = resolveCityName(context, lat, lon) ?: fallbackCity.ifBlank { "当前位置" }
        val forecast = fetchOpenMeteo(lat, lon)
        if (forecast == null) {
            return@withContext WeatherUiState(
                cityName = resolvedCity,
                temperatureC = null,
                condition = "天气服务暂不可达",
                isLoading = false,
                locationSource = source,
            )
        }
        forecast.copy(
            cityName = resolvedCity,
            isLoading = false,
            locationSource = source,
        )
    }

    private data class GeoPoint(
        val lat: Double,
        val lon: Double,
        val city: String,
        val source: String,
    )

    @SuppressLint("MissingPermission")
    private fun getSystemLocation(context: Context): GeoPoint? {
        if (!hasLocationPermission(context)) return null
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            var best: Location? = null
            for (provider in providers) {
                val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
                if (best == null || loc.time > best.time) {
                    best = loc
                }
            }
            best?.let { GeoPoint(it.latitude, it.longitude, "", "系统定位") }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(context: Context, lat: Double, lon: Double): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val addr = addresses?.firstOrNull() ?: return null
        listOfNotNull(addr.locality, addr.subAdminArea, addr.adminArea)
            .firstOrNull { it.isNotBlank() }
    }.getOrNull()

    private fun fetchIpLocation(): GeoPoint? {
        runCatching {
            val body = httpGet("https://get.geojs.io/v1/ip/geo.json") ?: return@runCatching null
            val json = JSONObject(body)
            val lat = json.optString("latitude").toDoubleOrNull() ?: return@runCatching null
            val lon = json.optString("longitude").toDoubleOrNull() ?: return@runCatching null
            val city = json.optString("city").ifBlank { json.optString("region") }
            GeoPoint(lat, lon, city, "网络定位")
        }.getOrNull()?.let { return it }

        return runCatching {
            val body = httpGet("https://ipwho.is/") ?: return@runCatching null
            val json = JSONObject(body)
            if (!json.optBoolean("success", true)) return@runCatching null
            val lat = json.optDouble("latitude", Double.NaN)
            val lon = json.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return@runCatching null
            val city = json.optString("city").ifBlank { json.optString("region") }
            GeoPoint(lat, lon, city, "网络定位")
        }.getOrNull()
    }

    private fun fetchOpenMeteo(lat: Double, lon: Double): WeatherUiState? = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=auto"
        val body = httpGet(url) ?: return null
        val current = JSONObject(body).optJSONObject("current") ?: return null
        val temp = current.optDouble("temperature_2m", Double.NaN)
        if (temp.isNaN()) return null
        val feels = current.optDouble("apparent_temperature", temp)
        val humidity = current.optInt("relative_humidity_2m", -1).takeIf { it >= 0 }
        val wind = current.optDouble("wind_speed_10m", -1.0)
        val wmoCode = current.optInt("weather_code", 2)
        val (condition, type) = mapWmoCode(wmoCode)
        val windText = if (wind >= 0.0) "风速 ${wind.roundToInt()} km/h" else ""
        WeatherUiState(
            temperatureC = temp.roundToInt(),
            feelsLikeC = feels.roundToInt(),
            condition = condition,
            humidity = humidity,
            windDesc = windText,
            weatherType = type,
            isLoading = false,
        )
    }.getOrNull()

    private fun mapWmoCode(code: Int): Pair<String, WeatherType> = when (code) {
        0 -> "晴" to WeatherType.SUNNY
        1, 2 -> "多云" to WeatherType.CLOUDY
        3, 45, 48 -> "阴" to WeatherType.OVERCAST
        in 51..67, in 80..82 -> "雨" to WeatherType.RAIN
        in 71..77, 85, 86 -> "雪" to WeatherType.SNOW
        95, 96, 99 -> "雷阵雨" to WeatherType.STORM
        else -> "多云" to WeatherType.CLOUDY
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeepFocus/1.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
