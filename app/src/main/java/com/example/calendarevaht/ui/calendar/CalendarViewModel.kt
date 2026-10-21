package com.example.calendarevaht.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.data.WorkCalendarRepository
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.Person
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.domain.WorkDayInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year

data class CalendarUiState(
    val year: Int = Year.now().value,
    val templates: List<CycleTemplate> = emptyList(),
    val persons: List<Person> = emptyList(),
    val days: Map<LocalDate, List<ShiftEntry>> = emptyMap(),
    val workDaysMap: Map<LocalDate, WorkDayInfo> = emptyMap(), // Заменили holidays
    val shiftHours: Int = 11
) {
    val mine: CycleTemplate? get() = templates.firstOrNull { it.isMine }
    val other: CycleTemplate? get() = templates.firstOrNull { !it.isMine }
}

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val shiftPrefs = ShiftPreferences(app)
    private val settingsPrefs = SettingsPreferences(app)
    private val workCalendarRepo = WorkCalendarRepository(app)

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        reload(_state.value.year)
    }

    fun changeYear(delta: Int) {
        reload(_state.value.year + delta)
    }

    fun reload(year: Int = _state.value.year) {
        viewModelScope.launch {
            val templates = shiftPrefs.templates.first()
            val persons = shiftPrefs.persons.first()
            val settings = settingsPrefs.settings.first()

            val from = LocalDate.of(year, 1, 1)
            val to = LocalDate.of(year, 12, 31)
            val queryFrom = from.minusMonths(13)
            val queryTo = to.plusMonths(1)

            val map = mutableMapOf<LocalDate, MutableList<ShiftEntry>>()
            templates.forEach { t ->
                CycleCalculator.expandCycleToDays(t, queryFrom, queryTo).forEach { e ->
                    var d = e.start
                    while (!d.isAfter(e.end)) {
                        if (!d.isBefore(from) && !d.isAfter(to)) {
                            map.getOrPut(d) { mutableListOf() }.add(e)
                        }
                        d = d.plusDays(1)
                    }
                }
            }

            // Берем календарь из репозитория
            val workDaysMap = workCalendarRepo.getWorkDays()

            _state.value = CalendarUiState(
                year = year,
                templates = templates,
                persons = persons,
                days = map.mapValues { it.value.toList() },
                workDaysMap = workDaysMap, // Заменили holidays
                shiftHours = settings.shiftHours
            )
        }
    }
}