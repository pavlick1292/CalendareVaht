package com.example.calendarevaht.data

import android.content.Context
import android.util.Log
import com.example.calendarevaht.data.network.CalendarApi
import com.example.calendarevaht.data.network.WorkCalendarResponse
import com.example.calendarevaht.domain.DayType
import com.example.calendarevaht.domain.MonthNorm
import com.example.calendarevaht.domain.WorkDayInfo
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class WorkCalendarRepository(context: Context) {
    private val prefs = ShiftPreferences(context)
    private val gson = Gson()

    suspend fun ensureCalendarLoaded(): Boolean {
        val lastUpdate = prefs.lastCalendarUpdate.first()
        val now = System.currentTimeMillis()
        val daysSinceUpdate = TimeUnit.MILLISECONDS.toDays(now - lastUpdate)

        if (lastUpdate > 0 && daysSinceUpdate < 30) return true

        return try {
            val year = LocalDate.now().year
            val response = CalendarApi.service.getYear(year)
            val jsonString = gson.toJson(response)
            prefs.saveWorkCalendar(jsonString)
            Log.d("CalendarRepo", "Календарь за $year успешно скачан")
            true
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Ошибка скачивания календаря", e)
            lastUpdate > 0
        }
    }

    /**
     * Парсит JSON из кэша. Если нужен другой год, пробует скачать его.
     */
    suspend fun getWorkDays(year: Int = LocalDate.now().year): Map<LocalDate, WorkDayInfo> {
        // Если это текущий год, берем из кэша
        if (year == LocalDate.now().year) {
            val json = prefs.workCalendarJson.first() ?: return emptyMap()
            return parseJson(json)
        }

        // Если другой год — качаем с API (с кэшированием в память не заморачиваемся)
        return try {
            val response = CalendarApi.service.getYear(year)
            val jsonString = gson.toJson(response)
            parseJson(jsonString)
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Ошибка скачивания за $year", e)
            emptyMap()
        }
    }

    private fun parseJson(json: String): Map<LocalDate, WorkDayInfo> {
        return try {
            val response = gson.fromJson(json, WorkCalendarResponse::class.java)
            val map = mutableMapOf<LocalDate, WorkDayInfo>()

            response?.days?.forEach { dto ->
                val dateStr = dto.date ?: return@forEach
                val date = LocalDate.parse(dateStr)
                val type = when (dto.type) {
                    "WORKING" -> DayType.WORKING
                    "WEEKEND" -> DayType.WEEKEND
                    "PUBLIC_HOLIDAY" -> DayType.PUBLIC_HOLIDAY
                    "SHORTENED_WORKING" -> DayType.SHORTENED_WORKING
                    "TRANSFERRED_DAY_OFF" -> DayType.TRANSFERRED_DAY_OFF
                    else -> DayType.UNKNOWN
                }
                map[date] = WorkDayInfo(
                    date = date,
                    type = type,
                    isShortened = dto.shortened ?: false,
                    hours40h = dto.hours?.h40 ?: 0.0,
                    holidayName = dto.holidayName
                )
            }
            map
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Ошибка парсинга календаря", e)
            emptyMap()
        }
    }

    suspend fun getMonthlyNorms(year: Int = LocalDate.now().year): List<MonthNorm> {
        val daysMap = getWorkDays(year)
        if (daysMap.isEmpty()) return emptyList()

        return (1..12).map { m ->
            val ym = YearMonth.of(year, m)
            val daysInMonth = daysMap.filterKeys { YearMonth.from(it) == ym }.values

            val calendarDays = ym.lengthOfMonth()
            val workingDays = daysInMonth.count { it.type == DayType.WORKING || it.type == DayType.SHORTENED_WORKING }
            val weekendDays = daysInMonth.count { it.type == DayType.WEEKEND || it.type == DayType.TRANSFERRED_DAY_OFF }
            val holidayDays = daysInMonth.count { it.type == DayType.PUBLIC_HOLIDAY }
            val shortened = daysInMonth.count { it.isShortened }
            val normHours = daysInMonth.sumOf { it.hours40h }.toInt()

            MonthNorm(
                month = m,
                calendarDays = calendarDays,
                workingDays = workingDays,
                weekendDays = weekendDays,
                holidayDays = holidayDays,
                shortenedHours = shortened,
                normHours40h = normHours
            )
        }
    }
}