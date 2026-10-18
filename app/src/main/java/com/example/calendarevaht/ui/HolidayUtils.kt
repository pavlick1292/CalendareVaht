package com.example.calendarevaht.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun groupHolidaysForDisplay(holidays: Map<LocalDate, String>): List<String> {
    if (holidays.isEmpty()) return emptyList()

    val sorted = holidays.toSortedMap()
    val result = mutableListOf<String>()
    val fmt = DateTimeFormatter.ofPattern("dd.MM")

    var currentStart: LocalDate? = null
    var currentEnd: LocalDate? = null
    var currentName: String? = null

    fun flush() {
        val start = currentStart
        val end = currentEnd
        val name = currentName
        if (start != null && end != null && name != null) {
            val dateStr = if (start == end) {
                start.format(fmt)
            } else {
                "${start.format(fmt)} – ${end.format(fmt)}"
            }
            result.add("$dateStr — $name")
        }
        currentStart = null
        currentEnd = null
        currentName = null
    }

    for ((date, name) in sorted) {
        // Проверяем, относится ли день к новогодним каникулам (1-8 января)
        val isNewYear = date.monthValue == 1 && date.dayOfMonth in 1..8
        // Если это 7 января, сохраняем его настоящее имя ("Рождество Христово")
        val displayName = if (isNewYear && date.dayOfMonth != 7) "Новогодние каникулы" else name

        if (currentStart == null) {
            currentStart = date
            currentEnd = date
            currentName = displayName
        } else {
            val isNextDay = date == currentEnd?.plusDays(1)
            val isSameName = displayName == currentName

            if (isNextDay && isSameName) {
                currentEnd = date
            } else {
                flush()
                currentStart = date
                currentEnd = date
                currentName = displayName
            }
        }
    }
    flush()

    // Добавляем Рождество отдельной строкой, если оно было в списке
    val christmas = sorted.entries.find { it.key.monthValue == 1 && it.key.dayOfMonth == 7 }
    if (christmas != null) {
        result.add("07.01 — ${christmas.value}")
    }

    return result.distinct()
}