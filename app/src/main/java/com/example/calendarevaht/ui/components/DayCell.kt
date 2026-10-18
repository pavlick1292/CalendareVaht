package com.example.calendarevaht.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

private val HolidayRed = Color(0xFFE53935)
private val DarkRedForText = Color(0xFFB71C1C)

@Composable
fun DayCell(
    date: LocalDate,
    shifts: List<ShiftEntry>,
    isHoliday: Boolean,
    isWeekend: Boolean,
    isToday: Boolean = false,
    onDayClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(6.dp)
    val todayBorder = MaterialTheme.colorScheme.tertiary
    val distinct = shifts.distinctBy { it.color }

    // Фон: если есть смены — цвет первой смены, иначе серый
    val baseColor = when {
        distinct.isNotEmpty() -> Color(distinct[0].color)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(baseColor)
            .then(if (isToday) Modifier.border(2.dp, todayBorder, shape) else Modifier)
            .clickable { onDayClick() },
        contentAlignment = Alignment.Center
    ) {
        // 2+ смены — делим пополам (верх/низ)
        if (distinct.size >= 2) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(distinct[0].color)))
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(distinct[1].color)))
            }
        }

        // КРАСНАЯ ПОЛОСКА ВНИЗУ (Для праздников И выходных)
        if (isHoliday || isWeekend) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp) // Чуть толще, чем в году, так как ячейка больше
                    .background(HolidayRed)
            )
        }

        // КРАСНАЯ ТОЧКА В УГЛУ (ТОЛЬКО для праздников)
        if (isHoliday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(8.dp)
                    .background(HolidayRed, CircleShape)
            )
        }

        // Цвет текста и обводка
        val textColor: Color
        val shadow: Shadow?

        if (distinct.isNotEmpty()) {
            // Если есть вахта — текст белый с обводкой
            textColor = Color.White
            shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
        } else {
            // Если вахты нет — для выходных/праздников темно-красный, иначе обычный
            textColor = if (isHoliday || isWeekend) DarkRedForText else MaterialTheme.colorScheme.onSurfaceVariant
            shadow = null
        }

        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 18.sp,
            fontWeight = if (isToday || isHoliday || isWeekend) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            style = TextStyle(shadow = shadow)
        )
    }
}