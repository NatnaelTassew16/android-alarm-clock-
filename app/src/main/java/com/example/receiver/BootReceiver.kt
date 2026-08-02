package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "Device rebooted or app updated. Restoring alarms...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val repo = AlarmRepository(db.alarmDao())
                    val scheduler = AlarmScheduler(context)

                    val enabledAlarms = repo.getEnabledAlarms()
                    for (alarm in enabledAlarms) {
                        scheduler.schedule(alarm)
                    }
                    Log.d("BootReceiver", "Successfully restored ${enabledAlarms.size} alarms.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restore alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
