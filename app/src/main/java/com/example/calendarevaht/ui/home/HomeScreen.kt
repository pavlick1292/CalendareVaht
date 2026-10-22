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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.data.AppSettings
import com.example.calendarevaht.data.SettingsPreferences
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.DayType
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.domain.WorkDayInfo
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

    val workDays by hvm.workDaysMap.collectAsState()
    val holidaysThisYear = holidaysByYear[currentMonth.year].orEmpty()

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

    // Проверяем, есть ли сокращенные дни в текущем месяце
    val hasShortenedThisMonth = remember(workDays, currentMonth) {
        workDays.values.any {
            YearMonth.from(it.date) == currentMonth &&
                    (it.isShortened || it.type == DayType.SHORTENED_WORKING)
        }
    }

    val mineTemplate = templates.firstOrNull { it.isMine }
    val otherTemplate = templates.firstOrNull { !it.isMine }
    val mineColor = mineTemplate?.baseColor
    val otherColor = otherTemplate?.baseColor
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
            modifier = Modifier.fillMaxWidth().height(380.dp),
            key = { page -> page }
        ) { page ->
            val month = startMonth.plusMonths(page.toLong())
            val overridesHash = remember(templates) {
                templates.fold(0) { acc, t -> acc * 31 + t.overrides.hashCode() }
            }

            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = pageOffset.absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 14f * density
                        rotationY = pageOffset * 25f
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (pageOffset > 0) 1f else 0f,
                            pivotFractionY = 0.5f
                        )
                        if (!isDarkTheme) {
                            shadowElevation = 20.dp.toPx() * absOffset
                        }
                        alpha = 1f - (absOffset * 0.4f)
                        scaleX = 1f - (absOffset * 0.05f)
                        scaleY = 1f - (absOffset * 0.05f)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MonthGrid(
                        month = month,
                        today = today,
                        templates = templates,
                        workDaysMap = workDays,
                        overridesHash = overridesHash,
                        templatesHash = templatesHash
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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

        // --- 6.1. Сокращенные дни месяца ---
        val shortenedThisMonth = remember(workDays, currentMonth) {
            workDays.values
                .filter {
                    YearMonth.from(it.date) == currentMonth &&
                            (it.isShortened || it.type == DayType.SHORTENED_WORKING)
                }
                .sortedBy { it.date }
        }

        if (shortenedThisMonth.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Сокращенные дни", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    shortenedThisMonth.forEach { info ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Color(0xFFFFF59D), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            val dateStr = info.date.format(DateTimeFormatter.ofPattern("dd.MM"))
                            Text(dateStr, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }


        // --- 7. Ближайшие события ---
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
                    subtitle = "Осталось ${total - day + 1} дн.",
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
        val entries = CycleCalculator.expandCycleToDays(t, historyStart, horizon)
        val sorted = entries.sortedBy { it.start }

        sorted.forEachIndexed { index, s ->
            if (!s.start.isBefore(today)) {
                result.add(Event(s.start, "Начало вахты", t.baseColor))
            }
            val isLastPiece = (index == sorted.lastIndex) || sorted[index + 1].start.isAfter(s.end.plusDays(1))
            if (isLastPiece && !s.end.isBefore(today)) {
                result.add(Event(s.end, "Конец вахты", t.baseColor))
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
    workDaysMap: Map<LocalDate, WorkDayInfo>,
    overridesHash: Int,
    templatesHash: Int
) {
    val shiftsByDay = remember(month, templatesHash, overridesHash) {
        val days = mutableMapOf<LocalDate, MutableList<ShiftEntry>>()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        templates.forEach { t ->
            CycleCalculator.expandCycleToDays(t, monthStart.minusDays(10), monthEnd.plusDays(10))
                .forEach { s ->
                    var d = s.start
                    while (!d.isAfter(s.end)) {
                        if (!d.isBefore(monthStart) && !d.isAfter(monthEnd)) {
                            days.getOrPut(d) { mutableListOf() }.add(s)
                        }
                        d = d.plusDays(1)
                    }
                }
        }
        days
    }

    val firstDay = month.atDay(1)
    val firstDow = firstDay.dayOfWeek.value
    val daysInMonth = month.lengthOfMonth()
    val lead = (firstDow + 6) % 7

    Column(Modifier.fillMaxSize()) {
        val dowLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dowLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        var cell = 0
        val weeks = (lead + daysInMonth + 6) / 7
        repeat(weeks) { weekIndex ->
            if (weekIndex > 0) Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) {
                    val dayNumber = cell - lead + 1
                    cell++
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Spacer(Modifier.weight(1f).size(48.dp))
                        return@repeat
                    }
                    val date = month.atDay(dayNumber)
                    val info = workDaysMap[date]
                    val isHoliday = info?.type == DayType.PUBLIC_HOLIDAY
                    val isWeekend = info?.type == DayType.WEEKEND ||
                            info?.type == DayType.TRANSFERRED_DAY_OFF ||
                            date.dayOfWeek.value >= 6
                    val isShortened = info?.isShortened == true ||
                            info?.type == DayType.SHORTENED_WORKING

                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isHoliday = isHoliday,
                            isWeekend = isWeekend,
                            isShortened = isShortened,
                            shifts = shiftsByDay[date]?.toList() ?: emptyList(),
                            onDayClick = { }
                        )
                    }
                }
            }
        }
    }
}