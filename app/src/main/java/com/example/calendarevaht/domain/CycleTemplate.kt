package com.example.calendarevaht.domain

import java.time.LocalDate

data class CycleTemplate(
    val id: Long,
    val title: String,                        // было brigadeName
    val initialRule: CycleRule,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isOneTime: Boolean = false,
    val baseColor: Long,
    val overrides: List<ShiftOverride> = emptyList(),
    val countBackward: Boolean = false,
    val backwardLimitMonths: Int = 0,
    val isMine: Boolean = false,
    val assignedPersonIds: List<Long> = emptyList()  // новое
)