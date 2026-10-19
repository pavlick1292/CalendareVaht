package com.example.calendarevaht.domain

import java.time.LocalDate

data class CycleTemplate(
    val id: Long,
    val initialRule: CycleRule,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isOneTime: Boolean,
    val baseColor: Long,
    val overrides: List<ShiftOverride> = emptyList(),
    val countBackward: Boolean = false,
    val backwardLimitMonths: Int = 0,
    val isMine: Boolean,
    val brigadeId: Long = 0L,
    val brigadeName: String = "Мой состав" // Снимок названия
)