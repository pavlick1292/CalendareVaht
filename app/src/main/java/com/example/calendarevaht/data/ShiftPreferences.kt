package com.example.calendarevaht.data
import androidx.datastore.preferences.core.longPreferencesKey
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.Person
import com.example.calendarevaht.domain.ShiftOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ShiftPreferences(private val context: Context) {

    private val templatesKey = stringPreferencesKey("templates")
    private val personsKey = stringPreferencesKey("persons")
    private val calendarJsonKey = stringPreferencesKey("work_calendar_json")
    private val calendarUpdateKey = longPreferencesKey("work_calendar_update_time")
    // brigadesKey больше не используется, оставлен для очистки старого ключа
    private val brigadesKeyOld = stringPreferencesKey("brigades")

    // === ВАХТЫ ===
    val templates: Flow<List<CycleTemplate>> = context.dataStore.data.map { prefs ->
        val json = prefs[templatesKey] ?: return@map emptyList()
        parseTemplates(json)
    }

    // === ЛЮДИ ===
    val persons: Flow<List<Person>> = context.dataStore.data.map { prefs ->
        val json = prefs[personsKey] ?: return@map emptyList()
        parsePersons(json)
    }

    // СОХРАНЕНИЕ
    suspend fun saveTemplates(list: List<CycleTemplate>) {
        context.dataStore.edit { prefs ->
            prefs[templatesKey] = serializeTemplates(list)
        }
    }

    suspend fun savePersons(list: List<Person>) {
        context.dataStore.edit { prefs ->
            prefs[personsKey] = serializePersons(list)
        }
    }

    // === МИГРАЦИЯ ===
    suspend fun migrateIfNeeded() {
        val currentTemplates = templates.first()
        val currentPersons = persons.first()

        // Если у шаблонов есть старые поля brigadeId/brigadeName (признак старой версии),
        // переносим brigadeName в title, ставим assignedPersonIds пустым.
        val needsMigration = currentTemplates.any { t ->
            // В новой версии title не может быть пустым, если вахта существует
            // (старая версия хранила "Мой состав" по умолчанию)
            t.title.isBlank() || t.assignedPersonIds.isEmpty()
        } && currentTemplates.isNotEmpty()

        if (needsMigration) {
            val migrated = currentTemplates.map { t ->
                t.copy(
                    title = t.title.ifBlank { "Мой состав" },
                    assignedPersonIds = t.assignedPersonIds
                )
            }
            saveTemplates(migrated)
        }

        // Удаляем старый ключ бригад, если он есть
        context.dataStore.edit { prefs ->
            if (prefs.contains(brigadesKeyOld)) {
                prefs.remove(brigadesKeyOld)
            }
        }
    }

    // === СЕРИАЛИЗАЦИЯ (JSON) ===

    private fun parseTemplates(json: String): List<CycleTemplate> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CycleTemplate(
                    id = obj.getLong("id"),
                    title = obj.optString("title", obj.optString("brigadeName", "Мой состав")),
                    initialRule = CycleRule(
                        workDays = obj.getJSONObject("initialRule").getInt("workDays"),
                        restDays = obj.getJSONObject("initialRule").getInt("restDays")
                    ),
                    startDate = LocalDate.parse(obj.getString("startDate")),
                    endDate = if (obj.isNull("endDate")) null else LocalDate.parse(obj.getString("endDate")),
                    isOneTime = obj.getBoolean("isOneTime"),
                    baseColor = obj.getLong("baseColor"),
                    overrides = parseOverrides(obj.optJSONArray("overrides")),
                    countBackward = obj.optBoolean("countBackward", false),
                    backwardLimitMonths = obj.optInt("backwardLimitMonths", 0),
                    isMine = obj.getBoolean("isMine"),
                    assignedPersonIds = parseIds(obj.optJSONArray("assignedPersonIds"))
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeTemplates(list: List<CycleTemplate>): String {
        val array = JSONArray()
        list.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("initialRule", JSONObject().apply {
                put("workDays", t.initialRule.workDays)
                put("restDays", t.initialRule.restDays)
            })
            obj.put("startDate", t.startDate.toString())
            obj.put("endDate", t.endDate?.toString())
            obj.put("isOneTime", t.isOneTime)
            obj.put("baseColor", t.baseColor)
            obj.put("overrides", serializeOverrides(t.overrides))
            obj.put("countBackward", t.countBackward)
            obj.put("backwardLimitMonths", t.backwardLimitMonths)
            obj.put("isMine", t.isMine)
            obj.put("assignedPersonIds", JSONArray(t.assignedPersonIds))
            array.put(obj)
        }
        return array.toString()
    }

    private fun parsePersons(json: String): List<Person> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Person(
                    id = obj.getLong("id"),
                    fullName = obj.getString("fullName"),
                    position = obj.optString("position", ""),
                    isMe = obj.optBoolean("isMe", false)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializePersons(list: List<Person>): String {
        val array = JSONArray()
        list.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("fullName", p.fullName)
                put("position", p.position)
                put("isMe", p.isMe)
            })
        }
        return array.toString()
    }

    private fun parseIds(array: JSONArray?): List<Long> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getLong(it) }
    }

    private fun parseOverrides(array: JSONArray?): List<ShiftOverride> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ShiftOverride(
                originalStart = LocalDate.parse(obj.getString("originalStart")),
                newStart = LocalDate.parse(obj.getString("newStart")),
                workDays = obj.getInt("workDays"),
                restDays = obj.getInt("restDays"),
                shiftSubsequent = obj.getBoolean("shiftSubsequent")
            )
        }
    }

    private fun serializeOverrides(list: List<ShiftOverride>): JSONArray {
        val array = JSONArray()
        list.forEach { o ->
            array.put(JSONObject().apply {
                put("originalStart", o.originalStart.toString())
                put("newStart", o.newStart.toString())
                put("workDays", o.workDays)
                put("restDays", o.restDays)
                put("shiftSubsequent", o.shiftSubsequent)
            })
        }
        return array
    }

    // === ПРОИЗВОДСТВЕННЫЙ КАЛЕНДАРЬ ===
    val workCalendarJson: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[calendarJsonKey]
    }

    val lastCalendarUpdate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[calendarUpdateKey] ?: 0L
    }

    suspend fun saveWorkCalendar(json: String) {
        context.dataStore.edit { prefs ->
            prefs[calendarJsonKey] = json
            prefs[calendarUpdateKey] = System.currentTimeMillis()
        }
    }}