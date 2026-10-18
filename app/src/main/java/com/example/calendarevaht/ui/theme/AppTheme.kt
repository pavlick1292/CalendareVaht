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

@Composable
fun CalendareVahtTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.LIGHT -> lightColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFBBDEFB),
            onPrimaryContainer = Color(0xFF0D47A1),
            secondary = Color(0xFF388E3C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFC8E6C9),
            onSecondaryContainer = Color(0xFF1B5E20),
            background = Color(0xFFF2F4F7),
            onBackground = Color(0xFF212121),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF212121),
            surfaceVariant = Color(0xFFE9EDF2),
            onSurfaceVariant = Color(0xFF4A4A4A)
        )

        AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF1565C0),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFA5D6A7),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF2E7D32),
            onSecondaryContainer = Color.White,
            background = Color(0xFF0D0D0D),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1C1C1C),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFB0B0B0)
        )

        AppTheme.BLUE -> lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB3E5FC),
            onPrimaryContainer = Color(0xFF01579B),
            secondary = Color(0xFF0288D1),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFB3E5FC),
            onSecondaryContainer = Color(0xFF01579B),
            background = Color(0xFFDCEAF7),
            onBackground = Color(0xFF0D47A1),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D47A1),
            surfaceVariant = Color(0xFFCBE3F5),
            onSurfaceVariant = Color(0xFF1565C0)
        )

        AppTheme.GREEN -> lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF558B2F),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDCEDC8),
            onSecondaryContainer = Color(0xFF33691E),
            background = Color(0xFFE7F1DA),
            onBackground = Color(0xFF1B5E20),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1B5E20),
            surfaceVariant = Color(0xFFDCEBC9),
            onSurfaceVariant = Color(0xFF33691E)
        )

        AppTheme.RED -> lightColorScheme(
            primary = Color(0xFFC62828),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFCDD2),
            onPrimaryContainer = Color(0xFFB71C1C),
            secondary = Color(0xFFD84315),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFCCBC),
            onSecondaryContainer = Color(0xFFBF360C),
            background = Color(0xFFF7E1E1),
            onBackground = Color(0xFFB71C1C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFFB71C1C),
            surfaceVariant = Color(0xFFF8D5D5),
            onSurfaceVariant = Color(0xFFC62828)
        )

        AppTheme.PURPLE -> lightColorScheme(
            primary = Color(0xFF6A1B9A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE1BEE7),
            onPrimaryContainer = Color(0xFF4A148C),
            secondary = Color(0xFF4527A0),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD1C4E9),
            onSecondaryContainer = Color(0xFF311B92),
            background = Color(0xFFEDE1F1),
            onBackground = Color(0xFF4A148C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF4A148C),
            surfaceVariant = Color(0xFFE7D1EC),
            onSurfaceVariant = Color(0xFF6A1B9A)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}