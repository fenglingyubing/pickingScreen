package com.fenglingyubing.pickupfilter.client.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ClientSettings {
    private static final String KEY_INTRO_SHOWN = "intro.shown";

    private final File configFile;
    private boolean introShown;

    public ClientSettings(File configFile) {
        this.configFile = configFile;
    }

    public synchronized void load() {
        if (configFile == null) {
            return;
        }

        Properties properties = new Properties();
        if (configFile.exists() && configFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                properties.load(inputStream);
            } catch (Exception ignored) {
            }
        }

        introShown = Boolean.parseBoolean(properties.getProperty(KEY_INTRO_SHOWN, "false"));
        save();
    }

    public synchronized void save() {
        if (configFile == null) {
            return;
        }

        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }

        Properties out = new Properties();
        out.setProperty(KEY_INTRO_SHOWN, Boolean.toString(introShown));
        try (FileOutputStream outputStream = new FileOutputStream(configFile)) {
            out.store(outputStream, "PickupFilter client settings");
        } catch (Exception ignored) {
        }
    }

    public synchronized boolean isIntroShown() {
        return introShown;
    }

    public synchronized void markIntroShown() {
        introShown = true;
        save();
    }
}

