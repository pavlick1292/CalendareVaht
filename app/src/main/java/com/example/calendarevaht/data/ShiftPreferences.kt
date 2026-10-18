package com.example.calendarevaht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.calendarevaht.domain.CycleRule
import com.example.calendarevaht.domain.CycleTemplate
import com.example.calendarevaht.domain.ShiftOverride
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ShiftPreferences(private val context: Context) {
    private val key = stringPreferencesKey("cycle_templates")

    val templates = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList<CycleTemplate>()
        runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    suspend fun save(list: List<CycleTemplate>) {
        context.dataStore.edit { it[key] = serialize(list) }
    }

    private fun serialize(list: List<CycleTemplate>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("workDays", t.initialRule.workDays)
            obj.put("restDays", t.initialRule.restDays)
            obj.put("startDate", t.startDate.toString())
            obj.put("endDate", t.endDate?.toString() ?: JSONObject.NULL)
            obj.put("isOneTime", t.isOneTime)
            obj.put("baseColor", t.baseColor)
            obj.put("countBackward", t.countBackward)
            obj.put("backwardLimitMonths", t.backwardLimitMonths)
            obj.put("isMine", t.isMine)
            obj.put("customName", t.customName)

            // Сохраняем overrides (измененные вахты)
            val overridesArr = JSONArray()
            t.overrides.forEach { ov ->
                val oo = JSONObject()
                oo.put("originalStart", ov.originalStart.toString())
                oo.put("newStart", ov.newStart.toString())
                oo.put("workDays", ov.workDays)
                oo.put("restDays", ov.restDays)
                oo.put("shiftSubsequent", ov.shiftSubsequent)
                overridesArr.put(oo)
            }
            obj.put("overrides", overridesArr)

            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parse(raw: String): List<CycleTemplate> {
        val arr = JSONArray(raw)
        val result = mutableListOf<CycleTemplate>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            // Читаем overrides
            val overridesArr = obj.optJSONArray("overrides") ?: JSONArray()
            val overrides = mutableListOf<ShiftOverride>()
            for (j in 0 until overridesArr.length()) {
                val oo = overridesArr.getJSONObject(j)
                runCatching {
                    overrides.add(
                        ShiftOverride(
                            originalStart = LocalDate.parse(oo.getString("originalStart")),
                            newStart = LocalDate.parse(oo.getString("newStart")),
                            workDays = oo.getInt("workDays"),
                            restDays = oo.getInt("restDays"),
                            shiftSubsequent = oo.optBoolean("shiftSubsequent", false)
                        )
                    )
                }
            }

            val endRaw = obj.opt("endDate")
            val endDate = if (endRaw == null || endRaw == JSONObject.NULL) null
            else runCatching { LocalDate.parse(endRaw.toString()) }.getOrNull()

            result.add(
                CycleTemplate(
                    id = obj.getLong("id"),
                    initialRule = CycleRule(
                        workDays = obj.getInt("workDays"),
                        restDays = obj.getInt("restDays")
                    ),
                    startDate = LocalDate.parse(obj.getString("startDate")),
                    endDate = endDate,
                    isOneTime = obj.getBoolean("isOneTime"),
                    baseColor = obj.getLong("baseColor"),
                    overrides = overrides.sortedBy { it.originalStart },
                    countBackward = obj.optBoolean("countBackward", false),
                    backwardLimitMonths = obj.optInt("backwardLimitMonths", 0),
                    isMine = obj.optBoolean("isMine", false),
                    customName = obj.optString("customName", "")
                )
            )
        }
        return result
    }
}