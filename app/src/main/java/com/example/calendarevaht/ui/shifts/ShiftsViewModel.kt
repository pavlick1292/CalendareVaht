package com.example.calendarevaht.ui.shifts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.ShiftOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShiftsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = ShiftPreferences(app)
    private val _localTemplates = MutableStateFlow<List<CycleTemplate>?>(null)

    // Состояние горизонта (по умолчанию 6 месяцев)
    private val _forecastMonths = MutableStateFlow(6)
    val forecastMonths: StateFlow<Int> = _forecastMonths

    val templates = combine(prefs.templates, _localTemplates) { dataStoreList, localList ->
        localList ?: dataStoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            val current = prefs.templates.first()
            if (current.isEmpty()) {
                seedTestData()
            }
        }
    }

    fun setForecastMonths(months: Int) {
        _forecastMonths.value = months.coerceIn(3, 36) // Ограничиваем от 3 мес до 3 лет
    }

    private fun seedTestData() {
        viewModelScope.launch {
            val myShift = CycleTemplate(
                id = 1L,
                initialRule = CycleRule(workDays = 23, restDays = 19),
                startDate = LocalDate.of(2026, 10, 14),
                endDate = null,
                isOneTime = false,
                baseColor = 0xFF4CAF50,
                overrides = emptyList(),
                countBackward = false, // Отключили
                backwardLimitMonths = 0,
                isMine = true,
                customName = "Мой состав"
            )

            val otherShift = CycleTemplate(
                id = 2L,
                initialRule = CycleRule(workDays = 23, restDays = 19),
                startDate = LocalDate.of(2026, 9, 23),
                endDate = null,
                isOneTime = false,
                baseColor = 0xFFFF9800,
                overrides = emptyList(),
                countBackward = false, // Отключили
                backwardLimitMonths = 0,
                isMine = false,
                customName = "Сменщик"
            )

            saveAndSync(listOf(myShift, otherShift))
        }
    }

    private suspend fun saveAndSync(list: List<CycleTemplate>) {
        _localTemplates.value = list
        prefs.save(list)
    }

    fun add(template: CycleTemplate) {
        viewModelScope.launch {
            val current = templates.value
            val newItem = template.copy(id = nextId(current))
            val updated = if (newItem.isMine) {
                current.map { it.copy(isMine = false) } + newItem
            } else {
                current + newItem
            }
            saveAndSync(updated)
        }
    }

    fun update(template: CycleTemplate) {
        viewModelScope.launch {
            val updated = templates.value.map { t ->
                when {
                    t.id == template.id -> template
                    template.isMine -> t.copy(isMine = false)
                    else -> t
                }
            }
            val finalList = if (updated.count { it.isMine } > 1) {
                updated.map { t -> if (t.id != template.id && t.isMine) t.copy(isMine = false) else t }
            } else updated
            saveAndSync(finalList)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            saveAndSync(templates.value.filterNot { it.id == id })
        }
    }

    fun setMine(id: Long) {
        viewModelScope.launch {
            saveAndSync(templates.value.map { it.copy(isMine = it.id == id) })
        }
    }

    fun removeOverride(templateId: Long, originalStart: LocalDate) {
        viewModelScope.launch {
            val updated = templates.value.map { t ->
                if (t.id != templateId) return@map t
                t.copy(overrides = t.overrides.filterNot { it.originalStart == originalStart })
            }
            saveAndSync(updated)
        }
    }

    private fun nextId(list: List<CycleTemplate>): Long =
        (list.maxOfOrNull { it.id } ?: 0L) + 1L
}