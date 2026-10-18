package com.example.calendarevaht

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calendarevaht.data.ThemePreferences
import com.example.calendarevaht.ui.MainScreen
import com.example.calendarevaht.ui.theme.AppTheme
import com.example.calendarevaht.ui.theme.CalendareVahtTheme
import com.example.calendarevaht.widget.ShiftWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShiftWidgetProvider.refreshAll(applicationContext)
        val prefs = ThemePreferences(applicationContext)
        setContent {
            val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = AppTheme.LIGHT)
            CalendareVahtTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShiftWidgetProvider.refreshAll(applicationContext)
    }
}