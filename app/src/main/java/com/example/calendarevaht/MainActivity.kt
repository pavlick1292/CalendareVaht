package com.example.calendarevaht

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.calendarevaht.data.ThemePreferences
import com.example.calendarevaht.ui.MainScreen
import com.example.calendarevaht.ui.theme.AppTheme
import com.example.calendarevaht.ui.theme.CalendareVahtTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            val currentTheme by themePrefs.theme.collectAsState(initial = AppTheme.SYSTEM)

            // Создаем скоуп для запуска корутин (сохранения темы)
            val scope = rememberCoroutineScope()

            CalendareVahtTheme(theme = currentTheme) {
                MainScreen(
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme ->
                        // Запускаем сохранение в фоне
                        scope.launch {
                            themePrefs.save(newTheme)
                        }
                    }
                )
            }
        }
    }
}