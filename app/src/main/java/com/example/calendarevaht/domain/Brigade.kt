package com.example.calendarevaht.domain

data class Brigade(
    val id: Long,
    val name: String,
    val personIds: List<Long> = emptyList()
)