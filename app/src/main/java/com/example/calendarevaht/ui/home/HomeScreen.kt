@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.calendarevaht.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.calendarevaht.ui.components.DayCell
import com.example.calendarevaht.ui.groupHolidaysForDisplay
import com.example.calendarevaht.ui.shifts.ShiftsViewModel
import com.example.calendarevaht.ui.theme.ElevatedCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    onOpenSummaryCalendar: () -> Unit,
    vm: ShiftsViewModel = viewModel(),
    hvm: HomeViewModel = viewModel()
) {
    val templates by vm.templates.collectAsState()
    val holidaysByYear by hvm.holidays.collectAsState()
    val weather by hvm.weather.collectAsState()
    val today = remember { LocalDate.now() }
    val isDarkTheme = isSystemInDarkTheme()

    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val settings by settingsPrefs.settings.collectAsState(initial = AppSettings())

    val startMonth = remember { YearMonth.now().minusMonths(120) }
    val monthsCount = 240
    val initialPage = remember { 120 }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { monthsCount }
    )
    val scope = rememberCoroutineScope()

    val currentMonth = startMonth.plusMonths(pagerState.currentPage.toLong())

    LaunchedEffect(today.year) { hvm.ensureHolidaysLoaded(today.year) }
    LaunchedEffect(currentMonth.year) { hvm.ensureHolidaysLoaded(currentMonth.year) }

    val status = remember(templates, today) { computeStatus(templates, today) }

    val holidaysThisYear: Map<LocalDate, String> = holidaysByYear[currentMonth.year].orEmpty()

    val events = remember(templates, currentMonth, today, holidaysThisYear) {
        val monthStart = currentMonth.atDay(1)
        val monthEnd = currentMonth.atEndOfMonth()

        upcomingEvents(templates, today.minusDays(365), today, holidaysThisYear, 100)
            .filter { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            .take(6)
    }

    val holidaysThisMonth = remember(holidaysThisYear, currentMonth) {
        holidaysThisYear
            .filterKeys { YearMonth.from(it) == currentMonth }
            .toSortedMap()
    }

    val mineTemplate = templates.firstOrNull { it.isMine }
    val otherTemplate = templates.firstOrNull { !it.isMine }
    val mineColor = mineTemplate?.baseColor
    val otherColor = otherTemplate?.baseColor
    val mineLabel = mineTemplate?.brigadeName?.ifBlank { "Мой состав" } ?: "Мой состав"
    val otherLabel = otherTemplate?.brigadeName?.ifBlank { "Сменщик" } ?: "Сменщик"

    val overridesHash = remember(templates) {
        templates.fold(0) { acc, t -> 31 * acc + t.overrides.hashCode() }
    }
    val templatesHash = remember(templates) { templates.hashCode() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- 1. Карточка места и погоды ---
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Место вахты", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (settings.shiftPlace.isNotBlank()) settings.shiftPlace else "не задано",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Погода", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { hvm.refreshWeather() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить погоду", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (weather != null) {
                        Text(
                            text = "${weather!!.icon} ${weather!!.temperature}°",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = weather!!.description,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("--°", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 2. Карточка статуса ---
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(status.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (status.progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(status.progress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                if (status.subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(status.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 3. Заголовок месяца ---
        val monthNames = listOf(
            "ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ",
            "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"
        )
        val monthTitle = monthNames[currentMonth.monthValue - 1] + " " + currentMonth.year

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
            }
            Text(
                text = monthTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenSummaryCalendar() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 4. Календарь с 3D-анимацией ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(340.dp),
            key = { page -> page }
        ) { page ->
            val month = startMonth.plusMonths(page.toLong())
            val overridesHash = remember(templates) {
                templates.fold(0) { acc, t -> acc * 31 + t.overrides.hashCode() }
            }

            // Вычисляем смещение страницы: от -1 (слева) до +1 (справа)
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = pageOffset.absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 1. Сильная перспектива для эффекта объема (камера приближена)
                        cameraDistance = 14f * density

                        // 2. Поворот по оси Y (книжный переворот)
                        rotationY = pageOffset * 25f

                        // 3. Точка опоры (ось вращения).
                        // Если листаем влево (pageOffset > 0), опора справа (1f).
                        // Если листаем вправо (pageOffset < 0), опора слева (0f).
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (pageOffset > 0) 1f else 0f,
                            pivotFractionY = 0.5f
                        )

                        // 4. Тень от отрывающегося листа (только для светлой темы)
                        if (!isDarkTheme) {
                            shadowElevation = 20.dp.toPx() * absOffset
                        }

                        // 5. Прозрачность для противоположной страницы
                        alpha = 1f - (absOffset * 0.4f)

                        // 6. Легкое уменьшение уходящей страницы
                        scaleX = 1f - (absOffset * 0.05f)
                        scaleY = 1f - (absOffset * 0.05f)
                    }
            ) {
                // Оборачиваем в Box с непрозрачным фоном,
                // чтобы сквозь поворачивающийся лист не просвечивал следующий месяц
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MonthGrid(
                        month = month,
                        today = today,
                        templates = templates,
                        holidays = holidaysByYear[month.year].orEmpty(),
                        overridesHash = overridesHash,
                        templatesHash = templatesHash
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 5. Легенда ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mineColor != null) LegendItem(Color(mineColor), mineLabel)
            if (otherColor != null) LegendItem(Color(otherColor), otherLabel)
            LegendItem(Color(0xFFE53935), "праздник")
            LegendItem(Color(0xFFEF9A9A), "выходной")
        }

        // --- 6. Праздники месяца ---
        if (holidaysThisMonth.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Праздники месяца", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val grouped = groupHolidaysForDisplay(holidaysThisMonth)
                    grouped.forEach { text ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(text, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 7. Ближайшие события (ВОЗВРАЩЕНЫ ВНИЗ) ---
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Ближайшие события", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (events.isEmpty()) {
                    Text("В этом месяце событий нет", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    events.forEachIndexed { index, e ->
                        if (index > 0) Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(Color(e.color))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${e.label} · ${e.date.format(fmt)}",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
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

private data class TodayStatus(
    val title: String,
    val subtitle: String = "",
    val progress: Float = 0f
)

private data class Event(
    val date: LocalDate,
    val label: String,
    val color: Long,
    val isHoliday: Boolean = false
)

private fun computeStatus(
    templates: List<CycleTemplate>,
    today: LocalDate
): TodayStatus {
    val mine = templates.firstOrNull { it.isMine }
        ?: return TodayStatus("Мой состав не выбран", "Загляните в «Вахты» и отметьте свой состав")

    if (mine.isOneTime) {
        val end = mine.endDate ?: return TodayStatus("Разовая вахта без конца")
        return when {
            today.isBefore(mine.startDate) -> {
                val days = ChronoUnit.DAYS.between(today, mine.startDate).toInt()
                TodayStatus(
                    title = "До вахты $days дн.",
                    subtitle = "Начало ${mine.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
                )
            }
            today.isAfter(end) -> TodayStatus("Разовая вахта завершена")
            else -> {
                val total = ChronoUnit.DAYS.between(mine.startDate, end).toInt() + 1
                val day = ChronoUnit.DAYS.between(mine.startDate, today).toInt() + 1
                TodayStatus(
                    title = "Идёт вахта: $day/$total",
                    subtitle = "Осталось ${total - day} дн.",
                    progress = day.toFloat() / total
                )
            }
        }
    }

    val shifts = CycleCalculator.expandCycleToDays(mine, today.minusDays(60), today.plusDays(60))

    val currentShift = shifts.firstOrNull { !today.isBefore(it.start) && !today.isAfter(it.end) }

    if (currentShift != null) {
        val total = ChronoUnit.DAYS.between(currentShift.start, currentShift.end).toInt() + 1
        val day = ChronoUnit.DAYS.between(currentShift.start, today).toInt() + 1
        return TodayStatus(
            title = "Идёт вахта: $day/$total",
            subtitle = "Осталось ${total - day} дн.",
            progress = day.toFloat() / total
        )
    }

    val nextShift = shifts.firstOrNull { it.start.isAfter(today) }
    if (nextShift != null) {
        val daysToShift = ChronoUnit.DAYS.between(today, nextShift.start).toInt()
        val prevShift = shifts.lastOrNull { it.end.isBefore(today) }
        if (prevShift != null) {
            val totalRest = ChronoUnit.DAYS.between(prevShift.end, nextShift.start).toInt() - 1
            val currentRest = ChronoUnit.DAYS.between(prevShift.end, today).toInt()
            return TodayStatus(
                title = "Отдых: $currentRest/$totalRest",
                subtitle = "До вахты $daysToShift дн.",
                progress = if (totalRest > 0) currentRest.toFloat() / totalRest else 0f
            )
        } else {
            return TodayStatus(
                title = "До вахты $daysToShift дн.",
                subtitle = "Начало ${nextShift.start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
            )
        }
    }

    return TodayStatus("Нет данных о вахте")
}

private fun upcomingEvents(
    templates: List<CycleTemplate>,
    historyStart: LocalDate,
    today: LocalDate,
    holidays: Map<LocalDate, String>,
    limit: Int
): List<Event> {
    val horizon = today.plusDays(90)
    val result = mutableListOf<Event>()

    templates.forEach { t ->
        val shiftName = t.brigadeName.ifBlank { if (t.isMine) "Мой состав" else "Сменщик" }
        val entries = CycleCalculator.expandCycleToDays(t, historyStart, horizon)
        val sorted = entries.sortedBy { it.start }

        sorted.forEachIndexed { index, s ->
            if (!s.start.isBefore(today)) {
                result.add(Event(s.start, "Начало вахты ($shiftName)", t.baseColor))
            }
            val isLastPiece = (index == sorted.lastIndex) || sorted[index + 1].start.isAfter(s.end.plusDays(1))
            if (isLastPiece && !s.end.isBefore(today)) {
                result.add(Event(s.end, "Конец вахты ($shiftName)", t.baseColor))
            }
        }
    }

    return result.sortedBy { it.date }.distinctBy { it.date to it.label }.take(limit)
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    templates: List<CycleTemplate>,
    holidays: Map<LocalDate, String>,
    overridesHash: Int,
    templatesHash: Int
) {
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val monthStart = month.atDay(1)
    val monthEnd = month.atDay(daysInMonth)

    val shiftsByDay: Map<LocalDate, List<ShiftEntry>> = remember(month, templatesHash, overridesHash) {
        if (templates.isEmpty()) return@remember emptyMap()

        val all: List<ShiftEntry> = templates.flatMap { t ->
            CycleCalculator.expandCycleToDays(t, monthStart.minusDays(5), monthEnd.plusDays(5))
        }

        val map = mutableMapOf<LocalDate, MutableList<ShiftEntry>>()
        var d = monthStart
        while (!d.isAfter(monthEnd)) {
            val shiftsForDay = all.filter { !d.isBefore(it.start) && !d.isAfter(it.end) }
            if (shiftsForDay.isNotEmpty()) {
                map[d] = shiftsForDay.toMutableList()
            }
            d = d.plusDays(1)
        }
        map
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { day ->
                Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isHoliday = date in holidays,
                            isWeekend = date.dayOfWeek.value >= 6,
                            shifts = shiftsByDay[date]?.toList() ?: emptyList(),
                            onDayClick = { }
                        )
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}