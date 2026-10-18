package com.example.calendarevaht.domain

import java.time.LocalDate

data class CycleRuleChange(
    val fromDate: LocalDate,
    val rule: CycleRule
)