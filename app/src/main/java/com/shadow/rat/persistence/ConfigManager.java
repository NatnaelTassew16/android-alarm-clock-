package com.shadow.rat.persistence;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {

    private static Properties properties;

    public static void loadConfig(Context context) throws IOException {
        if (properties == null) {
            properties = new Properties();
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
