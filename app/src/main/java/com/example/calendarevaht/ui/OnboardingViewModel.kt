package com.example.calendarevaht.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarevaht.data.OnboardingPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = OnboardingPreferences(app)

    val shown = prefs.shown.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun markShown() {
        viewModelScope.launch { prefs.markShown() }
    }
}