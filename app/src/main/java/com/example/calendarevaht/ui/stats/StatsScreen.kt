package com.example.calendarevaht.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.ui.theme.ElevatedCard

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Статистика по составам", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip("Месяц", state.mode == StatsMode.MONTH) { vm.setMode(StatsMode.MONTH) }
            ModeChip("Год", state.mode == StatsMode.YEAR) { vm.setMode(StatsMode.YEAR) }
        }

        Spacer(Modifier.height(8.dp))

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
            Text(
                text = if (state.mode == StatsMode.MONTH)
                    "${monthName(state.month.monthValue)} ${state.month.year}"
                else
                    "Год ${state.year}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                if (state.mode == StatsMode.MONTH) vm.changeMonth(1) else vm.changeYear(1)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Вперёд")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompositionCard(
                modifier = Modifier.weight(1f),
                stats = state.mine,
                fallbackTitle = "Мой состав"
            )
            CompositionCard(
                modifier = Modifier.weight(1f),
                stats = state.other,
                fallbackTitle = "Сменщик"
            )
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Отработано часов по месяцам (${state.year})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(state.mine?.color ?: 0xFF4CAF50, state.mine?.title ?: "Мой состав")
                    Spacer(Modifier.width(12.dp))
                    LegendDot(state.other?.color ?: 0xFF2196F3, state.other?.title ?: "Сменщик")
                }

                Spacer(Modifier.height(8.dp))

                MonthlyChart(
                    points = state.monthly,
                    mineColor = Color(state.mine?.color ?: 0xFF4CAF50),
                    otherColor = Color(state.other?.color ?: 0xFF2196F3),
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompositionCard(
    modifier: Modifier,
    stats: CompositionStats?,
    fallbackTitle: String
) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(Color(stats?.color ?: 0xFF9E9E9E), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stats?.title ?: fallbackTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            StatLine("Дней на вахте", stats?.workDays?.toString() ?: "—")
            StatLine("Отработано часов", stats?.hours?.let { "$it ч" } ?: "—")
            StatLine("Захвачено выходных", stats?.weekendsOnShift?.toString() ?: "—")
            StatLine("Захвачено праздников", stats?.holidaysOnShift?.toString() ?: "—")
            StatLine("Дней отдыха", stats?.restDays?.toString() ?: "—")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(colorLong: Long, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(Color(colorLong), CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun MonthlyChart(
    points: List<MonthPoint>,
    mineColor: Color,
    otherColor: Color,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val n = points.size.coerceAtLeast(1)
        val maxVal = maxOf(
            points.maxOfOrNull { it.mineHours } ?: 0,
            points.maxOfOrNull { it.otherHours } ?: 0,
            1
        )
        val paddingBottom = 18.dp.toPx()
        val usableHeight = size.height - paddingBottom
        val cellWidth = size.width / n
        val barWidth = cellWidth / 3.2f

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        points.forEachIndexed { index, p ->
            val centerX = index * cellWidth + cellWidth / 2f
            val leftBarX = centerX - barWidth - 1.dp.toPx()
            val rightBarX = centerX + 1.dp.toPx()

            drawBar(
                x = leftBarX,
                value = p.mineHours,
                maxVal = maxVal,
                usableHeight = usableHeight,
                barWidth = barWidth,
                color = mineColor
            )
            drawBar(
                x = rightBarX,
                value = p.otherHours,
                maxVal = maxVal,
                usableHeight = usableHeight,
                barWidth = barWidth,
                color = otherColor
            )

            drawContext.canvas.nativeCanvas.drawText(
                p.month.toString(),
                centerX,
                size.height - 4.dp.toPx(),
                paint
            )
        }
    }
}

private fun DrawScope.drawBar(
    x: Float,
    value: Int,
    maxVal: Int,
    usableHeight: Float,
    barWidth: Float,
    color: Color
) {
    val h = if (value <= 0) 0f else usableHeight * (value.toFloat() / maxVal)
    drawRect(
        color = if (value <= 0) color.copy(alpha = 0.15f) else color,
        topLeft = Offset(x, usableHeight - h),
        size = Size(barWidth, if (h <= 0f) 1f else h)
    )
}

private fun monthName(month: Int): String = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)[month - 1]