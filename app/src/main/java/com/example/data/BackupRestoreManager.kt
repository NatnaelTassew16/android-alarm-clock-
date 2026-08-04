package com.example.data

import com.example.data.AlarmEntity
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {

    fun exportAlarmsToJson(alarms: List<AlarmEntity>): String {
        val jsonArray = JSONArray()
        for (alarm in alarms) {
            val jsonObject = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("label", alarm.label)
                put("isEnabled", alarm.isEnabled)
                put("repeatDays", JSONArray(alarm.repeatDays))
                put("vibrate", alarm.vibrate)
                put("ringtoneUri", alarm.ringtoneUri)
                put("ringtoneName", alarm.ringtoneName)
                put("snoozeDurationMinutes", alarm.snoozeDurationMinutes)
                put("gradualVolume", alarm.gradualVolume)
                put("volume", alarm.volume.toDouble())
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString(2)
    }

    fun importAlarmsFromJson(jsonString: String): List<AlarmEntity> {
        val alarms = mutableListOf<AlarmEntity>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val repeatDaysArray = obj.optJSONArray("repeatDays")
            val repeatDays = mutableListOf<Int>()
            if (repeatDaysArray != null) {
                for (j in 0 until repeatDaysArray.length()) {
                    repeatDays.add(repeatDaysArray.getInt(j))
                }
            }

            alarms.add(
                AlarmEntity(
                    id = 0L, // Reset ID so Room assigns new auto-generated IDs
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    label = obj.optString("label", "Alarm"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    repeatDays = repeatDays,
                    vibrate = obj.optBoolean("vibrate", true),
                    ringtoneUri = obj.optString("ringtoneUri", "default"),
                    ringtoneName = obj.optString("ringtoneName", "Default Alarm Tone"),
                    snoozeDurationMinutes = obj.optInt("snoozeDurationMinutes", 10),
                    gradualVolume = obj.optBoolean("gradualVolume", true),
                    volume = obj.optDouble("volume", 0.9).toFloat()
                )
            )
        }
        return alarms
    }
}
