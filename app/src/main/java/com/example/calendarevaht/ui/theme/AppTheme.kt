package com.example.calendarevaht.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val title: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    BLUE("Синяя"),
    GREEN("Зелёная"),
    RED("Красная"),
    PURPLE("Фиолетовая")
}

private fun scheme(primary: Color, secondary: Color, tertiary: Color, dark: Boolean) =
    if (dark) darkColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
    else lightColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)

@Composable
fun CalendareVahtTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.LIGHT -> scheme(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), false)
        AppTheme.DARK -> scheme(Color(0xFF90CAF9), Color(0xFFA5D6A7), Color(0xFFFFCC80), true)
        AppTheme.BLUE -> scheme(Color(0xFF1565C0), Color(0xFF0288D1), Color(0xFF00ACC1), false)
        AppTheme.GREEN -> scheme(Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFF00897B), false)
        AppTheme.RED -> scheme(Color(0xFFC62828), Color(0xFFD84315), Color(0xFFAD1457), false)
        AppTheme.PURPLE -> scheme(Color(0xFF6A1B9A), Color(0xFF4527A0), Color(0xFF283593), false)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}