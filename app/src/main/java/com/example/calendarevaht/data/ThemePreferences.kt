package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calendarevaht.ui.theme.AppTheme
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    private val KEY = stringPreferencesKey("app_theme")

    val theme = context.dataStore.data.map { prefs ->
        val name = prefs[KEY] ?: AppTheme.LIGHT.name
        runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.LIGHT)
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[KEY] = theme.name }
    }
}