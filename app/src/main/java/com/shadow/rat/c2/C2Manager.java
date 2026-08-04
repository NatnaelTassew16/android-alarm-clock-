package com.shadow.rat.c2;

import com.shadow.rat.modules.Module;

public interface C2Manager {
    void startListening(CommandCallback callback);
    void registerModule(String command, Module module);
    void sendMessage(String message);

    interface CommandCallback {
        void onCommandReceived(String command);
    }
}
