package com.example.calendarevaht.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarevaht.domain.ShiftEntry
import java.time.LocalDate
import java.time.YearMonth

private val HolidayRed = Color(0xFFE53935)
private val DarkRedForText = Color(0xFFB71C1C)

private val CellSize = 18.dp

@Composable
fun MiniMonth(
    yearMonth: YearMonth,
    days: Map<LocalDate, List<ShiftEntry>>,
    holidays: Map<LocalDate, String>,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = getMonthName(yearMonth.monthValue),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNumber)
                            MiniDayCell(
                                date = date,
                                shifts = days[date] ?: emptyList(),
                                isHoliday = date in holidays,
                                isWeekend = date.dayOfWeek.value >= 6,
                                isToday = date == today
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniDayCell(
    date: LocalDate,
    shifts: List<ShiftEntry>,
    isHoliday: Boolean,
    isWeekend: Boolean,
    isToday: Boolean = false
) {
    val shape = RoundedCornerShape(3.dp)
    val todayBorder = MaterialTheme.colorScheme.tertiary
    val distinct = shifts.distinctBy { it.color }

    // Фон: если есть вахта — её цвет, иначе серый
    val baseColor = if (distinct.isNotEmpty()) {
        Color(distinct[0].color)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(CellSize)
            .padding(0.5.dp)
            .clip(shape)
            .background(baseColor)
            .then(if (isToday) Modifier.border(1.dp, todayBorder, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1. Две смены — делим пополам
        if (distinct.size >= 2) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(distinct[0].color)))
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(distinct[1].color)))
            }
        }

        // 2. КРАСНАЯ ПОЛОСКА ВНИЗУ (Для праздников И выходных, всегда яркая)
        if (isHoliday || isWeekend) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp) // Сделал чуть тоньше, чтобы не перекрывала цифры
                    .background(HolidayRed) // ОДИН ЯРКО-КРАСНЫЙ ЦВЕТ ДЛЯ ВСЕХ
            )
        }

        // 3. КРАСНАЯ ТОЧКА В УГЛУ (ТОЛЬКО для праздников!)
        if (isHoliday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(3.dp)
                    .background(HolidayRed, CircleShape)
            )
        }

        // 4. Текст дня
        val textColor: Color
        val shadow: Shadow?

        if (distinct.isNotEmpty()) {
            // Если есть вахта — текст всегда белый с черной обводкой
            textColor = Color.White
            shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
        } else {
            // Если вахты нет, но выходной/праздник — темно-красный текст
            textColor = if (isHoliday || isWeekend) DarkRedForText else MaterialTheme.colorScheme.onSurfaceVariant
            shadow = null
        }

        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 9.sp,
            fontWeight = if (isToday || isHoliday || isWeekend) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            style = TextStyle(shadow = shadow)
        )
    }
}

private fun getMonthName(month: Int): String = when (month) {
    1 -> "Январь"; 2 -> "Февраль"; 3 -> "Март"; 4 -> "Апрель"
    5 -> "Май"; 6 -> "Июнь"; 7 -> "Июль"; 8 -> "Август"
    9 -> "Сентябрь"; 10 -> "Октябрь"; 11 -> "Ноябрь"; 12 -> "Декабрь"
    else -> ""
}