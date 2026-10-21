package com.example.calendarevaht.ui.calendar

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.export.exportCalendar
import com.example.calendarevaht.domain.DayType
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    vm: CalendarViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь года") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.changeYear(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Text(
                    text = state.year.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { vm.changeYear(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            Legend(
                mineColor = state.mine?.baseColor,
                otherColor = state.other?.baseColor
            )

            Spacer(Modifier.height(8.dp))

            for (row in 0 until 4) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 3) {
                        val monthIndex = row * 3 + col + 1
                        MiniMonth(
                            yearMonth = YearMonth.of(state.year, monthIndex),
                            days = state.days,
                            workDaysMap = state.workDaysMap, // Передаем новую карту
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }

            // Секция с праздниками (из нового workDaysMap)
            val holidays = state.workDaysMap.filter { it.value.type == DayType.PUBLIC_HOLIDAY }
            if (holidays.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Праздники ${state.year}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))

                        // Группируем по названию, чтобы не выводить каждый день отдельно
                        val groupedHolidays = holidays.values
                            .filter { it.holidayName != null }
                            .groupBy { it.holidayName!! }

                        groupedHolidays.forEach { (name, days) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "$name (${days.size} дн.)",
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспорт в PDF")
            }
        }
    }

    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { isLandscape, withStats ->
                showExportDialog = false

                // Конвертируем workDaysMap в Map<LocalDate, String?> для PDF
                val holidayNames = state.workDaysMap
                    .filter { it.value.type == DayType.PUBLIC_HOLIDAY }
                    .mapValues { it.value.holidayName ?: "Праздник" }

                exportCalendar(
                    context = context,
                    year = state.year,
                    templates = state.templates,
                    persons = state.persons,
                    holidays = holidayNames, // Передаем названия праздников
                    shiftHours = state.shiftHours,
                    isLandscape = isLandscape,
                    withStats = withStats
                )
            }
        )
    }
}

@Composable
private fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (Boolean, Boolean) -> Unit
) {
    var isLandscape by remember { mutableStateOf(true) }
    var withStats by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры экспорта") },
        text = {
            Column {
                Text("Ориентация листа:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isLandscape, onClick = { isLandscape = true })
                    Text("Альбомная (для печати)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isLandscape, onClick = { isLandscape = false })
                    Text("Книжная (для телефона)")
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withStats, onCheckedChange = { withStats = it })
                    Text("Добавить страницу со статистикой")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(isLandscape, withStats) }) {
                Text("Сформировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun Legend(mineColor: Long?, otherColor: Long?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mineColor != null) LegendItem(Color(mineColor), "мой состав")
        if (otherColor != null) LegendItem(Color(otherColor), "сменщик")
        LegendItem(Color(0xFFE53935), "выходной / праздник")
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