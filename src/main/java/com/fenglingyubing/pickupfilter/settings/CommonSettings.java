package com.fenglingyubing.pickupfilter.settings;

import com.fenglingyubing.pickupfilter.event.DropClearArea;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class CommonSettings {
    private static final String KEY_CLEAR_DROPS_CHUNK_RADIUS = "clear_drops.chunk_radius";
    private static final int CLEAR_DROPS_CHUNK_RADIUS_MIN = 0;
    private static final int CLEAR_DROPS_CHUNK_RADIUS_MAX = 16;

    private final File configFile;
    private int clearDropsChunkRadius = DropClearArea.DEFAULT_CHUNK_RADIUS;

    public CommonSettings(File configFile) {
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

        clearDropsChunkRadius = clampInt(
                parseInt(properties.getProperty(KEY_CLEAR_DROPS_CHUNK_RADIUS, Integer.toString(DropClearArea.DEFAULT_CHUNK_RADIUS)),
                        DropClearArea.DEFAULT_CHUNK_RADIUS),
                CLEAR_DROPS_CHUNK_RADIUS_MIN,
                CLEAR_DROPS_CHUNK_RADIUS_MAX
        );

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
        out.setProperty(KEY_CLEAR_DROPS_CHUNK_RADIUS, Integer.toString(clearDropsChunkRadius));
        try (FileOutputStream outputStream = new FileOutputStream(configFile)) {
            out.store(outputStream, "PickupFilter common settings");
        } catch (Exception ignored) {
        }
    }

    public synchronized int getClearDropsChunkRadius() {
        return clearDropsChunkRadius;
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

