
package com.example.calendarevaht.domain

data class Person(
    val id: Long,
    val fullName: String,
    val position: String = "", // Должность
    val isActive: Boolean = true
)