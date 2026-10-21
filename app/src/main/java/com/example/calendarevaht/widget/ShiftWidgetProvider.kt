package com.example.calendarevaht.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.calendarevaht.MainActivity
import com.example.calendarevaht.R
import com.example.calendarevaht.data.ShiftPreferences
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ShiftWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_THEME) {
            val current = WidgetTheme.getTheme(context)
            WidgetTheme.setTheme(context, WidgetTheme.nextTheme(current))
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_THEME = "com.example.calendarevaht.widget.THEME"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ShiftWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val prefs = ShiftPreferences(context)
                val templates = prefs.templates.first()
                val mine = templates.firstOrNull { it.isMine }

                val result = calculateStatus(mine, LocalDate.now())

                // Берем тему из настроек ВИДЖЕТА
                val theme = WidgetTheme.getTheme(context)

                val layoutId = when (theme) {
                    WidgetTheme.THEME_LIGHT -> R.layout.widget_light
                    WidgetTheme.THEME_TRANSPARENT -> R.layout.widget_transparent
                    else -> R.layout.widget_dark
                }

                val views = RemoteViews(context.packageName, layoutId)

                // Установка текста
                if (result.daysToHome != null) {
                    views.setTextViewText(R.id.widgetToWork, result.daysToHome.toString())
                    views.setTextViewText(R.id.widgetLabelToWork, "до дома")
                } else if (result.daysToWork != null) {
                    views.setTextViewText(R.id.widgetToWork, result.daysToWork.toString())
                    views.setTextViewText(R.id.widgetLabelToWork, "до вахты")
                } else {
                    views.setTextViewText(R.id.widgetToWork, "—")
                    views.setTextViewText(R.id.widgetLabelToWork, "нет данных")
                }

                views.setTextViewText(R.id.widgetTitle, result.title)
                views.setProgressBar(R.id.widgetProgress, 100, result.progress, false)

                // Цвета текста в зависимости от темы
                if (theme == WidgetTheme.THEME_LIGHT) {
                    views.setTextColor(R.id.widgetToWork, Color.parseColor("#121212"))
                    views.setTextColor(R.id.widgetLabelToWork, Color.parseColor("#555555"))
                    views.setTextColor(R.id.widgetTitle, Color.parseColor("#121212"))
                } else {
                    views.setTextColor(R.id.widgetToWork, Color.WHITE)
                    views.setTextColor(R.id.widgetLabelToWork, Color.parseColor("#CCCCCC"))
                    views.setTextColor(R.id.widgetTitle, Color.WHITE)
                }

                // Клик по виджету (открыть приложение)
                val openIntent = Intent(context, MainActivity::class.java)
                val openPending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, openPending)

                // Клик по иконке смены темы
                val themeIntent = Intent(context, ShiftWidgetProvider::class.java).apply {
                    action = ACTION_THEME
                }
                val themePending = PendingIntent.getBroadcast(
                    context, 2, themeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetTheme, themePending)

                manager.updateAppWidget(widgetId, views)
            }
        }

        private data class WidgetStatus(
            val daysToWork: Int?,
            val daysToHome: Int?,
            val title: String,
            val progress: Int
        )

        private fun calculateStatus(template: CycleTemplate?, today: LocalDate): WidgetStatus {
            if (template == null) return WidgetStatus(null, null, "Моя вахта", 0)

            val horizon = today.plusDays(365)
            val entries = CycleCalculator.expandCycleToDays(template, today.minusDays(365), horizon)

            val currentShift = entries.firstOrNull { !today.isBefore(it.start) && !today.isAfter(it.end) }
            if (currentShift != null) {
                val total = ChronoUnit.DAYS.between(currentShift.start, currentShift.end).toInt() + 1
                val currentDay = ChronoUnit.DAYS.between(currentShift.start, today).toInt() + 1
                val toHome = total - currentDay + 1
                val progress = ((currentDay.toFloat() / total) * 100).toInt()
                return WidgetStatus(null, toHome, "Идёт вахта", progress)
            }

            val nextShift = entries.firstOrNull { it.start.isAfter(today) }
            val prevShift = entries.lastOrNull { it.end.isBefore(today) }

            if (nextShift != null && prevShift != null) {
                val totalRest = ChronoUnit.DAYS.between(prevShift.end, nextShift.start).toInt() - 1
                val currentRest = ChronoUnit.DAYS.between(prevShift.end, today).toInt()
                val toWork = ChronoUnit.DAYS.between(today, nextShift.start).toInt()
                val progress = if (totalRest > 0) ((currentRest.toFloat() / totalRest) * 100).toInt() else 0
                return WidgetStatus(toWork, null, "Отдых", progress)
            } else if (nextShift != null) {
                val toWork = ChronoUnit.DAYS.between(today, nextShift.start).toInt()
                return WidgetStatus(toWork, null, "До вахты", 0)
            }

            return WidgetStatus(null, null, "Нет данных", 0)
        }
    }
}