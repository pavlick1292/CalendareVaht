package com.example.calendarevaht

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.ui.components.DayCell
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.padding(16.dp)) {
                    CalendarScreen()
                }
            }
        }
    }
}

@Composable
fun CalendarScreen() {
    val currentMonth = YearMonth.now()
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDay = currentMonth.atDay(1).dayOfWeek
    val paddingCells = when (firstDay) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
        else -> 0
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${currentMonth.month} ${currentMonth.year}",
            style = MaterialTheme.typography.headlineMedium
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(paddingCells) {
                Spacer(modifier = Modifier.size(48.dp))
            }

            items(daysInMonth) { index ->
                val dayNumber = index + 1
                val date = currentMonth.atDay(dayNumber)
                val isToday = date == LocalDate.now()

                val template = CycleTemplate(
                    id = 1,
                    rule = CycleRule(workDays = 28, restDays = 14),
                    startDate = YearMonth.of(2026, 1).atDay(1),
                    changeDate = YearMonth.of(2026, 7).atDay(15),
                    newRule = CycleRule(workDays = 30, restDays = 14),
                    baseColor = 0xFF4CAF50L
                )

                val shiftsForDay = CycleCalculator()
                    .expandCycleToDays(template, date, date)
                    .filter { it.start <= date && it.end >= date }

                DayCell(
                    date = date,
                    isToday = isToday,
                    shifts = shiftsForDay,
                    onDayClick = { clickedDate ->
                        println("Нажали на день: $clickedDate")
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        CalendarScreen()
    }
}