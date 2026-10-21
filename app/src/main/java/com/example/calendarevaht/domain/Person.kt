package com.example.calendarevaht.domain

data class Person(
    val id: Long,
    val fullName: String,
    val position: String,
    val isMe: Boolean = false
)