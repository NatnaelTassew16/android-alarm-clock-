package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val repeatDays: List<Int> = emptyList(), // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val vibrate: Boolean = true,
    val ringtoneUri: String = "default",
    val ringtoneName: String = "Default Alarm Tone",
    val snoozeDurationMinutes: Int = 10,
    val gradualVolume: Boolean = true,
    val volume: Float = 0.9f,
    val isSnoozed: Boolean = false,
    val snoozeUntilMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Format time as HH:MM with AM/PM or 24h format
     */
    fun getFormattedTime(is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            String.format("%02d:%02d", h, minute)
        }
    }

    fun getAmPm(): String {
        return if (hour >= 12) "PM" else "AM"
    }

    /**
     * Get textual description of repeat days (e.g. "Every day", "Weekdays", "Mon, Wed, Fri", "Once")
     */
    fun getRepeatDaysText(): String {
        if (repeatDays.isEmpty()) return "Once"
        val sorted = repeatDays.sorted()
        if (sorted == listOf(1, 2, 3, 4, 5, 6, 7)) return "Every day"
        if (sorted == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (sorted == listOf(6, 7)) return "Weekends"

        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return sorted.joinToString(", ") { dayNames[it] ?: "" }
    }

    /**
     * Calculates the next trigger timestamp in milliseconds from now.
     */
    fun calculateNextTriggerMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        if (isSnoozed && snoozeUntilMillis > nowMillis) {
            return snoozeUntilMillis
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeatDays.isEmpty()) {
            // One-time alarm
            if (cal.timeInMillis <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        // Converting Calendar.DAY_OF_WEEK to 1=Mon .. 7=Sun convention
        // Calendar: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
        fun calendarDayToCustom(calDay: Int): Int {
            return if (calDay == Calendar.SUNDAY) 7 else calDay - 1
        }

        val currentCustomDay = calendarDayToCustom(cal.get(Calendar.DAY_OF_WEEK))

        // Check if today matches repeat days and the time hasn't passed
        if (repeatDays.contains(currentCustomDay) && cal.timeInMillis > nowMillis) {
            return cal.timeInMillis
        }

        // Find the next upcoming day in repeatDays
        for (i in 1..7) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val customDay = calendarDayToCustom(cal.get(Calendar.DAY_OF_WEEK))
            if (repeatDays.contains(customDay)) {
                return cal.timeInMillis
            }
        }

        return cal.timeInMillis
    }
}
