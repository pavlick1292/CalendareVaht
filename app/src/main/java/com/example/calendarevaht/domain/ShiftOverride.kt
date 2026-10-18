package com.example.calendarevaht.domain

import java.time.LocalDate

data class ShiftOverride(
    val originalStart: LocalDate,
    val newStart: LocalDate,
    val workDays: Int,
    val restDays: Int,
    val shiftSubsequent: Boolean
)