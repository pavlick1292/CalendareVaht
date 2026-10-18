package com.example.calendarevaht.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.calendarevaht.domain.CycleCalculator
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.ShiftEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 28f

private val RED = Color.rgb(229, 57, 53)
private val GRAY = Color.rgb(238, 238, 238)

fun exportReport(
    context: Context,
    year: Int,
    templates: List<CycleTemplate>,
    holidays: Map<LocalDate, String>,
    shiftHours: Int
) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            buildPdf(context, year, templates, holidays, shiftHours, withStats = true)
        }.onFailure {
            it.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun exportCalendarOnly(
    context: Context,
    year: Int,
    templates: List<CycleTemplate>,
    holidays: Map<LocalDate, String>
) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            buildPdf(context, year, templates, holidays, 0, withStats = false)
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
    holidays: Map<LocalDate, String>,
    shiftHours: Int,
    withStats: Boolean
) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var cursorY = MARGIN

    if (withStats) {
        paint.color = BLACK
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Календарь смен — $year", MARGIN, cursorY + 22f, paint)
        cursorY += 34f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        val mine = templates.firstOrNull { it.isMine }
        canvas.drawText("Мой состав: ${mine?.let { describe(it) } ?: "не задан"}", MARGIN, cursorY + 12f, paint)
        cursorY += 22f

        val stats = computeStats(year, templates, holidays, shiftHours)
        val lines = listOf(
            "Дней на вахте: ${stats.workDays}",
            "Отработано часов: ${stats.hours}",
            "Выходных на вахте: ${stats.weekendWorkDays}",
            "Праздников на вахте: ${stats.holidayWorkDays}",
            "Дней дома: ${stats.homeDays}"
        )
        lines.forEach {
            canvas.drawText(it, MARGIN, cursorY + 12f, paint)
            cursorY += 18f
        }
        cursorY += 10f
    }

    val mineDays = expand(templates.filter { it.isMine }, year)
    val allDays = expand(templates, year)

    if (withStats) {
        cursorY = drawMonthGrid(canvas, paint, year, mineDays, holidays, cursorY, title = "Мой состав")
        if (templates.any { !it.isMine }) {
            cursorY = drawMonthGrid(canvas, paint, year, allDays, holidays, cursorY, title = "Совмещённый")
        }
    } else {
        cursorY = drawMonthGrid(canvas, paint, year, allDays, holidays, cursorY, title = null)
    }

    doc.finishPage(page)

    val fileName = "CalendareVaht_${year}_${if (withStats) "report" else "calendar"}.pdf"
    val uri = savePdf(context, fileName, doc)
    doc.close()
    if (uri != null) {
        CoroutineScope(Dispatchers.Main).launch {
            sharePdf(context, uri)
        }
    }
}

private fun drawMonthGrid(
    canvas: android.graphics.Canvas,
    paint: Paint,
    year: Int,
    days: Map<LocalDate, List<ShiftEntry>>,
    holidays: Map<LocalDate, String>,
    startY: Float,
    title: String?
): Float {
    var y = startY
    if (title != null) {
        paint.color = BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText(title, MARGIN, y + 14f, paint)
        y += 22f
        paint.isFakeBoldText = false
    }

    val gapX = 10f
    val gapY = 10f
    val colW = (PAGE_W - 2 * MARGIN - 2 * gapX) / 3f
    val cell = 8f
    val monthHeaderH = 14f
    val gridH = 7 * cell + monthHeaderH + 6f
    val rowH = gridH + gapY

    for (row in 0 until 4) {
        for (col in 0 until 3) {
            val monthIndex = row * 3 + col + 1
            val ym = YearMonth.of(year, monthIndex)
            val x0 = MARGIN + col * (colW + gapX)
            val y0 = y + row * rowH

            paint.color = BLACK
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText(
                ym.month.getDisplayName(TextStyle.FULL, Locale("ru")).replaceFirstChar { it.uppercase() },
                x0, y0 + 9f, paint
            )
            paint.isFakeBoldText = false

            val gridTop = y0 + monthHeaderH
            val first = ym.atDay(1)
            val leading = (first.dayOfWeek.value + 6) % 7
            val total = ym.lengthOfMonth()

            for (d in 1..total) {
                val idx = leading + d - 1
                val cx = x0 + (idx % 7) * cell
                val cy = gridTop + (idx / 7) * cell
                val date = ym.atDay(d)
                val isHoliday = holidays.containsKey(date)
                val isWeekend = date.dayOfWeek.value >= 6
                val dayShifts = days[date].orEmpty()
                val color = when {
                    isHoliday -> RED
                    isWeekend -> RED
                    dayShifts.isEmpty() -> GRAY
                    else -> android.graphics.Color.parseColor(toHex(dayShifts.first().color))
                }
                paint.color = color
                canvas.drawRect(cx, cy, cx + cell - 0.8f, cy + cell - 0.8f, paint)
            }
        }
    }
    return y + 4 * rowH + 10f
}

private fun expand(
    templates: List<CycleTemplate>,
    year: Int
): Map<LocalDate, List<ShiftEntry>> {
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

private data class Stats(
    val workDays: Int,
    val hours: Int,
    val weekendWorkDays: Int,
    val holidayWorkDays: Int,
    val homeDays: Int
)

private fun computeStats(
    year: Int,
    templates: List<CycleTemplate>,
    holidays: Map<LocalDate, String>,
    shiftHours: Int
): Stats {
    val mine = templates.filter { it.isMine }
    val days = expand(mine, year)
    val workDays = days.size
    val hours = workDays * shiftHours
    val weekendWorkDays = days.keys.count { it.dayOfWeek.value >= 6 }
    val holidayWorkDays = days.keys.count { holidays.containsKey(it) }
    val homeDays = (if (year % 4 == 0) 366 else 365) - workDays
    return Stats(workDays, hours, weekendWorkDays, holidayWorkDays, homeDays)
}

private fun describe(t: CycleTemplate): String {
    val r = t.initialRule
    return "${r.workDays}/${r.restDays} с ${t.startDate}"
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
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить PDF"))
}