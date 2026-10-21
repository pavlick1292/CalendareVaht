package com.example.calendarevaht.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarevaht.ui.theme.ElevatedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Красивая круглая иконка
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("CalendareVaht", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Версия 1.0.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // Карточка с описанием
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Удобный календарь вахтовых смен", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Приложение помогает планировать график вахт как по циклу так и разовых командировок, учитывать праздники и выходные, а также вести учёт состава бригад.\n\n" +
                                "Особенности:\n" +
                                "• Гибкая настройка графиков и сдвигов\n" +
                                "• Учёт состава бригад и должностей\n" +
                                "• Виджет с обратным отсчётом\n" +
                                "• Экспорт в PDF для печати на А4\n" +
                                "• Статистика по месяцам и часам",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))


        }
    }
}