package com.shadow.rat.utils;

import android.util.Log;

public class Logger {
    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
    
    public static void e(String tag, String message) {
        Log.e(tag, message);
    }
}
