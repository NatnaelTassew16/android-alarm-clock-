package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val CHANNEL_ID = "alarm_ring_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_RINGING = "com.example.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.example.ACTION_STOP_RINGING"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_RINGTONE_URI = "ringtone_uri"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_GRADUAL_VOLUME = "gradual_volume"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING

        if (action == ACTION_STOP_RINGING) {
            stopRinging()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val ringtoneUriStr = intent?.getStringExtra(EXTRA_RINGTONE_URI) ?: "default"
        val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        val gradualVolume = intent?.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true) ?: true
        val targetVolume = intent?.getFloatExtra(EXTRA_VOLUME, 0.9f) ?: 0.9f
        val snoozeMinutes = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, 10) ?: 10

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        val pendingFullScreen = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm Ringing")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingFullScreen, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        startRinging(ringtoneUriStr, gradualVolume, targetVolume, vibrate)

        return START_STICKY
    }

    private fun startRinging(
        ringtoneUriStr: String,
        gradualVolume: Boolean,
        targetVolume: Float,
        vibrate: Boolean
    ) {
        stopRinging()

        try {
            val uri = if (ringtoneUriStr == "default" || ringtoneUriStr.isBlank()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(ringtoneUriStr)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
            }

            if (gradualVolume) {
                mediaPlayer?.setVolume(0.1f, 0.1f)
                mediaPlayer?.start()

                volumeJob = serviceScope.launch {
                    val steps = 15
                    val stepDelay = 2000L // 30 seconds total ramp up
                    val startVol = 0.1f
                    val volIncrement = (targetVolume - startVol) / steps

                    for (i in 1..steps) {
                        delay(stepDelay)
                        val currentVol = (startVol + i * volIncrement).coerceAtMost(targetVolume)
                        mediaPlayer?.setVolume(currentVol, currentVol)
                    }
                }
            } else {
                mediaPlayer?.setVolume(targetVolume, targetVolume)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("AlarmAudioService", "Error starting media player", e)
        }

        if (vibrate) {
            startVibration()
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmAudioService", "Error starting vibration", e)
        }
    }

    private fun stopRinging() {
        volumeJob?.cancel()
        volumeJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmAudioService", "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmAudioService", "Error stopping vibrator", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for active alarms"
                setBypassDnd(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}
