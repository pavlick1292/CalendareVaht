package com.example.calendarevaht.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.AppSettings
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.data.ThemePreferences
import com.example.calendarevaht.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val themePrefs = ThemePreferences(app)
    private val settingsPrefs = SettingsPreferences(app)

    val theme = themePrefs.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.LIGHT
    )

    val settings = settingsPrefs.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    data class GeoOption(
        val lat: Double,
        val lon: Double,
        val label: String
    )

    private val _options = MutableStateFlow<List<GeoOption>>(emptyList())
    val options: StateFlow<List<GeoOption>> = _options.asStateFlow()

    fun saveTheme(value: AppTheme) {
        viewModelScope.launch { themePrefs.save(value) }
    }

    fun savePlace(value: String) {
        viewModelScope.launch {
            if (value.isBlank()) {
                _message.value = null
                _options.value = emptyList()
                return@launch
            }

            _message.value = "Ищу координаты…"
            _options.value = emptyList()

            val result = geocode(value)
            if (result.isNullOrEmpty()) {
                _message.value = "Город не найден или нет интернета"
            } else {
                _options.value = result
                _message.value = "Выберите нужный вариант:"
            }
        }
    }

    fun selectOption(option: GeoOption) {
        viewModelScope.launch {
            settingsPrefs.savePlace(option.label)
            settingsPrefs.saveCoordinates(option.lat, option.lon)
            _options.value = emptyList()
            _message.value = "Сохранено: ${option.label}"
        }
    }

    fun saveHours(value: Int) {
        viewModelScope.launch { settingsPrefs.saveHours(value) }
    }

    private suspend fun geocode(city: String): List<GeoOption>? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(city, "UTF-8")
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=ru&format=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode != 200) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(text).optJSONArray("results") ?: return@withContext null

            val list = mutableListOf<GeoOption>()
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val name = o.optString("name")
                val region = o.optString("admin1", "")
                val country = o.optString("country", "")

                val label = buildString {
                    append(name)
                    if (region.isNotBlank()) append(", $region")
                    if (country.isNotBlank()) append(", $country")
                }

                list.add(GeoOption(o.getDouble("latitude"), o.getDouble("longitude"), label))
            }
            list
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}