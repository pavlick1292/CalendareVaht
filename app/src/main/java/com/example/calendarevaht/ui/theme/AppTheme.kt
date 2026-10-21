package com.example.calendarevaht.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val title: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    INDUSTRIAL("Индустриальная"),
    SYSTEM("Системная")
}

@Composable
fun CalendareVahtTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val isDark = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.INDUSTRIAL -> true // Индустриальная всегда темная
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val scheme = when {
        theme == AppTheme.INDUSTRIAL -> {
            darkColorScheme(
                primary = Color(0xFFFFD700),         // Яркий желтый (Liebherr)
                onPrimary = Color(0xFF000000),       // Черный текст на желтом
                primaryContainer = Color(0xFF333333),
                onPrimaryContainer = Color(0xFFFFD700),
                secondary = Color(0xFFCCCCCC),
                onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFF333333),
                onSecondaryContainer = Color(0xFFFFFFFF),
                background = Color(0xFF121212),      // Глубокий черный
                onBackground = Color(0xFFFFFFFF),    // Белый текст
                surface = Color(0xFF1E1E1E),         // Темно-серые карточки
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF2C2C2C),  // Ячейки календаря
                onSurfaceVariant = Color(0xFFCCCCCC),
                tertiary = Color(0xFFFFD700)         // Рамка "сегодня"
            )
        }
        isDark -> {
            darkColorScheme(
                primary = Color(0xFFD0BCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                secondary = Color(0xFFCCC2DC),
                onSecondary = Color(0xFF332D41),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                background = Color(0xFF141218),
                onBackground = Color(0xFFE6E1E5),
                surface = Color(0xFF1D1B20),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF49454F),
                onSurfaceVariant = Color(0xFFCAC4D0),
                tertiary = Color(0xFFD0BCFF)
            )
        }
        else -> {
            lightColorScheme(
                primary = Color(0xFF6650a4),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                secondary = Color(0xFF625b71),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1D192B),
                background = Color(0xFFFEF7FF),
                onBackground = Color(0xFF1D1B20),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFE7E0EC),
                onSurfaceVariant = Color(0xFF49454F),
                tertiary = Color(0xFF6650a4)
            )
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}