package com.example.calendarevaht.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.calendarevaht.domain.Brigade
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.Person
import com.example.calendarevaht.domain.ShiftEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth

private const val MARGIN = 20f
private val RED_HOLIDAY = Color.rgb(229, 57, 53)
private val GRAY_EMPTY = Color.rgb(245, 245, 245)
private val BORDER_COLOR = Color.rgb(200, 200, 200)
private val BLACK_TEXT = Color.BLACK

// Жестко задаем названия месяцев для правильного падежа
private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

fun exportCalendar(
    context: Context,
    year: Int,
    templates: List<CycleTemplate>,
    brigades: List<Brigade>,
    persons: List<Person>,
    holidays: Map<LocalDate, String>,
    shiftHours: Int,
    isLandscape: Boolean,
    withStats: Boolean
) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            buildPdf(context, year, templates, brigades, persons, holidays, shiftHours, isLandscape, withStats)
        }.onFailure {
            it.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun buildPdf(
    context: Context,
    year: Int,
    templates: List<CycleTemplate>,
    brigades: List<Brigade>,
    persons: List<Person>,
    holidays: Map<LocalDate, String>,
    shiftHours: Int,
    isLandscape: Boolean,
    withStats: Boolean
) {
    val doc = PdfDocument()

    val pageW = if (isLandscape) 842 else 595
    val pageH = if (isLandscape) 595 else 842

    var pageNum = 1

    if (withStats) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum++).create()
        val page = doc.startPage(pageInfo)
        drawStatsPage(page.canvas, year, templates, brigades, persons, holidays, shiftHours, pageW.toFloat())
        doc.finishPage(page)
    }

    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
    val page = doc.startPage(pageInfo)
    drawCalendarPage(page.canvas, year, templates, brigades, persons, holidays, isLandscape, pageW.toFloat(), pageH.toFloat())
    doc.finishPage(page)
    val gapX = 6f // Было 10f, уменьшили, чтобы календарь был шире
    val gapY = 8f // Было 10f, уменьшили по вертикали
    val fileName = "CalendareVaht_${year}_${if (withStats) "report" else "calendar"}.pdf"
    val uri = savePdf(context, fileName, doc)
    doc.close()
    if (uri != null) {
        CoroutineScope(Dispatchers.Main).launch {
            sharePdf(context, uri)
        }
    }
}

