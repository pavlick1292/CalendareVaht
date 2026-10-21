package com.example.calendarevaht.ui.summary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.domain.MonthNorm
import com.example.calendarevaht.ui.stats.StatsMode
import com.example.calendarevaht.ui.stats.StatsViewModel
import com.example.calendarevaht.ui.theme.ElevatedCard
import java.time.YearMonth
import androidx.core.graphics.toColorInt

// Расширяем enum для режима
enum class SummaryTab { MONTH, YEAR, NORMS }

@Composable
fun SummaryScreen(
    vm: StatsViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    var selectedTab by remember { mutableStateOf(SummaryTab.MONTH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Сводка", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        // Переключатель Месяц / Год / Нормы
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = selectedTab == SummaryTab.MONTH,
                onClick = { selectedTab = SummaryTab.MONTH; vm.setMode(StatsMode.MONTH) },
                label = { Text("Месяц") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selectedTab == SummaryTab.YEAR,
                onClick = { selectedTab = SummaryTab.YEAR; vm.setMode(StatsMode.YEAR) },
                label = { Text("Год") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selectedTab == SummaryTab.NORMS,
                onClick = { selectedTab = SummaryTab.NORMS },
                label = { Text("Нормы") }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (selectedTab == SummaryTab.NORMS) {
            NormsTable(
                norms = state.monthlyNorms,
                year = state.year,
                myMonthlyStats = state.monthly // Передаем данные из StatsViewModel
            )
        } else {
            // Навигация по времени (только для Месяца/Года)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (state.mode == StatsMode.MONTH) vm.changeMonth(-1) else vm.changeYear(-1)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Назад")
                }
                val title = if (state.mode == StatsMode.MONTH) {
                    monthTitle(state.month)
                } else {
                    "${state.year} год"
                }
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (state.mode == StatsMode.MONTH) vm.changeMonth(1) else vm.changeYear(1)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Вперед")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                state.mine?.let { mine ->
                    StatsCard(stats = mine)
                    Spacer(Modifier.height(12.dp))
                }

                state.other?.let { other ->
                    StatsCard(stats = other)
                    Spacer(Modifier.height(12.dp))
                }

                if (state.mode == StatsMode.YEAR && state.monthly.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Отработано часов по месяцам", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            HoursChart(
                                monthly = state.monthly,
                                norms = state.monthlyNorms, // <-- ВОТ ЭТА СТРОКА
                                mineColor = state.mine?.color,
                                otherColor = state.other?.color
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
private fun NormsTable(
    norms: List<MonthNorm>,
    year: Int,
    myMonthlyStats: List<com.example.calendarevaht.ui.stats.MonthPoint> // Передаем статистику по месяцам
) {
    if (norms.isEmpty()) {
        Text("Данные о нормах загружаются...", modifier = Modifier.padding(16.dp))
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Text("Нормы рабочего времени ($year)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            // Заголовок таблицы
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Мес.", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.3f))
                Text("Кал.", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                Text("Раб.", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                Text("Вых.", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                Text("Норма", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                Text("Мои", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                Text("Откл.", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
            }
            HorizontalDivider()

            var totalNorm = 0
            var totalMine = 0

            norms.forEach { norm ->
                val myHours = myMonthlyStats.firstOrNull { it.month == norm.month }?.mineHours ?: 0
                val diff = myHours - norm.normHours40h
                totalNorm += norm.normHours40h
                totalMine += myHours

                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(monthNames[norm.month - 1].take(3), fontSize = 10.sp, modifier = Modifier.weight(1.3f))
                    Text("${norm.calendarDays}", fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                    Text("${norm.workingDays}", fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                    Text("${norm.weekendDays}", fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                    Text("${norm.normHours40h}", fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.9f))
                    Text("$myHours", fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                    Text(
                        text = if (diff > 0) "+$diff" else if (diff < 0) "$diff" else "0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diff > 0) Color(0xFF4CAF50) else if (diff < 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                HorizontalDivider()
            }

            // Итоговая строка за год
            val totalDiff = totalMine - totalNorm
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$year", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.3f))
                Text("-", fontSize = 10.sp, modifier = Modifier.weight(0.7f)) // Кал
                Text("-", fontSize = 10.sp, modifier = Modifier.weight(0.7f)) // Раб
                Text("-", fontSize = 10.sp, modifier = Modifier.weight(0.7f)) // Вых
                Text("$totalNorm", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                Text("$totalMine", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                Text(
                    text = if (totalDiff > 0) "+$totalDiff" else "$totalDiff",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (totalDiff > 0) Color(0xFF4CAF50) else if (totalDiff < 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

private val monthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

@Composable
private fun StatsCard(stats: com.example.calendarevaht.ui.stats.CompositionStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(Color(stats.color), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(stats.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            StatRow("Дней на вахте", "${stats.workDays}")
            StatRow("Отработано часов", "${stats.hours} ч")
            StatRow("Выходных на вахте", "${stats.weekendsOnShift}")
            StatRow("Праздников на вахте", "${stats.holidaysOnShift}")
            StatRow("Дней отдыха", "${stats.restDays}")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HoursChart(
    monthly: List<com.example.calendarevaht.ui.stats.MonthPoint>,
    norms: List<MonthNorm>,
    mineColor: Long?,
    otherColor: Long?
) {
    val isDarkTheme = isSystemInDarkTheme()

    val maxVal = monthly.maxOfOrNull { maxOf(it.mineHours, it.otherHours) }?.coerceAtLeast(1) ?: 1
    val maxNorm = norms.maxOfOrNull { it.normHours40h }?.coerceAtLeast(1) ?: 1
    val finalMaxVal = maxOf(maxVal, maxNorm).toFloat()

    Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        val colWidth = size.width / 12f
        val barWidth = colWidth / 3f
        // Оставляем отступ снизу для подписей месяцев и отклонений
        val chartHeight = size.height - 60f

        monthly.forEachIndexed { index, point ->
            val x = index * colWidth
            val monthNum = index + 1

            // --- 1. Рисуем столбики ---
            if (mineColor != null) {
                val h = (point.mineHours.toFloat() / finalMaxVal) * chartHeight
                drawRect(
                    color = Color(mineColor),
                    topLeft = Offset(x + barWidth / 2, chartHeight - h),
                    size = Size(barWidth, h)
                )
            }

            if (otherColor != null) {
                val h = (point.otherHours.toFloat() / finalMaxVal) * chartHeight
                drawRect(
                    color = Color(otherColor),
                    topLeft = Offset(x + barWidth * 1.5f, chartHeight - h),
                    size = Size(barWidth, h)
                )
            }

            // --- 2. Рисуем линию нормы, её значение и отклонение ---
            val norm = norms.firstOrNull { it.month == monthNum }

            if (norm != null && norm.normHours40h > 0) {
                val normY = chartHeight - (norm.normHours40h.toFloat() / finalMaxVal) * chartHeight

                // Красная пунктирная линия
                drawLine(
                    color = Color.Red.copy(alpha = 0.5f),
                    start = Offset(x + 2f, normY),
                    end = Offset(x + colWidth - 2f, normY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Цвет обводки зависит от темы
                val outlineColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                val textColor = android.graphics.Color.parseColor("#E53935") // Красный

                val textPaint = android.graphics.Paint().apply {
                    textSize = 22f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Обводка для числа нормы
                textPaint.color = outlineColor
                textPaint.style = android.graphics.Paint.Style.STROKE
                textPaint.strokeWidth = 5f
                textPaint.strokeJoin = android.graphics.Paint.Join.ROUND
                drawContext.canvas.nativeCanvas.drawText(
                    "${norm.normHours40h}",
                    x + colWidth / 2f,
                    normY - 8f,
                    textPaint
                )

                // Сам текст числа нормы
                textPaint.color = textColor
                textPaint.style = android.graphics.Paint.Style.FILL
                drawContext.canvas.nativeCanvas.drawText(
                    "${norm.normHours40h}",
                    x + colWidth / 2f,
                    normY - 8f,
                    textPaint
                )

                // Подпись отклонения внизу
                val diff = point.mineHours - norm.normHours40h
                if (diff != 0) {
                    val diffText = if (diff > 0) "+$diff" else "$diff"
                    val diffColor = if (diff > 0) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336")

                    drawContext.canvas.nativeCanvas.drawText(
                        diffText,
                        x + colWidth / 2,
                        chartHeight + 50f,
                        android.graphics.Paint().apply {
                            color = diffColor
                            textSize = 24f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // --- 3. Подписи месяцев ---
            val mn = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
            drawContext.canvas.nativeCanvas.drawText(
                mn[index],
                x + colWidth / 2,
                chartHeight + 25f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

private fun monthTitle(month: YearMonth): String {
    val names = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    return "${names[month.monthValue - 1]} ${month.year}"
}