package com.shadow.rat.exfiltration;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import com.shadow.rat.utils.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CompressionManager {
    private static final String TAG = "CompressionManager";
    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final int IMAGE_QUALITY = 80;
    private static final int MAX_VIDEO_BITRATE = 1000000; // 1 Mbps
    
    public static String compressFile(Context context, String filePath) {
        String mimeType = getMimeType(filePath);
        if (mimeType == null) {
            return filePath;
        }
        
        try {
            if (mimeType.startsWith("image/")) {
                return compressImage(context, filePath);
            } else if (mimeType.startsWith("video/")) {
                return compressVideo(context, filePath);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Compression failed", e);
        }
        return filePath;
    }
    
    private static String compressImage(Context context, String filePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            
            int width = options.outWidth;
            int height = options.outHeight;
            
            // Calculate scale factor
            int scale = 1;
            while (width / scale > MAX_IMAGE_DIMENSION || 
                   height / scale > MAX_IMAGE_DIMENSION) {
                scale *= 2;
            }
            
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;
            
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            
            String outputPath = context.getCacheDir().getAbsolutePath() 
                + "/compressed_" + System.currentTimeMillis() + ".jpg";
            
            FileOutputStream fos = new FileOutputStream(outputPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, fos);
            fos.close();
            bitmap.recycle();
            
            return outputPath;
            
        } catch (Exception e) {
            Logger.e(TAG, "Image compression failed", e);
            return filePath;
        }
    }
    
    private static String compressVideo(Context context, String filePath) {
        try {
            // Video compression using FFmpeg or other library would go here
            // For now, just return original
            return filePath;
        } catch (Exception e) {
            Logger.e(TAG, "Video compression failed", e);
            return filePath;
        }
    }
    
    private static String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "3gp":
                return "video/3gpp";
            default:
                return null;
        }
    }
}