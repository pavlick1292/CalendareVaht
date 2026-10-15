package com.example.calendarevaht.domain

import java.time.LocalDate

class CycleCalculator {

    fun expandCycleToDays(
        template: CycleTemplate,
        from: LocalDate,
        to: LocalDate
    ): List<ShiftEntry> {
        val result = mutableListOf<ShiftEntry>()
        var current = from
        var id = 1L

        while (!current.isAfter(to)) {
            val rule = if (current.isBefore(template.changeDate)) {
                template.rule
            } else {
                template.newRule
            }

            val cycleLength = rule.workDays + rule.restDays
            val daysSinceStart = current.toEpochDay() - template.startDate.toEpochDay()
            val dayInCycle = (daysSinceStart % cycleLength).toInt()

            val isWorkDay = dayInCycle < rule.workDays

            if (isWorkDay) {
                result.add(
                    ShiftEntry(
                        id = id++,
                        start = current,
                        end = current,
                        color = template.baseColor,
                        label = "Работа"
                    )
                )
            }

            current = current.plusDays(1)
        }

        return result
    }
}