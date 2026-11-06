package com.ontime.app.model

import org.json.JSONArray
import org.json.JSONObject

data class AlarmItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarme",
    val isEnabled: Boolean = true,
    val vibrate: Boolean = true,
    val isPuzzle: Boolean = false,
    val customAudioUri: String? = null,
    val customAudioName: String? = null,
    val daysOfWeek: Set<Int> = emptySet() // 1=Segunda ... 7=Domingo (vazio = apenas próxima ocorrência)
) {
    fun formattedTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("hour", hour)
        obj.put("minute", minute)
        obj.put("label", label)
        obj.put("isEnabled", isEnabled)
        obj.put("vibrate", vibrate)
        obj.put("isPuzzle", isPuzzle)
        obj.put("customAudioUri", customAudioUri ?: "")
        obj.put("customAudioName", customAudioName ?: "")
        val daysArray = JSONArray()
        daysOfWeek.forEach { daysArray.put(it) }
        obj.put("daysOfWeek", daysArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): AlarmItem {
            val daysSet = mutableSetOf<Int>()
            if (obj.has("daysOfWeek")) {
                val arr = obj.getJSONArray("daysOfWeek")
                for (i in 0 until arr.length()) {
                    daysSet.add(arr.getInt(i))
                }
            }
            val uriStr = obj.optString("customAudioUri", "")
            val nameStr = obj.optString("customAudioName", "")
            return AlarmItem(
                id = obj.getString("id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                label = obj.optString("label", "Alarme"),
                isEnabled = obj.optBoolean("isEnabled", true),
                vibrate = obj.optBoolean("vibrate", true),
                isPuzzle = obj.optBoolean("isPuzzle", false),
                customAudioUri = if (uriStr.isNotBlank()) uriStr else null,
                customAudioName = if (nameStr.isNotBlank()) nameStr else null,
                daysOfWeek = daysSet
            )
        }
    }
}

