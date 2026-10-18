package com.example.calendarevaht.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DailyForecast(
    val date: String,
    val tempMin: Int,
    val tempMax: Int,
    val icon: String,
    val description: String
)

data class WeatherInfo(
    val temperature: Int,
    val windSpeed: Int,
    val icon: String,
    val description: String,
    val forecast: List<DailyForecast> = emptyList()
)

class WeatherRepository {

    private val apiKey = "b5ce26c2-413d-4e12-a824-7371f1db1bea"
    private val endpoint = "https://api.weather.yandex.ru/graphql/query"

    suspend fun fetch(lat: Double, lon: Double): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Yandex-Weather-Key", apiKey)

            // Только текущая погода, без forecast
            val query = """
                {
                  weatherByPoint(request: { lat: $lat, lon: $lon }) {
                    now {
                      temperature
                      windSpeed
                      condition
                    }
                  }
                }
            """.trimIndent()

            val body = JSONObject().put("query", query).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            println("Weather: запрос lat=$lat lon=$lon")

            if (conn.responseCode != 200) {
                println("Weather API error code: ${conn.responseCode}")
                return@withContext null
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            println("Weather API response: $text")

            val json = JSONObject(text)

            if (json.has("errors")) {
                println("Weather API errors: ${json.getJSONArray("errors").toString()}")
                return@withContext null
            }

            val data = json.getJSONObject("data").getJSONObject("weatherByPoint")
            val now = data.getJSONObject("now")

            val temp = now.getInt("temperature")
            val wind = now.getInt("windSpeed")
            val condition = now.getString("condition")
            val (icon, desc) = decodeCondition(condition)

            WeatherInfo(temp, wind, icon, desc, emptyList())

        } catch (e: Exception) {
            println("Weather exception: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun decodeCondition(condition: String): Pair<String, String> {
        return when (condition) {
            "clear" -> "☀️" to "Ясно"
            "partly-cloudy" -> "⛅" to "Малооблачно"
            "cloudy" -> "☁️" to "Облачно"
            "overcast" -> "☁️" to "Пасмурно"
            "drizzle" -> "🌦️" to "Морось"
            "light-rain" -> "🌧️" to "Небольшой дождь"
            "rain" -> "🌧️" to "Дождь"
            "heavy-rain" -> "🌧️" to "Сильный дождь"
            "showers" -> "🌧️" to "Ливень"
            "wet-snow" -> "🌨️" to "Дождь со снегом"
            "light-snow" -> "❄️" to "Небольшой снег"
            "snow" -> "❄️" to "Снег"
            "snow-showers" -> "🌨️" to "Снегопад"
            "hail" -> "🧊" to "Град"
            "thunderstorm" -> "⛈️" to "Гроза"
            "thunderstorm-with-rain" -> "⛈️" to "Дождь с грозой"
            "thunderstorm-with-hail" -> "⛈️" to "Гроза с градом"
            else -> "🌡️" to "—"
        }
    }
}