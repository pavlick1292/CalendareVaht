package com.example.calendarevaht.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarevaht.domain.DayType
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.domain.WorkDayInfo
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MiniMonth(
    yearMonth: YearMonth,
    days: Map<LocalDate, List<ShiftEntry>>,
    workDaysMap: Map<LocalDate, WorkDayInfo>,
    modifier: Modifier = Modifier
) {
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Сетка дней (6 строк по 7 дней)
        val firstDay = yearMonth.atDay(1)
        val leadingEmpty = (firstDay.dayOfWeek.value + 6) % 7
        val totalDays = yearMonth.lengthOfMonth()

        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - leadingEmpty + 1
                    if (dayIndex in 1..totalDays) {
                        val date = yearMonth.atDay(dayIndex)
                        val shifts = days[date].orEmpty()
                        val info = workDaysMap[date]

                        val isHoliday = info?.type == DayType.PUBLIC_HOLIDAY
                        val isWeekend = info?.type == DayType.WEEKEND || info?.type == DayType.TRANSFERRED_DAY_OFF
                        val isShortened = info?.isShortened == true

                        val bgColor = when {
                            shifts.isNotEmpty() -> Color(shifts.first().color)
                            isHoliday -> Color(0xFFFFCDD2) // Светло-красный для праздников
                            isShortened -> Color(0xFFFFF9C4) // Светло-желтый для сокращенных
                            else -> Color.Transparent
                        }

                        val textColor = if (shifts.isNotEmpty()) Color.White else if (isHoliday || isWeekend) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .background(bgColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayIndex.toString(),
                                fontSize = 9.sp,
                                color = textColor,
                                fontWeight = if (isShortened) FontWeight.Bold else FontWeight.Normal
                            )

                            // Звездочка для сокращенного дня (маленькая в углу)
                            if (isShortened) {
                                Text(
                                    text = "*",
                                    fontSize = 8.sp,
                                    color = Color(0xFFF57C00),
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