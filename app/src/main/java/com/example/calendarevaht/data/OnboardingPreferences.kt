package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

class OnboardingPreferences(private val context: Context) {
    private val key = booleanPreferencesKey("onboarding_shown")

    val shown = context.dataStore.data.map { prefs ->
        prefs[key] ?: false
    }

    suspend fun markShown() {
        context.dataStore.edit { it[key] = true }
    }
}