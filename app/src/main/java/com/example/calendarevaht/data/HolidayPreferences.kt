package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.time.LocalDate

class HolidayPreferences(private val context: Context) {

    private fun key(year: Int) = stringPreferencesKey("holidays_$year")

    suspend fun get(year: Int): Map<LocalDate, String>? {
        val prefs = context.dataStore.data.first()
        val raw = prefs[key(year)] ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val map = mutableMapOf<LocalDate, String>()
            obj.keys().forEach { k ->
                runCatching {
                    map[LocalDate.parse(k)] = obj.getString(k)
                }
            }
            map
        }.getOrNull()
    }

    suspend fun save(year: Int, dates: Map<LocalDate, String>) {
        val obj = JSONObject()
        dates.toSortedMap().forEach { (d, name) -> obj.put(d.toString(), name) }
        context.dataStore.edit { it[key(year)] = obj.toString() }
    }
}