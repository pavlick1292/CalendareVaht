package com.example.calendarevaht.domain

import java.time.LocalDate

data class CycleTemplate(
    val id: Long,
    val rule: CycleRule,
    val startDate: LocalDate,
    val changeDate: LocalDate,
    val newRule: CycleRule,
    val baseColor: Long
)