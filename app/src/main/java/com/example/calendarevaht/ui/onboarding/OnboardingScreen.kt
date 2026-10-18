package com.example.calendarevaht.ui.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPage(
    val title: String,
    val text: String
)

private val pages = listOf(
    OnboardingPage(
        title = "Добро пожаловать!",
        text = "Это приложение для учёта вахтовых смен. Оно покажет ваш график, посчитает часы и напомнит о начале и конце вахты."
    ),
    OnboardingPage(
        title = "Вкладка «Вахты»",
        text = "Здесь вы создаёте свои вахты. Укажите дату начала, количество рабочих и выходных дней, цвет. Если вахта разовая — поставьте галочку «Разовая».\n\nГалочка «Мой состав» делает вахту основной — именно по ней считается прогресс на Главной."
    ),
    OnboardingPage(
        title = "Главная",
        text = "На Главной видно:\n• место и текущую погоду\n• сколько дней до вахты или сколько уже идёт\n• календарь смен на месяц\n• ближайшие события\n\nСвайпните календарь влево/вправо, чтобы сменить месяц."
    ),
    OnboardingPage(
        title = "Виджет на рабочий стол",
        text = "Добавьте наш виджет на главный экран телефона, чтобы всегда видеть, сколько дней осталось до вахты или до дома, не открывая приложение!\n\nДля этого зажмите палец на свободном месте рабочего стола, выберите «Виджеты» и найдите «CalendareVaht»."
    ),
    OnboardingPage(
        title = "Настройки",
        text = "В разделе «Ещё → Настройки» выберите тему, укажите место вахты (город) и часы в смене.\n\nГород нужен для показа погоды. После ввода нажмите «Сохранить и найти координаты»."
    ),
    OnboardingPage(
        title = "Сводка и Статистика",
        text = "«Сводка» покажет отчёт за месяц: сколько дней на вахте, выходных, праздников и отдыха.\n\n«Ещё → Статистика» сравнивает ваш состав и сменщика: дни, часы, захваченные выходные и праздники."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val page = pages[index]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    page.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.text,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onFinish) {
                        Text("Пропустить")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        pages.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (index < pages.size - 1) index++
                            else onFinish()
                        }
                    ) {
                        Text(if (index < pages.size - 1) "Далее" else "Понятно")
                    }
                }
            }
        }
    }
}