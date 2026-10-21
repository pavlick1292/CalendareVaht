package com.example.calendarevaht.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarevaht.ui.theme.AppTheme
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val settings by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val options by vm.options.collectAsState()

    var place by remember(settings.shiftPlace) { mutableStateOf(settings.shiftPlace) }
    var hours by remember(settings.shiftHours) { mutableStateOf(settings.shiftHours.toString()) }

    // Автоматический поиск при вводе города (с задержкой)
    LaunchedEffect(Unit) {
        snapshotFlow { place }
            .debounce(500L)
            .distinctUntilChanged()
            .collect { query ->
                if (query.length >= 2 && query != settings.shiftPlace) {
                    vm.savePlace(query)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        // Тема
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Тема оформления", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                AppTheme.entries.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(t) } // <-- Вызываем проброшенную функцию
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t.title, fontSize = 14.sp)
                        if (t == currentTheme) Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Вахта
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Вахта", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Место вахты (город)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                // Выпадающий список найденных городов
                options.forEach { opt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { vm.selectOption(opt) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            opt.label,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 13.sp
                        )
                    }
                }

                message?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (settings.latitude != null && settings.longitude != null) {
                    Text(
                        "Сохранено: ${"%.4f".format(settings.latitude)}, ${"%.4f".format(settings.longitude)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Часов в смену") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Сохранить часы",
                    modifier = Modifier
                        .clickable {
                            val v = hours.toIntOrNull() ?: return@clickable
                            vm.saveHours(v)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}