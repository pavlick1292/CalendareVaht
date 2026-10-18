package com.example.calendarevaht.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class HolidayRepository(private val context: Context) {

    private val prefs = HolidayPreferences(context)

    suspend fun load(year: Int): Map<LocalDate, String> {
        prefs.get(year)?.let { return it }

        val fetched = fetchFromApi(year)
        if (fetched.isNotEmpty()) {
            prefs.save(year, fetched)
        }
        return fetched
    }

    private suspend fun fetchFromApi(year: Int): Map<LocalDate, String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://calendar.kuzyak.in/api/calendar/$year/holidays")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                if (conn.responseCode != 200) return@runCatching emptyMap<LocalDate, String>()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseHolidays(body)
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyMap())
    }

    private fun parseHolidays(json: String): Map<LocalDate, String> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("holidays") ?: return emptyMap()
        val result = mutableMapOf<LocalDate, String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val dateStr = o.optString("date")
            val name = o.optString("name")
            if (dateStr.length >= 10) {
                runCatching {
                    result[LocalDate.parse(dateStr.substring(0, 10))] = name
                }
            }
        }
        return result
    }
}