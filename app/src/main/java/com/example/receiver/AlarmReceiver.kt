package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.AlarmActivity
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.ACTION_ALARM_TRIGGER"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_RINGTONE_URI = "ringtone_uri"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_GRADUAL_VOLUME = "gradual_volume"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)

        Log.d("AlarmReceiver", "Received action: $action for alarmId: $alarmId")

        when (action) {
            ACTION_ALARM_TRIGGER -> {
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI) ?: "default"
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                val gradualVolume = intent.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true)
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.9f)
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

                // 1. Start Audio Service
                val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
                    this.action = AlarmAudioService.ACTION_START_RINGING
                    putExtra(AlarmAudioService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmAudioService.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmAudioService.EXTRA_RINGTONE_URI, ringtoneUri)
                    putExtra(AlarmAudioService.EXTRA_VIBRATE, vibrate)
                    putExtra(AlarmAudioService.EXTRA_GRADUAL_VOLUME, gradualVolume)
                    putExtra(AlarmAudioService.EXTRA_VOLUME, volume)
                    putExtra(AlarmAudioService.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Open Alarm Activity directly
                val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmActivity.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                }
                context.startActivity(activityIntent)
            }

            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                stopRingingService(context)
                snoozeAlarm(context, alarmId, snoozeMinutes)
            }

            ACTION_DISMISS -> {
                stopRingingService(context)
                dismissAlarm(context, alarmId)
            }
        }
    }

    private fun stopRingingService(context: Context) {
        val stopIntent = Intent(context, AlarmAudioService::class.java).apply {
            action = AlarmAudioService.ACTION_STOP_RINGING
        }
        context.startService(stopIntent)
    }

    private fun snoozeAlarm(context: Context, alarmId: Long, snoozeMinutes: Int) {
        if (alarmId == -1L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AlarmDatabase.getDatabase(context)
                val repo = AlarmRepository(db.alarmDao())
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    val snoozeUntil = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                    val updatedAlarm = alarm.copy(
                        isSnoozed = true,
                        snoozeUntilMillis = snoozeUntil
                    )
                    repo.updateAlarm(updatedAlarm)

                    val scheduler = AlarmScheduler(context)
                    scheduler.schedule(updatedAlarm)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error snoozing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun dismissAlarm(context: Context, alarmId: Long) {
        if (alarmId == -1L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AlarmDatabase.getDatabase(context)
                val repo = AlarmRepository(db.alarmDao())
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    val isOneTime = alarm.repeatDays.isEmpty()
                    val updatedAlarm = alarm.copy(
                        isEnabled = !isOneTime, // disable if one-time
                        isSnoozed = false,
                        snoozeUntilMillis = 0L
                    )
                    repo.updateAlarm(updatedAlarm)

                    val scheduler = AlarmScheduler(context)
                    if (updatedAlarm.isEnabled) {
                        scheduler.schedule(updatedAlarm)
                    } else {
                        scheduler.cancel(alarm)
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error dismissing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
