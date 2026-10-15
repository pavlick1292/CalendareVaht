package com.example.calendarevaht.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarevaht.domain.ShiftEntry
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    shifts: List<ShiftEntry> = emptyList(),
    onDayClick: (LocalDate) -> Unit
) {
    val todayBorder = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(8.dp, CircleShape)
            .border(2.dp, todayBorder, CircleShape)
            .clickable { onDayClick(date) }
            .clip(CircleShape)
    ) {
        when {
            shifts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            shifts.size == 1 -> {
                val c = shifts[0].color
                val composeColor = Color(c)
                Box(
                    modifier = Modifier.fillMaxSize().background(composeColor),
                    contentAlignment = Alignment.Center
                ) {
                    val textColor = if (composeColor.red * 0.299 + composeColor.green * 0.587 + composeColor.blue * 0.114 > 186)
                        Color.Black else Color.White
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            else -> {
                val s1 = shifts[0]
                val s2 = shifts[1]
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(s1.color))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(s2.color))
                        )
                    }
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}