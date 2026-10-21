package com.example.calendarevaht.ui.shifts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.Person
import com.example.calendarevaht.domain.ShiftOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.calendarevaht.data.WorkCalendarRepository
class ShiftsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = ShiftPreferences(app)
    private val _localTemplates = MutableStateFlow<List<CycleTemplate>?>(null)

    private val _forecastMonths = MutableStateFlow(6)
    val forecastMonths: StateFlow<Int> = _forecastMonths

    val templates = combine(prefs.templates, _localTemplates) { dataStoreList, localList ->
        localList ?: dataStoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val persons = prefs.persons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            prefs.migrateIfNeeded()

            // Загружаем производственный календарь в фоне
            val calendarRepo = WorkCalendarRepository(getApplication())
            calendarRepo.ensureCalendarLoaded()
        }
    }

    fun setForecastMonths(months: Int) {
        _forecastMonths.value = months.coerceIn(3, 36)
    }

    // === ВАХТЫ ===

    private suspend fun saveTemplates(list: List<CycleTemplate>) {
        _localTemplates.value = list
        prefs.saveTemplates(list)
    }

    fun add(template: CycleTemplate) {
        viewModelScope.launch {
            val current = templates.value
            val newItem = template.copy(id = nextId(current))
            // При добавлении новой вахты проверяем, не нужно ли сделать её "моей"
            val updated = if (newItem.assignedPersonIds.any { id -> persons.value.find { it.id == id }?.isMe == true }) {
                // Если в новой вахте есть "я", делаем её моей
                current.map { it.copy(isMine = false) } + newItem.copy(isMine = true)
            } else {
                current + newItem
            }
            saveTemplates(updated)
        }
    }

    fun update(template: CycleTemplate) {
        viewModelScope.launch {
            val updated = templates.value.map { t ->
                if (t.id == template.id) template else t
            }
            // Пересчитываем isMine для всех вахт
            val finalList = updated.map { t ->
                val hasMe = t.assignedPersonIds.any { id -> persons.value.find { it.id == id }?.isMe == true }
                t.copy(isMine = hasMe)
            }
            saveTemplates(finalList)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            saveTemplates(templates.value.filterNot { it.id == id })
        }
    }

    fun removeOverride(templateId: Long, originalStart: LocalDate) {
        viewModelScope.launch {
            val updated = templates.value.map { t ->
                if (t.id != templateId) return@map t
                t.copy(overrides = t.overrides.filterNot { it.originalStart == originalStart })
            }
            saveTemplates(updated)
        }
    }

    private fun nextId(list: List<CycleTemplate>): Long =
        (list.maxOfOrNull { it.id } ?: 0L) + 1L

    // === ЛЮДИ ===

    fun addPerson(fullName: String, position: String, isMe: Boolean) {
        viewModelScope.launch {
            val currentPersons = persons.value
            var updatedPersons = currentPersons

            // Если ставим isMe = true, снимаем у остальных
            if (isMe) {
                updatedPersons = updatedPersons.map { it.copy(isMe = false) }
            }

            val newId = (updatedPersons.maxOfOrNull { it.id } ?: 0L) + 1L
            val newPerson = Person(id = newId, fullName = fullName, position = position, isMe = isMe)
            prefs.savePersons(updatedPersons + newPerson)

            // Если добавили "себя", пересчитываем isMine у вахт
            if (isMe) {
                recalculateMineStatus()
            }
        }
    }

    fun updatePerson(person: Person, selectedShiftIds: List<Long>) {
        viewModelScope.launch {
            var currentPersons = persons.value

            // Если человек становится "мной", снимаем флаг у остальных
            if (person.isMe) {
                currentPersons = currentPersons.map { if (it.id != person.id) it.copy(isMe = false) else it }
            }

            val updatedPersons = currentPersons.map { if (it.id == person.id) person else it }
            prefs.savePersons(updatedPersons)

            // Обновляем привязку к вахтам
            val currentTemplates = templates.value
            val updatedTemplates = currentTemplates.map { t ->
                val wasAssigned = person.id in t.assignedPersonIds
                val shouldBeAssigned = person.id in selectedShiftIds

                if (shouldBeAssigned && !wasAssigned) {
                    t.copy(assignedPersonIds = t.assignedPersonIds + person.id)
                } else if (!shouldBeAssigned && wasAssigned) {
                    t.copy(assignedPersonIds = t.assignedPersonIds - person.id)
                } else {
                    t
                }
            }
            saveTemplates(updatedTemplates)

            // Пересчитываем "мои" вахты
            recalculateMineStatus()
        }
    }

    fun deletePerson(personId: Long) {
        viewModelScope.launch {
            // Удаляем из списка людей
            val updatedPersons = persons.value.filterNot { it.id == personId }
            prefs.savePersons(updatedPersons)

            // Удаляем из всех вахт
            val updatedTemplates = templates.value.map { t ->
                t.copy(assignedPersonIds = t.assignedPersonIds - personId)
            }
            saveTemplates(updatedTemplates)

            recalculateMineStatus()
        }
    }

    fun togglePersonToShift(shiftId: Long, personId: Long) {
        viewModelScope.launch {
            val updatedTemplates = templates.value.map { t ->
                if (t.id == shiftId) {
                    val newAssigned = if (personId in t.assignedPersonIds) {
                        t.assignedPersonIds - personId
                    } else {
                        t.assignedPersonIds + personId
                    }
                    t.copy(assignedPersonIds = newAssigned)
                } else {
                    t
                }
            }
            saveTemplates(updatedTemplates)
            recalculateMineStatus()
        }
    }

    /**
     * Пересчитывает флаг isMine у вахт на основе того, есть ли в них человек с isMe = true.
     * Также обновляет isMine, если человек "я" был удален.
     */
    private suspend fun recalculateMineStatus() {
        val currentPersons = persons.value
        val mePerson = currentPersons.find { it.isMe }

        val updatedTemplates = templates.value.map { t ->
            val hasMe = mePerson != null && mePerson.id in t.assignedPersonIds
            t.copy(isMine = hasMe)
        }
        saveTemplates(updatedTemplates)
    }
}