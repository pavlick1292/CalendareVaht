package com.example.calendarevaht.domain

import java.time.LocalDate

data class ShiftEntry(
    val id: Long,
    val start: LocalDate,
    val end: LocalDate,
    val color: Long,       // цвет в формате 0xFF4CAF50
    val label: String = "" // необязательная подпись (работа/отдых)
)