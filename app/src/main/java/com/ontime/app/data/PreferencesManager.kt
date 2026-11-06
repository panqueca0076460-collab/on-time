package com.ontime.app.data

import android.content.Context
import android.content.SharedPreferences
import com.ontime.app.model.AlarmItem
import com.ontime.app.ui.theme.ThemePalette
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("on_time_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALARMS = "key_alarms"
        private const val KEY_VOLUME = "key_alarm_universal_volume"
        private const val KEY_THEME = "key_theme_palette"
    }

    // Volume Universal (0 a 100)
    var universalVolume: Int
        get() = prefs.getInt(KEY_VOLUME, 90)
        set(value) = prefs.edit().putInt(KEY_VOLUME, value.coerceIn(10, 100)).apply()

    // Tema escolhido
    var selectedTheme: ThemePalette
        get() {
            val name = prefs.getString(KEY_THEME, ThemePalette.LILAC.name) ?: ThemePalette.LILAC.name
            return try {
                ThemePalette.valueOf(name)
            } catch (e: Exception) {
                ThemePalette.LILAC
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    // Alarmes
    fun getAlarms(): List<AlarmItem> {
        val jsonStr = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val list = mutableListOf<AlarmItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(AlarmItem.fromJson(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveAlarms(alarms: List<AlarmItem>) {
        val array = JSONArray()
        alarms.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    fun getAlarmById(id: String): AlarmItem? {
        return getAlarms().find { it.id == id }
    }

    fun updateAlarm(alarm: AlarmItem) {
        val list = getAlarms().toMutableList()
        val idx = list.indexOfFirst { it.id == alarm.id }
        if (idx >= 0) {
            list[idx] = alarm
        } else {
            list.add(alarm)
        }
        saveAlarms(list)
    }

    fun deleteAlarm(id: String) {
        val list = getAlarms().toMutableList()
        list.removeAll { it.id == id }
        saveAlarms(list)
    }
}

