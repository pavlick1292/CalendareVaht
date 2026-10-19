package com.example.calendarevaht.ui.shifts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.domain.Brigade
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

class ShiftsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = ShiftPreferences(app)
    private val _localTemplates = MutableStateFlow<List<CycleTemplate>?>(null)

    // Состояние горизонта
    private val _forecastMonths = MutableStateFlow(6)
    val forecastMonths: StateFlow<Int> = _forecastMonths

    // Данные о вахтах
    val templates = combine(prefs.templates, _localTemplates) { dataStoreList, localList ->
        localList ?: dataStoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Данные о бригадах и людях (для UI)
    val brigades = prefs.brigades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persons = prefs.persons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            prefs.migrateIfNeeded()
            val current = prefs.templates.first()
            if (current.isEmpty()) {
                seedTestData()
            }
        }
    }

    fun setForecastMonths(months: Int) {
        _forecastMonths.value = months.coerceIn(3, 36)
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
                countBackward = false,
                backwardLimitMonths = 0,
                isMine = true,
                brigadeId = 1L,
                brigadeName = "Мой состав"
            )

            val otherShift = CycleTemplate(
                id = 2L,
                initialRule = CycleRule(workDays = 23, restDays = 19),
                startDate = LocalDate.of(2026, 9, 23),
                endDate = null,
                isOneTime = false,
                baseColor = 0xFFFF9800,
                overrides = emptyList(),
                countBackward = false,
                backwardLimitMonths = 0,
                isMine = false,
                brigadeId = 2L,
                brigadeName = "Сменщик"
            )

            // Создаем тестовые данные для людей и бригад
            val personsList = listOf(
                Person(id = 1L, fullName = "Иванов Иван", position = "Машинист"),
                Person(id = 2L, fullName = "Петров Петр", position = "Помощник"),
                Person(id = 3L, fullName = "Сидоров Сидор", position = "Водитель")
            )
            val brigadesList = listOf(
                Brigade(id = 1L, name = "Мой состав", personIds = listOf(1L, 2L)),
                Brigade(id = 2L, name = "Сменщик", personIds = listOf(3L))
            )

            savePersonsAndBrigades(personsList, brigadesList)
            saveAndSync(listOf(myShift, otherShift))
        }
    }

    // Сохранение списка вахт
    private suspend fun saveAndSync(list: List<CycleTemplate>) {
        _localTemplates.value = list
        prefs.saveTemplates(list)
    }

    // Сохранение людей и бригад
    private suspend fun savePersonsAndBrigades(personsList: List<Person>, brigadesList: List<Brigade>) {
        prefs.savePersons(personsList)
        prefs.saveBrigades(brigadesList)
    }

    // Методы для добавления/обновления людей и бригад (пока используются для тестов)
    fun addPerson(person: Person) {
        viewModelScope.launch {
            val current = persons.value
            prefs.savePersons(current + person)
        }
    }

    fun addBrigade(brigade: Brigade) {
        viewModelScope.launch {
            val current = brigades.value
            prefs.saveBrigades(current + brigade)
        }
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
    // === ДОБАВЛЕНИЕ И ОБНОВЛЕНИЕ БРИГАД / ЛЮДЕЙ ===

    fun addPersonToBrigade(brigadeId: Long, fullName: String, position: String) {
        viewModelScope.launch {
            val currentPersons = persons.value
            val newId = (currentPersons.maxOfOrNull { it.id } ?: 0L) + 1L
            val newPerson = Person(id = newId, fullName = fullName, position = position)

            // Сохраняем человека
            prefs.savePersons(currentPersons + newPerson)

            // Добавляем его ID в бригаду
            val currentBrigades = brigades.value
            val updatedBrigades = currentBrigades.map { b ->
                if (b.id == brigadeId) b.copy(personIds = b.personIds + newId) else b
            }
            prefs.saveBrigades(updatedBrigades)
        }
    }
    fun updatePerson(person: Person) {
        viewModelScope.launch {
            val currentPersons = persons.value.map { if (it.id == person.id) person else it }
            prefs.savePersons(currentPersons)
        }
    }
    fun addBrigade(name: String) {
        viewModelScope.launch {
            val current = brigades.value
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val newBrigade = Brigade(id = newId, name = name, personIds = emptyList())
            prefs.saveBrigades(current + newBrigade)
            fun updatePerson(person: Person) {
                viewModelScope.launch {
                    val currentPersons = persons.value.map { if (it.id == person.id) person else it }
                    prefs.savePersons(currentPersons)
                }
            }

            fun movePersonToBrigade(personId: Long, newBrigadeId: Long?) {
                viewModelScope.launch {
                    val currentBrigades = brigades.value.map { b ->
                        // Удаляем человека из всех бригад
                        val updatedIds = b.personIds.filterNot { it == personId }
                        // Если это та бригада, куда мы переводим — добавляем его
                        if (b.id == newBrigadeId) {
                            b.copy(personIds = updatedIds + personId)
                        } else {
                            b.copy(personIds = updatedIds)
                        }
                    }
                    prefs.saveBrigades(currentBrigades)
                }
            } }
    }

    fun movePersonToBrigade(id: Long, newBrigadeId: Long?) {}
}