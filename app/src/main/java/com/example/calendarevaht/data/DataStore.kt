package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "calendare_vaht_prefs")