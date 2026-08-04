package com.shadow.rat.core;

import android.os.Handler;
import android.os.Looper;
import com.shadow.rat.utils.Logger;
import com.shadow.rat.utils.ThreadPoolManager;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandQueue {
    private static final String TAG = "CommandQueue";
    private final BlockingQueue<QueuedCommand> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static final int RATE_LIMIT_MS = 100; // Max 10 commands per second
    private long lastExecutionTime = 0;
    
    public enum Priority {
        CRITICAL(0), HIGH(1), NORMAL(2), LOW(3), BACKGROUND(4);
        final int value;
        Priority(int value) { this.value = value; }
    }
    
    private static class QueuedCommand {
        final String command;
        final Priority priority;
        final CommandProcessor processor;
        final long timestamp;
        
        QueuedCommand(String command, Priority priority, CommandProcessor processor) {
            this.command = command;
            this.priority = priority;
            this.processor = processor;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    @FunctionalInterface
    public interface CommandProcessor {
        void process(String command);
    }
    
    public CommandQueue() {
        // Start processing thread with priority-based ordering
        ThreadPoolManager.getExecutorService().execute(this::processLoop);
    }
    
    public void enqueue(String command, Priority priority, CommandProcessor processor) {
        try {
            queue.offer(new QueuedCommand(command, priority, processor));
            Logger.d(TAG, "Enqueued command: " + command + " (priority: " + priority + ")");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to enqueue command", e);
        }
    }
    
    private void processLoop() {
        while (running.get()) {
            try {
                // Rate limiting
                long now = System.currentTimeMillis();
                long waitTime = RATE_LIMIT_MS - (now - lastExecutionTime);
                if (waitTime > 0) {
                    Thread.sleep(waitTime);
                }
                
                // Get highest priority command
                QueuedCommand cmd = queue.poll(1, TimeUnit.SECONDS);
                if (cmd != null) {
                    executeCommand(cmd);
                    lastExecutionTime = System.currentTimeMillis();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Logger.e(TAG, "Queue processing error", e);
            }
        }
    }
    
    private void executeCommand(QueuedCommand cmd) {
        mainHandler.post(() -> {
            try {
                Logger.d(TAG, "Executing command: " + cmd.command);
                cmd.processor.process(cmd.command);
            } catch (Exception e) {
                Logger.e(TAG, "Command execution failed", e);
            }
        });
    }
    
    public void shutdown() {
        running.set(false);
        queue.clear();
    }
    
    public int getPendingCount() {
        return queue.size();
    }
}