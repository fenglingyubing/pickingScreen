package com.fenglingyubing.pickupfilter.settings;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import com.fenglingyubing.pickupfilter.event.DropClearArea;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class CommonSettings {
    private static final String KEY_CLEAR_DROPS_CHUNK_RADIUS = "clear_drops.chunk_radius";
    private static final int CLEAR_DROPS_CHUNK_RADIUS_MIN = 0;
    private static final int CLEAR_DROPS_CHUNK_RADIUS_MAX = 16;

    private static final String KEY_AUTO_DESTROY_SCAN_MIN_INTERVAL_TICKS = "auto_destroy.scan_interval.min_ticks";
    private static final String KEY_AUTO_DESTROY_SCAN_MAX_INTERVAL_TICKS = "auto_destroy.scan_interval.max_ticks";
    private static final String KEY_AUTO_DESTROY_SCAN_MAX_ENTITIES = "auto_destroy.scan.max_entities";
    private static final String KEY_AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD = "auto_destroy.scan_backoff.empty_miss_threshold";

    private static final int AUTO_DESTROY_SCAN_INTERVAL_TICKS_MIN = 1;
    private static final int AUTO_DESTROY_SCAN_INTERVAL_TICKS_MAX = 200;
    private static final int AUTO_DESTROY_SCAN_MAX_ENTITIES_MIN = 0;
    private static final int AUTO_DESTROY_SCAN_MAX_ENTITIES_MAX = 10000;
    private static final int AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD_MIN = 1;
    private static final int AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD_MAX = 20;

    private final File configFile;
    private volatile int clearDropsChunkRadius = DropClearArea.DEFAULT_CHUNK_RADIUS;
    private volatile int autoDestroyScanMinIntervalTicks = 5;
    private volatile int autoDestroyScanMaxIntervalTicks = 5;
    private volatile int autoDestroyScanMaxEntities = 0;
    private volatile int autoDestroyEmptyBackoffMissThreshold = 2;

    public CommonSettings(File configFile) {
        this.configFile = configFile;
    }

    public synchronized void load() {
        if (configFile == null) {
            return;
        }

        Properties properties = new Properties();
        boolean loadedFromDisk = false;
        if (configFile.exists() && configFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                properties.load(inputStream);
                loadedFromDisk = true;
            } catch (Exception e) {
                PickupFilterCommon.LOGGER.warn("Failed to load common settings file: {}", configFile, e);
                return;
            }
        }

        clearDropsChunkRadius = clampInt(
                parseInt(properties.getProperty(KEY_CLEAR_DROPS_CHUNK_RADIUS, Integer.toString(DropClearArea.DEFAULT_CHUNK_RADIUS)),
                        DropClearArea.DEFAULT_CHUNK_RADIUS),
                CLEAR_DROPS_CHUNK_RADIUS_MIN,
                CLEAR_DROPS_CHUNK_RADIUS_MAX
        );

        autoDestroyScanMinIntervalTicks = clampInt(
                parseInt(properties.getProperty(KEY_AUTO_DESTROY_SCAN_MIN_INTERVAL_TICKS, Integer.toString(autoDestroyScanMinIntervalTicks)),
                        autoDestroyScanMinIntervalTicks),
                AUTO_DESTROY_SCAN_INTERVAL_TICKS_MIN,
                AUTO_DESTROY_SCAN_INTERVAL_TICKS_MAX
        );
        autoDestroyScanMaxIntervalTicks = clampInt(
                parseInt(properties.getProperty(KEY_AUTO_DESTROY_SCAN_MAX_INTERVAL_TICKS, Integer.toString(autoDestroyScanMaxIntervalTicks)),
                        autoDestroyScanMaxIntervalTicks),
                autoDestroyScanMinIntervalTicks,
                AUTO_DESTROY_SCAN_INTERVAL_TICKS_MAX
        );
        autoDestroyScanMaxEntities = clampInt(
                parseInt(properties.getProperty(KEY_AUTO_DESTROY_SCAN_MAX_ENTITIES, Integer.toString(autoDestroyScanMaxEntities)),
                        autoDestroyScanMaxEntities),
                AUTO_DESTROY_SCAN_MAX_ENTITIES_MIN,
                AUTO_DESTROY_SCAN_MAX_ENTITIES_MAX
        );
        autoDestroyEmptyBackoffMissThreshold = clampInt(
                parseInt(properties.getProperty(KEY_AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD, Integer.toString(autoDestroyEmptyBackoffMissThreshold)),
                        autoDestroyEmptyBackoffMissThreshold),
                AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD_MIN,
                AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD_MAX
        );

        if (!configFile.exists() || loadedFromDisk) {
            save();
        }
    }

    public synchronized void save() {
        if (configFile == null) {
            return;
        }

        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            PickupFilterCommon.LOGGER.warn("Failed to create settings directory: {}", parent);
            return;
        }

        Properties out = new Properties();
        out.setProperty(KEY_CLEAR_DROPS_CHUNK_RADIUS, Integer.toString(clearDropsChunkRadius));
        out.setProperty(KEY_AUTO_DESTROY_SCAN_MIN_INTERVAL_TICKS, Integer.toString(autoDestroyScanMinIntervalTicks));
        out.setProperty(KEY_AUTO_DESTROY_SCAN_MAX_INTERVAL_TICKS, Integer.toString(autoDestroyScanMaxIntervalTicks));
        out.setProperty(KEY_AUTO_DESTROY_SCAN_MAX_ENTITIES, Integer.toString(autoDestroyScanMaxEntities));
        out.setProperty(KEY_AUTO_DESTROY_EMPTY_BACKOFF_MISS_THRESHOLD, Integer.toString(autoDestroyEmptyBackoffMissThreshold));
        try (FileOutputStream outputStream = new FileOutputStream(configFile)) {
            out.store(outputStream, "PickupFilter common settings");
        } catch (Exception e) {
            PickupFilterCommon.LOGGER.warn("Failed to save common settings file: {}", configFile, e);
        }
    }

    public int getClearDropsChunkRadius() {
        return clearDropsChunkRadius;
    }

    public synchronized void setClearDropsChunkRadius(int chunkRadius) {
        clearDropsChunkRadius = clampInt(chunkRadius, CLEAR_DROPS_CHUNK_RADIUS_MIN, CLEAR_DROPS_CHUNK_RADIUS_MAX);
        save();
    }

    public synchronized void resetClearDropsChunkRadius() {
        clearDropsChunkRadius = DropClearArea.DEFAULT_CHUNK_RADIUS;
        save();
    }

    public int getAutoDestroyScanMinIntervalTicks() {
        return autoDestroyScanMinIntervalTicks;
    }

    public int getAutoDestroyScanMaxIntervalTicks() {
        return autoDestroyScanMaxIntervalTicks;
    }

    public int getAutoDestroyScanMaxEntities() {
        return autoDestroyScanMaxEntities;
    }

    public int getAutoDestroyEmptyBackoffMissThreshold() {
        return autoDestroyEmptyBackoffMissThreshold;
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
