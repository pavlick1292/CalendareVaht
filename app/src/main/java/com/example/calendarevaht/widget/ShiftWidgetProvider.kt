package com.example.calendarevaht.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        when (intent.action) {
            ACTION_REFRESH -> refreshAll(context)
            ACTION_THEME -> {
                val current = WidgetTheme.getTheme(context)
                WidgetTheme.setTheme(context, WidgetTheme.nextTheme(current))
                refreshAll(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.calendarevaht.widget.REFRESH"
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

                // Получаем статус: либо дни до вахты, либо дни до дома
                val (toWork, toHome) = calculateDays(mine, LocalDate.now())

                val theme = WidgetTheme.getTheme(context)
                val layoutId = when (theme) {
                    WidgetTheme.THEME_LIGHT -> R.layout.widget_light
                    WidgetTheme.THEME_TRANSPARENT -> R.layout.widget_transparent
                    else -> R.layout.widget_dark
                }

                val views = RemoteViews(context.packageName, layoutId)

                // Логика отображения: показываем только одно значение в центре
                if (toHome != null) {
                    // Сейчас на вахте -> показываем "до дома"
                    views.setTextViewText(R.id.widgetToWork, toHome.toString())
                    views.setTextViewText(R.id.widgetLabelToWork, "до дома")
                } else if (toWork != null) {
                    // Сейчас дома -> показываем "до вахты"
                    views.setTextViewText(R.id.widgetToWork, toWork.toString())
                    views.setTextViewText(R.id.widgetLabelToWork, "до вахты")
                } else {
                    // Нет данных
                    views.setTextViewText(R.id.widgetToWork, "—")
                    views.setTextViewText(R.id.widgetLabelToWork, "нет данных")
                }

                // Открыть приложение
                val openIntent = Intent(context, MainActivity::class.java)
                val openPending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, openPending)

                // Обновить
                val refreshIntent = Intent(context, ShiftWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context, 1, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRefresh, refreshPending)

                // Сменить тему
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

        /**
         * Возвращает пару (дней до начала вахты, дней до дома).
         * Если сейчас на вахте — (null, дни до конца вахты).
         * Если дома — (дни до начала вахты, null).
         */
        private fun calculateDays(template: CycleTemplate?, today: LocalDate): Pair<Int?, Int?> {
            if (template == null) return (null to null)

            // Найдём текущую и следующую вахту
            val horizon = today.plusDays(365)
            val entries = CycleCalculator.expandCycleToDays(template, today.minusDays(365), horizon)

            // Ищем, идёт ли вахта сегодня
            val currentShift = entries.firstOrNull { !today.isBefore(it.start) && !today.isAfter(it.end) }
            if (currentShift != null) {
                val toHome = ChronoUnit.DAYS.between(today, currentShift.end).toInt() + 1
                return (null to toHome)
            }

            // Иначе ищем ближайшую будущую вахту
            val nextShift = entries.firstOrNull { it.start.isAfter(today) } ?: return (null to null)
            val toWork = ChronoUnit.DAYS.between(today, nextShift.start).toInt()
            return (toWork to null)
        }
    }
}