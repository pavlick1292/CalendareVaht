package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.calendarevaht.ui.theme.AppTheme
import kotlinx.coroutines.flow.map

class ThemePreferences(private val context: Context) {
    private val key = stringPreferencesKey("app_theme")

    val theme = context.dataStore.data.map { prefs ->
        val name = prefs[key]
        runCatching { AppTheme.valueOf(name ?: AppTheme.LIGHT.name) }
            .getOrDefault(AppTheme.LIGHT)
    }

    suspend fun save(theme: AppTheme) {
        context.dataStore.edit { it[key] = theme.name }
    }
}