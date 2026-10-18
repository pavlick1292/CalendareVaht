package com.example.calendarevaht.widget

import android.content.Context

object WidgetTheme {
    private const val PREFS = "widget_prefs"
    private const val KEY_THEME = "widget_theme"

    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_TRANSPARENT = 2

    fun getTheme(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, THEME_DARK)
    }

    fun setTheme(context: Context, theme: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME, theme)
            .apply()
    }

    fun nextTheme(current: Int): Int {
        return when (current) {
            THEME_LIGHT -> THEME_DARK
            THEME_DARK -> THEME_TRANSPARENT
            else -> THEME_LIGHT
        }
    }
}