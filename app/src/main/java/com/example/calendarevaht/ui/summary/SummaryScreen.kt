package com.example.calendarevaht.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.data.AppSettings
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.ui.shifts.ShiftsViewModel
import com.example.calendarevaht.ui.theme.ElevatedCard
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SummaryScreen(
    vm: ShiftsViewModel = viewModel()
) {
    val templates by vm.templates.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }

    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val settings by settingsPrefs.settings.collectAsState(initial = AppSettings())

    val mine = templates.firstOrNull { it.isMine }
    val other = templates.firstOrNull { !it.isMine }

    val monthStart = month.atDay(1)
    val monthEnd = month.atDay(month.lengthOfMonth())

    val mineEntries: List<ShiftEntry> = remember(mine, month) {
        if (mine == null) emptyList()
        else CycleCalculator.expandCycleToDays(mine, monthStart, monthEnd)
    }
    val mineWorkDays: Set<LocalDate> = remember(mineEntries) { expandToDays(mineEntries) }

    val totalWorkDays = mineWorkDays.size
    val totalHours = totalWorkDays * settings.shiftHours
    val weekendOnShift = mineWorkDays.count { it.dayOfWeek.value >= 6 }
    val restDays = month.lengthOfMonth() - totalWorkDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Сводка", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
            }
            Text(monthTitle(month), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (mine == null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Мой состав не выбран. Выберите состав во вкладке «Вахты».",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
        } else {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Отчёт за ${monthTitle(month)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    StatRow("Дней на вахте", "$totalWorkDays")
                    StatRow("Отработано часов", "$totalHours ч")
                    StatRow("Выходных на вахте", "$weekendOnShift")
                    StatRow("Дней отдыха", "$restDays")
                }
            }

            Spacer(Modifier.height(12.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Сводный календарь", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    val mineColor = Color(mine.baseColor)
                    val otherColor = other?.let { Color(it.baseColor) }
                    val mineLabel = mine.brigadeName.ifBlank { "Мой состав" }
                    val otherLabel = other?.brigadeName?.ifBlank { "Сменщик" } ?: "Сменщик"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendItem(mineColor, mineLabel)
                        if (otherColor != null) {
                            Spacer(Modifier.width(12.dp))
                            LegendItem(otherColor, otherLabel)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    SummaryCalendar(
                        month = month,
                        mineColor = mineColor,
                        otherColor = otherColor,
                        mine = mine,
                        other = other
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun SummaryCalendar(
    month: YearMonth,
    mineColor: Color,
    otherColor: Color?,
    mine: CycleTemplate,
    other: CycleTemplate?
) {
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val monthStart = month.atDay(1)
    val monthEnd = month.atDay(daysInMonth)
    val today = LocalDate.now()

    val mineDays: Set<LocalDate> = remember(mine, month) {
        expandToDays(CycleCalculator.expandCycleToDays(mine, monthStart, monthEnd))
    }
    val otherDays: Set<LocalDate> = remember(other, month) {
        if (other == null) emptySet()
        else expandToDays(CycleCalculator.expandCycleToDays(other, monthStart, monthEnd))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { d ->
                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Text(d, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val dayNum = idx - firstDayOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        val isMine = date in mineDays
                        val isOther = date in otherDays

                        val bg: Color? = when {
                            isMine && isOther && otherColor != null -> null
                            isMine -> mineColor
                            isOther && otherColor != null -> otherColor
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg ?: MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bg == null && isMine && isOther && otherColor != null) {
                                Column(Modifier.fillMaxSize()) {
                                    Box(Modifier.weight(1f).fillMaxWidth().background(mineColor))
                                    Box(Modifier.weight(1f).fillMaxWidth().background(otherColor))
                                }
                            }
                            Text(
                                text = dayNum.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMine || isOther) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

private fun expandToDays(entries: List<ShiftEntry>): Set<LocalDate> {
    val set = mutableSetOf<LocalDate>()
    entries.forEach { s ->
        var d = s.start
        while (!d.isAfter(s.end)) {
            set.add(d)
            d = d.plusDays(1)
        }
    }
    return set
}

private fun monthTitle(month: YearMonth): String {
    val names = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    return "${names[month.monthValue - 1]} ${month.year}"
}