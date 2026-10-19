package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.calendarevaht.domain.Brigade
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
    private val brigadesKey = stringPreferencesKey("brigades")

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

    // === БРИГАДЫ ===
    val brigades: Flow<List<Brigade>> = context.dataStore.data.map { prefs ->
        val json = prefs[brigadesKey] ?: return@map emptyList()
        parseBrigades(json)
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

    suspend fun saveBrigades(list: List<Brigade>) {
        context.dataStore.edit { prefs ->
            prefs[brigadesKey] = serializeBrigades(list)
        }
    }

    // === МИГРАЦИЯ ===
    suspend fun migrateIfNeeded() {
        val currentTemplates = templates.first()
        if (currentTemplates.any { it.brigadeId == 0L }) {
            val defaultPerson = Person(id = 1L, fullName = "Сотрудник 1", position = "", isActive = true)
            val defaultBrigade = Brigade(id = 1L, name = "Мой состав", personIds = listOf(1L))

            val migratedTemplates = currentTemplates.map { t ->
                if (t.brigadeId == 0L) {
                    t.copy(brigadeId = 1L, brigadeName = "Мой состав")
                } else t
            }

            savePersons(listOf(defaultPerson))
            saveBrigades(listOf(defaultBrigade))
            saveTemplates(migratedTemplates)
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
                    brigadeId = obj.optLong("brigadeId", 0L),
                    brigadeName = obj.optString("brigadeName", "Мой состав")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeTemplates(list: List<CycleTemplate>): String {
        val array = JSONArray()
        list.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
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
            obj.put("brigadeId", t.brigadeId)
            obj.put("brigadeName", t.brigadeName)
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
                    isActive = obj.optBoolean("isActive", true)
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
                put("isActive", p.isActive)
            })
        }
        return array.toString()
    }

    private fun parseBrigades(json: String): List<Brigade> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val idsArray = obj.getJSONArray("personIds")
                val ids = (0 until idsArray.length()).map { idsArray.getLong(it) }
                Brigade(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    personIds = ids
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeBrigades(list: List<Brigade>): String {
        val array = JSONArray()
        list.forEach { b ->
            array.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("personIds", JSONArray(b.personIds))
            })
        }
        return array.toString()
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
}