private fun drawStatsPage(
    canvas: Canvas,
    year: Int,
    templates: List<CycleTemplate>,
    brigades: List<Brigade>,
    persons: List<Person>,
    holidays: Map<LocalDate, String>,
    shiftHours: Int,
    pageW: Float
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var y = MARGIN + 20f
    val pageH = 842f // Высота А4

    // Заголовок
    paint.color = BLACK_TEXT
    paint.textSize = 20f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Статистика за $year год", MARGIN, y, paint)
    y += 30f

    // 1. Сводка
    paint.textSize = 10f
    templates.forEach { t ->
        val brigade = brigades.find { it.id == t.brigadeId }
        val brigadePersons = brigade?.personIds?.mapNotNull { id -> persons.find { it.id == id } } ?: emptyList()

        var totalDays = 0
        var totalHours = 0
        var totalWeekends = 0
        var totalHolidays = 0

        for (m in 1..12) {
            val ym = YearMonth.of(year, m)
            val daysInMonth = mutableSetOf<LocalDate>()
            CycleCalculator.expandCycleToDays(t, ym.atDay(1), ym.atEndOfMonth()).forEach { e ->
                var d = e.start
                while (!d.isAfter(e.end)) {
                    if (!d.isBefore(ym.atDay(1)) && !d.isAfter(ym.atEndOfMonth())) {
                        daysInMonth.add(d)
                    }
                    d = d.plusDays(1)
                }
            }
            totalDays += daysInMonth.size
            totalHours += daysInMonth.size * shiftHours
            totalWeekends += daysInMonth.count { it.dayOfWeek.value >= 6 }
            totalHolidays += daysInMonth.count { holidays.containsKey(it) }
        }

        paint.color = Color.parseColor(toHex(t.baseColor))
        canvas.drawRect(MARGIN, y - 9f, MARGIN + 10, y, paint)

        paint.color = BLACK_TEXT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(t.brigadeName, MARGIN + 15f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        val names = brigadePersons.joinToString(", ") { it.fullName }
        canvas.drawText(names, MARGIN + 100f, y, paint)

        y += 14f
        paint.color = BLACK_TEXT
        val statsText = "Дней: $totalDays  |  Часов: $totalHours  |  Выходных: $totalWeekends  |  Праздников: $totalHolidays"
        canvas.drawText(statsText, MARGIN + 15f, y, paint)

        y += 18f
    }

    y += 5f

    // 2. График (сделали пониже, чтобы освободить место таблице)
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Отработано часов по месяцам", MARGIN, y, paint)
    y += 15f

    // Легенда
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    var legendX = MARGIN
    templates.forEach { t ->
        paint.color = Color.parseColor(toHex(t.baseColor))
        canvas.drawRect(legendX, y - 7f, legendX + 8, y, paint)
        paint.color = BLACK_TEXT
        canvas.drawText(t.brigadeName, legendX + 12f, y, paint)
        legendX += 120f
    }

    y += 10f

    val chartStartX = MARGIN
    val chartWidth = pageW - 2 * MARGIN
    val chartHeight = 130f // Уменьшили высоту
    val chartTop = y
    val chartBottom = chartTop + chartHeight

    paint.color = Color.TRANSPARENT
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(chartStartX, chartTop, chartStartX + chartWidth, chartBottom, paint)
    paint.style = Paint.Style.FILL

    var maxVal = 0
    for (m in 1..12) {
        val ym = YearMonth.of(year, m)
        templates.forEach { t ->
            val days = countDays(listOf(t), ym.atDay(1), ym.atEndOfMonth())
            if (days * shiftHours > maxVal) maxVal = days * shiftHours
        }
    }
    if (maxVal == 0) maxVal = 1

    val colW = chartWidth / 12f
    val barW = (colW - 16f) / templates.size

    for (m in 1..12) {
        val ym = YearMonth.of(year, m)
        val cx = chartStartX + (m - 1) * colW + 8f

        templates.forEachIndexed { idx, t ->
            val days = countDays(listOf(t), ym.atDay(1), ym.atEndOfMonth())
            val hours = days * shiftHours
            val barH = (hours.toFloat() / maxVal) * chartHeight

            paint.color = Color.parseColor(toHex(t.baseColor))
            canvas.drawRect(
                cx + idx * barW,
                chartBottom - barH,
                cx + (idx + 1) * barW - 2f,
                chartBottom,
                paint
            )
        }

        paint.color = BLACK_TEXT
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(MONTH_NAMES[m - 1].take(3), cx + colW / 2f - 4f, chartBottom + 10f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    y = chartBottom + 20f

    // 3. Таблица с нормальными отступами
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Детализация по месяцам", MARGIN, y, paint)
    y += 16f

    paint.textSize = 9f
    canvas.drawText("Месяц", MARGIN, y, paint)

    // Задаем нормальные координаты для колонок (чтобы текст не налезал)
    val offsetMine = MARGIN + 80f
    val offsetOther = MARGIN + 260f

    if (templates.isNotEmpty()) {
        canvas.drawText(templates.getOrNull(0)?.brigadeName?.take(12) ?: "Мой состав", offsetMine, y, paint)
    }
    if (templates.size > 1) {
        canvas.drawText(templates.getOrNull(1)?.brigadeName?.take(12) ?: "Сменщик", offsetOther, y, paint)
    }

    y += 12f
    paint.textSize = 8f
    if (templates.isNotEmpty()) {
        canvas.drawText("Дн    Час   Вых   Пр", offsetMine, y, paint)
    }
    if (templates.size > 1) {
        canvas.drawText("Дн    Час   Вых   Пр", offsetOther, y, paint)
    }

    y += 6f
    canvas.drawLine(MARGIN, y, pageW - MARGIN, y, paint)
    y += 10f

    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    for (m in 1..12) {
        if (y > pageH - MARGIN) break // Защита от вылезания

        val ym = YearMonth.of(year, m)
        paint.color = BLACK_TEXT
        canvas.drawText(MONTH_NAMES[m - 1], MARGIN, y, paint)

        templates.forEachIndexed { idx, t ->
            val daysInMonth = mutableSetOf<LocalDate>()
            CycleCalculator.expandCycleToDays(t, ym.atDay(1), ym.atEndOfMonth()).forEach { e ->
                var d = e.start
                while (!d.isAfter(e.end)) {
                    if (!d.isBefore(ym.atDay(1)) && !d.isAfter(ym.atEndOfMonth())) {
                        daysInMonth.add(d)
                    }
                    d = d.plusDays(1)
                }
            }
            val days = daysInMonth.size
            val hours = days * shiftHours
            val weekends = daysInMonth.count { it.dayOfWeek.value >= 6 }
            val hol = daysInMonth.count { holidays.containsKey(it) }

            val xPos = if (idx == 0) offsetMine else offsetOther
            // Используем фиксированный шаг для цифр, чтобы они стояли ровно
            val dStr = days.toString()
            val hStr = hours.toString()
            val wStr = weekends.toString()
            val pStr = hol.toString()

            canvas.drawText(dStr, xPos, y, paint)
            canvas.drawText(hStr, xPos + 25f, y, paint)
            canvas.drawText(wStr, xPos + 60f, y, paint)
            canvas.drawText(pStr, xPos + 90f, y, paint)
        }
        y += 14f
    }
}

// Вспомогательный data-класс
private data class CompositionStatsData(
    val template: CycleTemplate,
    val persons: List<Person>,
    val days: Int,
    val hours: Int,
    val weekends: Int,
    val holidays: Int
)

private fun drawCalendarPage(
    canvas: Canvas,
    year: Int,
    templates: List<CycleTemplate>,
    brigades: List<Brigade>,
    persons: List<Person>,
    holidays: Map<LocalDate, String>,
    isLandscape: Boolean,
    pageW: Float,
    pageH: Float
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = BLACK_TEXT
    paint.textSize = 18f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Календарь смен на $year год", MARGIN, MARGIN + 16f, paint)

    var startY = MARGIN + 36f

    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    templates.forEach { t ->
        val brigade = brigades.find { it.id == t.brigadeId }
        val names = brigade?.personIds?.mapNotNull { id -> persons.find { it.id == id }?.fullName }?.joinToString(", ") ?: ""

        paint.color = Color.parseColor(toHex(t.baseColor))
        canvas.drawRect(MARGIN, startY - 8f, MARGIN + 10, startY, paint)

        paint.color = BLACK_TEXT
        canvas.drawText("${t.brigadeName} ($names)", MARGIN + 15f, startY, paint)
        startY += 12f
    }

    paint.color = RED_HOLIDAY
    canvas.drawRect(MARGIN, startY - 8f, MARGIN + 10, startY, paint)
    paint.color = BLACK_TEXT
    canvas.drawText("Выходной / Праздник", MARGIN + 15f, startY, paint)

    startY += 15f

    val allDays = expand(templates, year)

    val gapX = 10f
    val gapY = 10f

    if (isLandscape) {
        // Альбомная (3 колонки, 4 ряда)
        val colW = (pageW - 2 * MARGIN - 2 * gapX) / 3f
        val rowH = (pageH - startY - MARGIN - 3 * gapY) / 4f

        for (row in 0 until 4) {
            for (col in 0 until 3) {
                val monthIndex = row * 3 + col + 1
                val x0 = MARGIN + col * (colW + gapX)
                val y0 = startY + row * (rowH + gapY)
                drawMonth(canvas, paint, year, monthIndex, allDays, holidays, x0, y0, colW, rowH)
            }
        }
    } else {
        // Книжная (2 колонки, 6 рядов)
        val colW = (pageW - 2 * MARGIN - gapX) / 2f
        val rowH = (pageH - startY - MARGIN - 5 * gapY) / 6f

        for (row in 0 until 6) {
            for (col in 0 until 2) {
                val monthIndex = row * 2 + col + 1
                val x0 = MARGIN + col * (colW + gapX)
                val y0 = startY + row * (rowH + gapY)
                drawMonth(canvas, paint, year, monthIndex, allDays, holidays, x0, y0, colW, rowH)
            }
        }
    }
}

private fun drawMonth(
    canvas: Canvas,
    paint: Paint,
    year: Int,
    month: Int,
    days: Map<LocalDate, List<ShiftEntry>>,
    holidays: Map<LocalDate, String>,
    x0: Float,
    y0: Float,
    w: Float,
    h: Float
) {
    val ym = YearMonth.of(year, month)

    // Заголовок месяца (по центру)
    paint.color = BLACK_TEXT
    paint.textSize = 11f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("${MONTH_NAMES[month - 1]} $year", x0 + w / 2f, y0 + 12f, paint)

    // Ширина ячейки: делим всю ширину месяца на 7 (без лишних отступов)
    val cellW = w / 7f
    val headerY = y0 + 24f

    // Дни недели (Пн-Вс) - крупнее и по центру
    paint.textSize = 9f
    paint.textAlign = Paint.Align.CENTER
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    weekDays.forEachIndexed { i, day ->
        canvas.drawText(day, x0 + i * cellW + cellW / 2f, headerY, paint)
    }

    // Сетка
    val gridTop = headerY + 6f
    val availableH = h - (gridTop - y0) - 4f
    val cellH = availableH / 6f

    val first = ym.atDay(1)
    val leading = (first.dayOfWeek.value + 6) % 7
    val total = ym.lengthOfMonth()

    for (d in 1..total) {
        val idx = leading + d - 1
        val col = idx % 7
        val row = idx / 7

        // Убрали отступы +4f, чтобы ячейки занимали всю ширину
        val cx = x0 + col * cellW
        val cy = gridTop + row * cellH

        val date = ym.atDay(d)
        val isHoliday = holidays.containsKey(date)
        val isWeekend = date.dayOfWeek.value >= 6
        val dayShifts = days[date].orEmpty()

        // Фон
        if (dayShifts.isEmpty()) {
            paint.color = GRAY_EMPTY
            canvas.drawRect(cx, cy, cx + cellW, cy + cellH, paint)
        } else {
            val distinct = dayShifts.distinctBy { it.color }
            if (distinct.size >= 2) {
                // Горизонтальное деление
                paint.color = Color.parseColor(toHex(distinct[0].color))
                canvas.drawRect(cx, cy, cx + cellW, cy + cellH / 2f, paint)
                paint.color = Color.parseColor(toHex(distinct[1].color))
                canvas.drawRect(cx, cy + cellH / 2f, cx + cellW, cy + cellH, paint)
            } else {
                paint.color = Color.parseColor(toHex(distinct[0].color))
                canvas.drawRect(cx, cy, cx + cellW, cy + cellH, paint)
            }
        }

        // Рамка ячейки
        paint.color = BORDER_COLOR
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(cx, cy, cx + cellW, cy + cellH, paint)
        paint.style = Paint.Style.FILL

        // Красная полоска внизу для праздников/выходных
        if (isHoliday || isWeekend) {
            paint.color = RED_HOLIDAY
            canvas.drawRect(cx, cy + cellH - 2f, cx + cellW, cy + cellH, paint)
        }

        // Цифра дня (крупнее и по центру)
        paint.color = if (dayShifts.isNotEmpty()) Color.WHITE else if (isHoliday || isWeekend) RED_HOLIDAY else BLACK_TEXT
        paint.textSize = 10f // Увеличили размер цифры
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val textY = cy + cellH / 2f + 3.5f
        canvas.drawText(d.toString(), cx + cellW / 2f, textY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Возвращаем выравнивание по левому краю
    paint.textAlign = Paint.Align.LEFT
}

// Вспомогательные функции (expand, countDays, toHex, savePdf, sharePdf) без изменений
private fun expand(templates: List<CycleTemplate>, year: Int): Map<LocalDate, List<ShiftEntry>> {
    val from = LocalDate.of(year, 1, 1)
    val to = LocalDate.of(year, 12, 31)
    val map = mutableMapOf<LocalDate, MutableList<ShiftEntry>>()
    templates.forEach { t ->
        CycleCalculator.expandCycleToDays(t, from, to).forEach { e ->
            var d = e.start
            while (!d.isAfter(e.end) && !d.isAfter(to)) {
                if (!d.isBefore(from)) map.getOrPut(d) { mutableListOf() }.add(e)
                d = d.plusDays(1)
            }
        }
    }
    return map
}

private fun countDays(templates: List<CycleTemplate>, from: LocalDate, to: LocalDate): Int {
    val set = mutableSetOf<LocalDate>()
    templates.forEach { t ->
        CycleCalculator.expandCycleToDays(t, from, to).forEach { e ->
            var d = e.start
            while (!d.isAfter(e.end)) {
                if (!d.isBefore(from) && !d.isAfter(to)) set.add(d)
                d = d.plusDays(1)
            }
        }
    }
    return set.size
}

private fun toHex(color: Long): String = "#%06X".format(color and 0xFFFFFF)

private fun savePdf(context: Context, fileName: String, doc: PdfDocument): Uri? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CalendareVaht")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            uri
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CalendareVaht")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { doc.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    } catch (e: Exception) { e.printStackTrace(); null }
}

private fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить PDF"))
}