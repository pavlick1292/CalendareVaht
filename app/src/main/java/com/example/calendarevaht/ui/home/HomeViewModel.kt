package com.example.calendarevaht.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.HolidayRepository
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.data.WeatherInfo
import com.example.calendarevaht.data.WeatherRepository
import com.example.calendarevaht.data.WorkCalendarRepository
import com.example.calendarevaht.domain.DayType
import com.example.calendarevaht.domain.WorkDayInfo
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

    private val workCalendarRepo = WorkCalendarRepository(getApplication())
    val workDaysMap = MutableStateFlow<Map<LocalDate, WorkDayInfo>>(emptyMap())

    private var lastFetchTime = 0L

    private fun mergeKnownHolidays(map: Map<LocalDate, WorkDayInfo>): Map<LocalDate, WorkDayInfo> {
        val result = map.toMutableMap()
        val holidayDates = listOf(
            "2027-01-01", "2027-01-02", "2027-01-03", "2027-01-04",
            "2027-01-05", "2027-01-06", "2027-01-07", "2027-01-08",
            "2027-02-23", "2027-03-08", "2027-05-01", "2027-05-09",
            "2027-06-12", "2027-11-04"
        ).map { LocalDate.parse(it) }

        val names = mapOf(
            1 to "Новогодние каникулы", 7 to "Рождество Христово",
            2 to "День защитника Отечества", 3 to "Международный женский день",
            5 to "Праздник Весны и Труда", 9 to "День Победы",
            6 to "День России", 11 to "День народного единства"
        )

        holidayDates.forEach { date ->
            val existing = result[date]
            if (existing == null || existing.type != DayType.PUBLIC_HOLIDAY) {
                result[date] = WorkDayInfo(
                    date = date,
                    type = DayType.PUBLIC_HOLIDAY,
                    isShortened = false,
                    hours40h = 0.0,
                    holidayName = names[date.monthValue] ?: "Праздник"
                )
            }
        }
        return result
    }

    init {
        viewModelScope.launch {
            workCalendarRepo.ensureCalendarLoaded()
            workDaysMap.value = mergeKnownHolidays(workCalendarRepo.getWorkDays())
        }
    }

    init {
        viewModelScope.launch {
            settingsPrefs.settings.collect { settings ->
                val lat = settings.latitude
                val lon = settings.longitude
                if (lat != null && lon != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastFetchTime > TimeUnit.MINUTES.toMillis(30) || _weather.value == null) {
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