package com.example.calendarevaht.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.HolidayRepository
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.data.WeatherInfo
import com.example.calendarevaht.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val holidayRepo = HolidayRepository(app)
    private val _holidays = MutableStateFlow<Map<Int, Map<LocalDate, String>>>(emptyMap())
    val holidays: StateFlow<Map<Int, Map<LocalDate, String>>> = _holidays.asStateFlow()

    private val weatherRepo = WeatherRepository()
    private val settingsPrefs = SettingsPreferences(app)

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private var lastFetchTime = 0L

    init {
        viewModelScope.launch {
            settingsPrefs.settings.collect { settings ->
                val lat = settings.latitude
                val lon = settings.longitude
                if (lat != null && lon != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastFetchTime > TimeUnit.MINUTES.toMillis(30) || _weather.value == null) {
                        // Вызов fetch с Float, а он принимает Double, поэтому .toDouble() здесь нужен
                        _weather.value = weatherRepo.fetch(lat.toDouble(), lon.toDouble())
                        lastFetchTime = now
                    }
                } else {
                    _weather.value = null
                }
            }
        }
    }

    fun ensureHolidaysLoaded(year: Int) {
        viewModelScope.launch {
            if (_holidays.value[year] != null) return@launch
            val map = holidayRepo.load(year)
            _holidays.value = _holidays.value + (year to map)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val settings = settingsPrefs.settings.first()
            val lat = settings.latitude
            val lon = settings.longitude
            if (lat != null && lon != null) {
                _weather.value = weatherRepo.fetch(lat.toDouble(), lon.toDouble())
                lastFetchTime = System.currentTimeMillis()
            }
        }
    }
}