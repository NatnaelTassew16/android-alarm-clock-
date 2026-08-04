package com.shadow.rat.core;

import android.util.Log;
import com.shadow.rat.utils.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRouter {
    private static final String TAG = "CommandRouter";
    private final Map<String, CommandHandler> handlers = new ConcurrentHashMap<>();
    
    @FunctionalInterface
    public interface CommandHandler {
        void execute(String args) throws Exception;
    }
    
    public void register(String command, CommandHandler handler) {
        handlers.put(command.toLowerCase(), handler);
        Logger.d(TAG, "Registered command: " + command);
    }
    
    public void route(String fullCommand) throws Exception {
        if (fullCommand == null || fullCommand.trim().isEmpty()) {
            return;
        }
        
        String[] parts = fullCommand.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        CommandHandler handler = handlers.get(command);
        if (handler != null) {
            Logger.d(TAG, "Routing command: " + command);
            handler.execute(args);
        } else {
            Logger.w(TAG, "Unknown command: " + command);
            throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
}