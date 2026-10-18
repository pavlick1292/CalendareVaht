package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map

data class AppSettings(
    val shiftPlace: String = "",
    val shiftHours: Int = 11,
    val latitude: Float? = null,
    val longitude: Float? = null
)

class SettingsPreferences(private val context: Context) {
    private val placeKey = stringPreferencesKey("shift_place")
    private val hoursKey = intPreferencesKey("shift_hours")
    private val latKey = floatPreferencesKey("shift_lat")
    private val lonKey = floatPreferencesKey("shift_lon")

    val settings = context.dataStore.data.map { prefs ->
        AppSettings(
            shiftPlace = prefs[placeKey] ?: "",
            shiftHours = prefs[hoursKey] ?: 11,
            latitude = prefs[latKey],
            longitude = prefs[lonKey]
        )
    }

    suspend fun savePlace(value: String) {
        context.dataStore.edit {
            it[placeKey] = value
            if (value.isBlank()) {
                it.remove(latKey)
                it.remove(lonKey)
            }
        }
    }

    suspend fun saveCoordinates(lat: Double, lon: Double) {
        context.dataStore.edit {
            it[latKey] = lat.toFloat()
            it[lonKey] = lon.toFloat()
        }
    }

    suspend fun saveHours(value: Int) {
        context.dataStore.edit { it[hoursKey] = value.coerceIn(1, 24) }
    }
}