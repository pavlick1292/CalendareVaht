package com.example.calendarevaht.data.network

import com.google.gson.annotations.SerializedName

data class WorkCalendarResponse(
    @SerializedName("version") val version: VersionDto?,
    @SerializedName("days") val days: List<DayDto>?
)

data class VersionDto(
    @SerializedName("year") val year: Int?
)

data class DayDto(
    @SerializedName("date") val date: String?, // "2026-01-01"
    @SerializedName("type") val type: String?, // "WORKING", "WEEKEND", "PUBLIC_HOLIDAY", "SHORTENED_WORKING", "TRANSFERRED_DAY_OFF"
    @SerializedName("isWorking") val isWorking: Boolean?,
    @SerializedName("shortened") val shortened: Boolean?,
    @SerializedName("hours") val hours: HoursDto?,
    @SerializedName("holidayName") val holidayName: String?
)

data class HoursDto(
    @SerializedName("40h") val h40: Double?
)