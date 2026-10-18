package com.example.calendarevaht.ui.more

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoreViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsPrefs = SettingsPreferences(app)

    val shiftHours = settingsPrefs.settings
        .map { it.shiftHours }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 11
        )

    fun save(shift: Int) {
        viewModelScope.launch {
            settingsPrefs.saveHours(shift.coerceAtLeast(1))
        }
    }
}