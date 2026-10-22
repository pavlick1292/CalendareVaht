package com.example.calendarevaht.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarevaht.domain.DayType
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.domain.WorkDayInfo
import java.time.LocalDate
import java.time.YearMonth

private val HolidayRed = Color(0xFFE53935)
// Светлая тема — светло-желтый, Темная — темно-янтарный
private val ShortenedYellowLight = Color(0xFFFFF59D)
private val ShortenedYellowDark = Color(0xFFB26A00) // Глубокий янтарный

@Composable
fun MiniMonth(
    yearMonth: YearMonth,
    days: Map<LocalDate, List<ShiftEntry>>,
    workDaysMap: Map<LocalDate, WorkDayInfo>,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(modifier = modifier) {
        Text(
            text = yearMonth.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Заголовки дней недели
        Row(Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                Text(
                    text = it,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Сетка дней
        val firstDay = yearMonth.atDay(1)
        val leadingEmpty = (firstDay.dayOfWeek.value + 6) % 7
        val totalDays = yearMonth.lengthOfMonth()

        for (row in 0 until 6) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - leadingEmpty + 1
                    if (dayIndex in 1..totalDays) {
                        val date = yearMonth.atDay(dayIndex)
                        val shifts = days[date].orEmpty()
                        val info = workDaysMap[date]

                        val isHoliday = info?.holidayName != null || info?.type == DayType.PUBLIC_HOLIDAY
                        val isWeekend = (info?.type == DayType.WEEKEND || info?.type == DayType.TRANSFERRED_DAY_OFF || date.dayOfWeek.value >= 6) && !isHoliday
                        val isShortened = info?.isShortened == true || info?.type == DayType.SHORTENED_WORKING

                        // Цвет фона
                        val bgColor = when {
                            shifts.isNotEmpty() -> Color(shifts.first().color)
                            isShortened -> if (isDarkTheme) ShortenedYellowDark else ShortenedYellowLight
                            else -> Color.Transparent
                        }

                        // Цвет текста
                        val textColor = when {
                            shifts.isNotEmpty() -> Color.White
                            isShortened && isDarkTheme -> Color.White
                            isHoliday || isWeekend -> HolidayRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Сама цифра
                            Text(
                                text = dayIndex.toString(),
                                fontSize = 9.sp,
                                color = textColor,
                                fontWeight = if (isHoliday || isWeekend || isShortened) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )

                            // 2. Красная полоска внизу
                            if (isHoliday || isWeekend) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(HolidayRed)
                                )
                            }

                            // 3. Красная точка
                            if (isHoliday) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 2.dp)
                                        .size(4.dp)
                                        .background(HolidayRed, CircleShape)
                                )
                            }

                            // 4. Звездочка
                            if (isShortened) {
                                Text(
                                    text = "*",
                                    fontSize = 8.sp,
                                    color = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57C00),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 2.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}