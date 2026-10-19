package com.example.calendarevaht.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.HolidayRepository
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CompositionStats(
    val title: String,
    val color: Long,
    val workDays: Int,
    val hours: Int,
    val weekendsOnShift: Int,
    val holidaysOnShift: Int,
    val restDays: Int
)

data class MonthPoint(
    val month: Int,
    val mineHours: Int,
    val otherHours: Int
)

data class StatsUiState(
    val mode: StatsMode = StatsMode.MONTH,
    val month: YearMonth = YearMonth.now(),
    val year: Int = YearMonth.now().year,
    val mine: CompositionStats? = null,
    val other: CompositionStats? = null,
    val monthly: List<MonthPoint> = emptyList(),
    val loading: Boolean = true
)

enum class StatsMode { MONTH, YEAR }

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val shiftPrefs = ShiftPreferences(app)
    private val settingsPrefs = SettingsPreferences(app)
    private val holidayRepo = HolidayRepository(app)

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun setMode(mode: StatsMode) {
        _state.value = _state.value.copy(mode = mode)
        reload()
    }

    fun changeMonth(delta: Long) {
        val newMonth = _state.value.month.plusMonths(delta)
        _state.value = _state.value.copy(month = newMonth, year = newMonth.year)
        reload()
    }

    fun changeYear(delta: Int) {
        val newYear = _state.value.year + delta
        _state.value = _state.value.copy(year = newYear, month = YearMonth.of(newYear, _state.value.month.monthValue))
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)

            val templates = shiftPrefs.templates.first()
            val settings = settingsPrefs.settings.first()
            val hoursPerDay = settings.shiftHours

            val s = _state.value
            val (from, to) = when (s.mode) {
                StatsMode.MONTH -> s.month.atDay(1) to s.month.atDay(s.month.lengthOfMonth())
                StatsMode.YEAR -> LocalDate.of(s.year, 1, 1) to LocalDate.of(s.year, 12, 31)
            }
            val holidays = holidayRepo.load(s.year)

            val mineTemplate = templates.firstOrNull { it.isMine }
            val otherTemplate = templates.firstOrNull { !it.isMine }

            val mineTitle = mineTemplate?.brigadeName?.ifBlank { "Мой состав" } ?: "Мой состав"
            val otherTitle = otherTemplate?.brigadeName?.ifBlank { "Сменщик" } ?: "Сменщик"

            val mine = mineTemplate?.let { buildStats(it, mineTitle, from, to, hoursPerDay, holidays) }
            val other = otherTemplate?.let { buildStats(it, otherTitle, from, to, hoursPerDay, holidays) }

            val monthly = buildMonthlyPoints(templates, s.year, hoursPerDay)

            _state.value = s.copy(
                mine = mine,
                other = other,
                monthly = monthly,
                loading = false
            )
        }
    }

    private fun buildStats(
        template: CycleTemplate,
        title: String,
        from: LocalDate,
        to: LocalDate,
        hoursPerDay: Int,
        holidays: Map<LocalDate, String>
    ): CompositionStats {
        val days = mutableSetOf<LocalDate>()
        CycleCalculator.expandCycleToDays(template, from, to).forEach { e ->
            var d = e.start
            while (!d.isAfter(e.end)) {
                if (!d.isBefore(from) && !d.isAfter(to)) days.add(d)
                d = d.plusDays(1)
            }
        }
        val workDays = days.size
        val weekendsOnShift = days.count { it.dayOfWeek.value >= 6 }
        val holidaysOnShift = days.count { it in holidays.keys }
        val restDays = (to.toEpochDay() - from.toEpochDay() + 1).toInt() - workDays
        return CompositionStats(
            title = title,
            color = template.baseColor,
            workDays = workDays,
            hours = workDays * hoursPerDay,
            weekendsOnShift = weekendsOnShift,
            holidaysOnShift = holidaysOnShift,
            restDays = restDays
        )
    }

    private fun buildMonthlyPoints(
        templates: List<CycleTemplate>,
        year: Int,
        hoursPerDay: Int
    ): List<MonthPoint> {
        val mineTemplate = templates.firstOrNull { it.isMine }
        val otherTemplate = templates.firstOrNull { !it.isMine }
        return (1..12).map { m ->
            val month = YearMonth.of(year, m)
            val from = month.atDay(1)
            val to = month.atDay(month.lengthOfMonth())
            val mineHours = mineTemplate?.let { countDays(it, from, to) * hoursPerDay } ?: 0
            val otherHours = otherTemplate?.let { countDays(it, from, to) * hoursPerDay } ?: 0
            MonthPoint(month = m, mineHours = mineHours, otherHours = otherHours)
        }
    }

    private fun countDays(template: CycleTemplate, from: LocalDate, to: LocalDate): Int {
        val set = mutableSetOf<LocalDate>()
        CycleCalculator.expandCycleToDays(template, from, to).forEach { e ->
            var d = e.start
            while (!d.isAfter(e.end)) {
                if (!d.isBefore(from) && !d.isAfter(to)) set.add(d)
                d = d.plusDays(1)
            }
        }
        return set.size
    }
}