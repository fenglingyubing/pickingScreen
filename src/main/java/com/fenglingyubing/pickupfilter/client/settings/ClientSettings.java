package com.fenglingyubing.pickupfilter.client.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ClientSettings {
    private static final String KEY_INTRO_SHOWN = "intro.shown";
    private static final String KEY_INVENTORY_BUTTON_OFFSET_X = "inventory.button.offset_x";
    private static final String KEY_INVENTORY_BUTTON_OFFSET_Y = "inventory.button.offset_y";
    private static final int INVENTORY_BUTTON_OFFSET_MIN = -80;
    private static final int INVENTORY_BUTTON_OFFSET_MAX = 80;

    private final File configFile;
    private boolean introShown;
    private int inventoryButtonOffsetX;
    private int inventoryButtonOffsetY;

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
        inventoryButtonOffsetX = clampInt(parseInt(properties.getProperty(KEY_INVENTORY_BUTTON_OFFSET_X, "0"), 0),
                INVENTORY_BUTTON_OFFSET_MIN, INVENTORY_BUTTON_OFFSET_MAX);
        inventoryButtonOffsetY = clampInt(parseInt(properties.getProperty(KEY_INVENTORY_BUTTON_OFFSET_Y, "0"), 0),
                INVENTORY_BUTTON_OFFSET_MIN, INVENTORY_BUTTON_OFFSET_MAX);
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
        out.setProperty(KEY_INVENTORY_BUTTON_OFFSET_X, Integer.toString(inventoryButtonOffsetX));
        out.setProperty(KEY_INVENTORY_BUTTON_OFFSET_Y, Integer.toString(inventoryButtonOffsetY));
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

    public synchronized int getInventoryButtonOffsetX() {
        return inventoryButtonOffsetX;
    }

    public synchronized int getInventoryButtonOffsetY() {
        return inventoryButtonOffsetY;
    }

    public synchronized void adjustInventoryButtonOffset(int dx, int dy) {
        inventoryButtonOffsetX = clampInt(inventoryButtonOffsetX + dx, INVENTORY_BUTTON_OFFSET_MIN, INVENTORY_BUTTON_OFFSET_MAX);
        inventoryButtonOffsetY = clampInt(inventoryButtonOffsetY + dy, INVENTORY_BUTTON_OFFSET_MIN, INVENTORY_BUTTON_OFFSET_MAX);
        save();
    }

    public synchronized void resetInventoryButtonOffset() {
        inventoryButtonOffsetX = 0;
        inventoryButtonOffsetY = 0;
        save();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
