@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.calendarevaht.ui.shifts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.domain.Brigade
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.Person
import com.example.calendarevaht.domain.ShiftEntry
import com.example.calendarevaht.domain.ShiftOverride
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val PaletteColors = listOf(
    0xFF4CAF50, 0xFF2196F3, 0xFFF44336, 0xFFFF9800,
    0xFF9C27B0, 0xFF00BCD4, 0xFFFFC107, 0xFF795548
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShiftsScreen(
    vm: ShiftsViewModel = viewModel()
) {
    val templates by vm.templates.collectAsState()
    val brigades by vm.brigades.collectAsState()
    val persons by vm.persons.collectAsState()

    val forecastMonths by vm.forecastMonths.collectAsState()
    var editing by remember { mutableStateOf<CycleTemplate?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showForecastMenu by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    var viewingPerson by remember { mutableStateOf<Person?>(null) }

    // Состояние для добавления нового сотрудника
    var creatingPerson by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вахты") },
                actions = {
                    Box {
                        IconButton(onClick = { showForecastMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Горизонт")
                        }
                        DropdownMenu(
                            expanded = showForecastMenu,
                            onDismissRequest = { showForecastMenu = false }
                        ) {
                            listOf(
                                3 to "3 месяца", 6 to "6 месяцев", 12 to "1 год",
                                24 to "2 года", 36 to "3 года"
                            ).forEach { (months, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (forecastMonths == months) "$label (выбрано)" else label) },
                                    onClick = {
                                        vm.setForecastMonths(months)
                                        showForecastMenu = false
                                    }
                                )
                            }
                        }
                    }
                    if (selectedTab == 0) {
                        IconButton(onClick = { creating = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить вахту")
                        }
                    } else {
                        IconButton(onClick = { creatingPerson = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить сотрудника")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Вахты") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Сотрудники") }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                },
                label = "TabsAnimation"
            ) { tabIndex ->
                if (tabIndex == 0) {
                    // Вкладка "Вахты"
                    if (templates.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Вахт пока нет", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { creating = true }) { Text("Добавить первую вахту") }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(templates, key = { it.id }) { t ->
                                TemplateRow(template = t, onClick = { editing = t })
                            }
                        }
                    }
                } else {
                    // Вкладка "Сотрудники"
                    if (persons.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Сотрудников пока нет", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { creatingPerson = true }) { Text("Добавить сотрудника") }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(persons, key = { it.id }) { person ->
                                val personBrigade = brigades.find { person.id in it.personIds }
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { viewingPerson = person },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                            Text(person.fullName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(person.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Должность: ${person.position.ifBlank { "не указана" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (personBrigade != null) {
                                                Text("Бригада: ${personBrigade.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            } else {
                                                Text("Без бригады", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалоги
    if (creating) {
        EditShiftDialog(
            initial = null,
            brigades = brigades,
            onDismiss = { creating = false },
            onSave = { t -> vm.add(t); creating = false },
            onDelete = null,
            onAddSpecificShiftChange = null
        )
    }

    editing?.let { template ->
        EditShiftDialog(
            initial = template,
            brigades = brigades,
            onDismiss = { editing = null },
            onSave = { t -> vm.update(t); editing = null },
            onDelete = { vm.remove(template.id); editing = null },
            onAddSpecificShiftChange = { originalStart, newStart, work, rest, shiftSubsequent ->
                val base = vm.templates.value.firstOrNull { it.id == template.id } ?: template
                val filteredOverrides = base.overrides.filterNot { it.originalStart == originalStart }
                val newOverride = ShiftOverride(originalStart, newStart, work, rest, shiftSubsequent)
                val updatedTemplate = base.copy(overrides = (filteredOverrides + newOverride).sortedBy { it.originalStart })
                vm.update(updatedTemplate)
                editing = updatedTemplate
            },
            onRemoveOverride = { originalStart ->
                val base = vm.templates.value.firstOrNull { it.id == template.id } ?: template
                val updatedTemplate = base.copy(overrides = base.overrides.filterNot { it.originalStart == originalStart })
                vm.update(updatedTemplate)
                editing = updatedTemplate
            }
        )
    }

    viewingPerson?.let { person ->
        EditPersonDialog(
            person = person,
            brigades = brigades,
            onDismiss = { viewingPerson = null },
            onSave = { updatedPerson, newBrigadeId ->
                vm.updatePerson(updatedPerson)
                vm.movePersonToBrigade(updatedPerson.id, newBrigadeId)
                viewingPerson = null
            }
        )
    }

    if (creatingPerson) {
        AddPersonDialog(
            onSave = { name, pos ->
                // По умолчанию добавляем в первую бригаду (если она есть)
                val defaultBrigadeId = brigades.firstOrNull()?.id ?: 1L
                vm.addPersonToBrigade(defaultBrigadeId, name, pos)
                creatingPerson = false
            },
            onCancel = { creatingPerson = false }
        )
    }
}

// === ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ===

@Composable
private fun EditPersonDialog(
    person: Person,
    brigades: List<Brigade>,
    onDismiss: () -> Unit,
    onSave: (Person, Long?) -> Unit
) {
    var name by remember { mutableStateOf(person.fullName) }
    var position by remember { mutableStateOf(person.position) }

    val currentBrigade = brigades.find { person.id in it.personIds }
    var selectedBrigadeId by remember { mutableStateOf(currentBrigade?.id) }
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать сотрудника") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ФИО") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Должность") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = brigades.find { it.id == selectedBrigadeId }?.name ?: "Не выбрана",
                        onValueChange = { },
                        label = { Text("Бригада") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedMenu = true })

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        brigades.forEach { brigade ->
                            DropdownMenuItem(
                                text = { Text(brigade.name) },
                                onClick = {
                                    selectedBrigadeId = brigade.id
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(person.copy(fullName = name, position = position), selectedBrigadeId)
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun AddPersonDialog(
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Новый сотрудник") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ФИО") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Должность") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, position) },
                enabled = name.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена") }
        }
    )
}

@Composable
private fun TemplateRow(
    template: CycleTemplate,
    onClick: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val displayName = template.brigadeName.ifBlank { "Без названия" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(template.baseColor)))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (template.isOneTime) {
                    Text("Разовая · ${template.startDate.format(fmt)} — ${template.endDate?.format(fmt) ?: "?"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Циклическая · с ${template.startDate.format(fmt)} · ${template.initialRule.workDays}/${template.initialRule.restDays}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (template.overrides.isNotEmpty()) {
                        Text("измененных вахт: ${template.overrides.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = date.format(fmt),
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ColorPicker(selected: Long, onSelect: (Long) -> Unit) {
    Text("Цвет", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaletteColors.forEach { c ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .then(if (c == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center
            ) {
                if (c == selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EditShiftDialog(
    initial: CycleTemplate?,
    brigades: List<Brigade>,
    onDismiss: () -> Unit,
    onSave: (CycleTemplate) -> Unit,
    onDelete: (() -> Unit)?,
    onAddSpecificShiftChange: ((LocalDate, LocalDate, Int, Int, Boolean) -> Unit)?,
    onRemoveOverride: ((LocalDate) -> Unit)? = null
) {
    val isEdit = initial != null
    var isOneTime by remember { mutableStateOf(initial?.isOneTime ?: false) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(initial?.endDate ?: LocalDate.now().plusDays(14)) }
    var workText by remember { mutableStateOf((initial?.initialRule?.workDays ?: 15).toString()) }
    var restText by remember { mutableStateOf((initial?.initialRule?.restDays ?: 15).toString()) }
    var isMine by remember { mutableStateOf(initial?.isMine ?: false) }

    var selectedBrigade by remember { mutableStateOf(brigades.firstOrNull { it.id == initial?.brigadeId } ?: brigades.firstOrNull()) }
    var expandedBrigadeMenu by remember { mutableStateOf(false) }

    var color by remember { mutableStateOf(initial?.baseColor ?: PaletteColors[0]) }
    var showShiftSelection by remember { mutableStateOf(false) }
    var selectedShiftToEdit by remember { mutableStateOf<ShiftEntry?>(null) }

    val currentOverrides: List<ShiftOverride> = initial?.overrides ?: emptyList()
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать вахту" else "Новая вахта") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (isEdit && !isOneTime && onAddSpecificShiftChange != null) {
                    Button(
                        onClick = { showShiftSelection = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Изменить конкретную вахту")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (isEdit && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить вахту", color = MaterialTheme.colorScheme.error)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedBrigade?.name ?: "Выберите бригаду",
                        onValueChange = { },
                        label = { Text("Бригада") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedBrigadeMenu = true })

                    DropdownMenu(
                        expanded = expandedBrigadeMenu,
                        onDismissRequest = { expandedBrigadeMenu = false }
                    ) {
                        brigades.forEach { brigade ->
                            DropdownMenuItem(
                                text = { Text(brigade.name) },
                                onClick = {
                                    selectedBrigade = brigade
                                    expandedBrigadeMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                ColorPicker(selected = color, onSelect = { color = it })
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isOneTime, onClick = { isOneTime = false }, label = { Text("Циклическая") })
                    FilterChip(selected = isOneTime, onClick = { isOneTime = true }, label = { Text("Разовая") })
                }
                Spacer(Modifier.height(8.dp))

                DateField(label = "Дата начала", date = startDate, onDateChange = { startDate = it })
                Spacer(Modifier.height(8.dp))

                if (isOneTime) {
                    DateField(label = "Дата конца", date = endDate, onDateChange = { endDate = it })
                } else {
                    OutlinedTextField(
                        value = workText,
                        onValueChange = { workText = it.filter { c -> c.isDigit() } },
                        label = { Text("Рабочих дней") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restText,
                        onValueChange = { restText = it.filter { c -> c.isDigit() } },
                        label = { Text("Дней отдыха") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMine, onCheckedChange = { isMine = it })
                        Text("Это мой состав", fontSize = 13.sp)
                    }

                    if (isEdit && currentOverrides.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Измененные вахты:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        currentOverrides.sortedBy { it.originalStart }.forEach { ov ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Было: ${ov.originalStart.format(fmt)}", fontSize = 13.sp)
                                    Text("Стало: ${ov.newStart.format(fmt)} · ${ov.workDays}/${ov.restDays}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { if (onRemoveOverride != null) onRemoveOverride(ov.originalStart) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = initial?.id ?: 0L
                    val newTemplate: CycleTemplate
                    if (isOneTime) {
                        if (endDate.isBefore(startDate) || selectedBrigade == null) return@TextButton
                        newTemplate = CycleTemplate(
                            id = id,
                            initialRule = CycleRule(workDays = 1, restDays = 0),
                            startDate = startDate,
                            endDate = endDate,
                            isOneTime = true,
                            baseColor = color,
                            overrides = initial?.overrides ?: emptyList(),
                            countBackward = false,
                            backwardLimitMonths = 0,
                            isMine = isMine,
                            brigadeId = selectedBrigade!!.id,
                            brigadeName = selectedBrigade!!.name
                        )
                    } else {
                        val work = workText.toIntOrNull() ?: return@TextButton
                        val rest = restText.toIntOrNull() ?: return@TextButton
                        if (work <= 0 || selectedBrigade == null) return@TextButton
                        newTemplate = CycleTemplate(
                            id = id,
                            initialRule = CycleRule(workDays = work, restDays = rest),
                            startDate = startDate,
                            endDate = null,
                            isOneTime = false,
                            baseColor = color,
                            overrides = initial?.overrides ?: emptyList(),
                            countBackward = false,
                            backwardLimitMonths = 0,
                            isMine = isMine,
                            brigadeId = selectedBrigade!!.id,
                            brigadeName = selectedBrigade!!.name
                        )
                    }
                    onSave(newTemplate)
                },
                enabled = selectedBrigade != null
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )

    if (showShiftSelection && initial != null && onAddSpecificShiftChange != null) {
        ShiftSelectionDialog(
            template = initial,
            onDismiss = { showShiftSelection = false },
            onSelect = { shift -> selectedShiftToEdit = shift; showShiftSelection = false }
        )
    }

    if (selectedShiftToEdit != null && initial != null && onAddSpecificShiftChange != null) {
        EditSpecificShiftDialog(
            shift = selectedShiftToEdit!!,
            defaultWorkDays = initial.initialRule.workDays,
            defaultRestDays = initial.initialRule.restDays,
            onDismiss = { selectedShiftToEdit = null },
            onConfirm = { originalStart, newStart, work, rest, shiftSubsequent ->
                onAddSpecificShiftChange(originalStart, newStart, work, rest, shiftSubsequent)
                selectedShiftToEdit = null
            }
        )
    }
}

@Composable
private fun ShiftSelectionDialog(
    template: CycleTemplate,
    onDismiss: () -> Unit,
    onSelect: (ShiftEntry) -> Unit
) {
    val today = LocalDate.now()
    val horizon = today.plusMonths(36)

    val shifts = remember(template) {
        CycleCalculator.expandCycleToDays(template, today, horizon)
            .filter { !it.end.isBefore(today) }
    }
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите вахту для изменения") },
        text = {
            if (shifts.isEmpty()) {
                Text("Будущих вахт не найдено. Проверьте настройки цикла.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shifts) { shift ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(shift) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "С ${shift.start.format(fmt)} по ${shift.end.format(fmt)}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Длительность: ${shift.end.toEpochDay() - shift.start.toEpochDay() + 1} дн.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun EditSpecificShiftDialog(
    shift: ShiftEntry,
    defaultWorkDays: Int,
    defaultRestDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate, Int, Int, Boolean) -> Unit
) {
    var newStartDate by remember { mutableStateOf(shift.start) }
    var newEndDate by remember { mutableStateOf(shift.end) }

    var workText by remember { mutableStateOf((shift.end.toEpochDay() - shift.start.toEpochDay() + 1).toString()) }
    var restText by remember { mutableStateOf(defaultRestDays.toString()) }

    var showModeSelection by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun updateEndDateFromWork() {
        val w = workText.toIntOrNull() ?: return
        if (w > 0) {
            newEndDate = newStartDate.plusDays(w.toLong() - 1)
        }
    }

    fun updateWorkFromEndDate() {
        val diff = java.time.temporal.ChronoUnit.DAYS.between(newStartDate, newEndDate) + 1
        if (diff > 0) {
            workText = diff.toString()
        }
    }

    if (!showModeSelection) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Изменить вахту") },
            text = {
                Column {
                    Text("Было: ${shift.start.format(fmt)} — ${shift.end.format(fmt)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    DateField(
                        label = "Новая дата начала",
                        date = newStartDate,
                        onDateChange = {
                            newStartDate = it
                            updateEndDateFromWork()
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    DateField(
                        label = "Новая дата конца",
                        date = newEndDate,
                        onDateChange = {
                            newEndDate = it
                            updateWorkFromEndDate()
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = workText,
                        onValueChange = {
                            workText = it.filter { c -> c.isDigit() }
                            updateEndDateFromWork()
                        },
                        label = { Text("Рабочих дней") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restText,
                        onValueChange = { restText = it.filter { c -> c.isDigit() } },
                        label = { Text("Дней отдыха") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val work = workText.toIntOrNull() ?: return@TextButton
                    val rest = restText.toIntOrNull() ?: return@TextButton
                    if (work <= 0) return@TextButton
                    showModeSelection = true
                }) { Text("Далее") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
        )
    } else {
        val work = workText.toIntOrNull() ?: 0
        val rest = restText.toIntOrNull() ?: 0

        AlertDialog(
            onDismissRequest = { showModeSelection = false },
            title = { Text("Как считать график после?") },
            text = {
                Column {
                    Text("Изменения вступают в силу с ${newStartDate.format(fmt)}.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { onConfirm(shift.start, newStartDate, work, rest, true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сдвинуть весь график")
                    }
                    Text("Эта и все последующие вахты сдвинутся. Следующая начнется через $rest дней отдыха после этой.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onConfirm(shift.start, newStartDate, work, rest, false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить старый ритм")
                    }
                    Text("Изменится только эта вахта. Следующая начнется в тот же день, что и по базовому графику.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { },
            dismissButton = { TextButton(onClick = { showModeSelection = false }) { Text("Назад") } }
        )
    }
}