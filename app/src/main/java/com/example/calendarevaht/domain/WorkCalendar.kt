package com.example.calendarevaht.domain

import java.time.LocalDate

enum class DayType {
    WORKING,
    WEEKEND,
    PUBLIC_HOLIDAY,
    SHORTENED_WORKING,
    TRANSFERRED_DAY_OFF,
    UNKNOWN
}

data class WorkDayInfo(
    val date: LocalDate,
    val type: DayType,
    val isShortened: Boolean,
    val hours40h: Double,
    val holidayName: String? = null
)

data class MonthNorm(
    val month: Int,
    val calendarDays: Int,
    val workingDays: Int,
    val weekendDays: Int,
    val holidayDays: Int,
    val shortenedHours: Int,
    val normHours40h: Int
)