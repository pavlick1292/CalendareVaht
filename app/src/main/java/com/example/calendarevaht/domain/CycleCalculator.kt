package com.example.calendarevaht.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CycleCalculator {

    fun expandCycleToDays(
        template: CycleTemplate,
        from: LocalDate,
        to: LocalDate
    ): List<ShiftEntry> {
        if (to.isBefore(from)) return emptyList()

        if (template.isOneTime) {
            val end = template.endDate ?: return emptyList()
            val realStart = maxOf(from, template.startDate)
            val realEnd = minOf(to, end)
            if (realEnd.isBefore(realStart)) return emptyList()
            return listOf(ShiftEntry(template.id, realStart, realEnd, template.baseColor, "вахта"))
        }

        // Генерируем базовую сетку ВСЕГДА от startDate до max(to, from + 5 лет)
        val generationEnd = maxOf(to, from.plusYears(5))
        val baseShifts = generateForwardShifts(template, template.startDate, generationEnd).toMutableList()

        if (baseShifts.isEmpty() && template.overrides.isEmpty()) return emptyList()

        val sortedOverrides = template.overrides.sortedBy { it.originalStart }

        for (ov in sortedOverrides) {
            val originalShift = baseShifts.firstOrNull { it.start == ov.originalStart } ?: continue

            baseShifts.remove(originalShift)

            val newEnd = ov.newStart.plusDays(ov.workDays.toLong() - 1)
            baseShifts.add(
                ShiftEntry(
                    id = originalShift.id,
                    start = ov.newStart,
                    end = newEnd,
                    color = template.baseColor,
                    label = "вахта"
                )
            )

            if (ov.shiftSubsequent) {
                val diffDays = ChronoUnit.DAYS.between(originalShift.start, ov.newStart).toInt()
                if (diffDays != 0) {
                    val subsequent = baseShifts.filter {
                        it.start.isAfter(originalShift.start) && it.start != ov.newStart
                    }
                    baseShifts.removeAll(subsequent)
                    subsequent.forEach { s ->
                        baseShifts.add(
                            s.copy(
                                start = s.start.plusDays(diffDays.toLong()),
                                end = s.end.plusDays(diffDays.toLong())
                            )
                        )
                    }
                }
            }
            // shiftSubsequent = false: просто оставляем старую сетку после originalShift
        }

        val sorted = baseShifts
            .filter { !it.end.isBefore(from) && !it.start.isAfter(to) }
            .sortedBy { it.start }

        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf<ShiftEntry>()
        var current = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (!next.start.isAfter(current.end.plusDays(1))) {
                current = current.copy(end = maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun generateForwardShifts(
        template: CycleTemplate,
        from: LocalDate,
        to: LocalDate
    ): List<ShiftEntry> {
        val list = mutableListOf<ShiftEntry>()
        val cycleLen = template.initialRule.workDays + template.initialRule.restDays
        if (cycleLen <= 0) return list

        // Жёстко стартуем от startDate шаблона, чтобы offset был корректным
        var d = maxOf(template.startDate, from)
        val endLimit = to

        while (!d.isAfter(endLimit)) {
            val offset = ChronoUnit.DAYS.between(template.startDate, d).toInt()
            val inCycle = ((offset % cycleLen) + cycleLen) % cycleLen
            val isWork = inCycle < template.initialRule.workDays

            if (isWork) {
                var end = d
                var next = d.plusDays(1)
                while (!next.isAfter(endLimit)) {
                    val off2 = ChronoUnit.DAYS.between(template.startDate, next).toInt()
                    val inCyc2 = ((off2 % cycleLen) + cycleLen) % cycleLen
                    if (inCyc2 < template.initialRule.workDays) {
                        end = next
                        next = next.plusDays(1)
                    } else break
                }
                list.add(ShiftEntry(template.id * 1000L + list.size, d, end, template.baseColor, "вахта"))
                d = end.plusDays(1)
            } else {
                d = d.plusDays(1)
            }
        }
        return list
    }
}