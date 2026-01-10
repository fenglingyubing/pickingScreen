package com.fenglingyubing.pickupfilter.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigManager {
    public enum FilterMode {
        DESTROY_MATCHING,
        PICKUP_MATCHING,
        DISABLED
    }

    private final Configuration configuration;
    private FilterMode currentMode = FilterMode.DISABLED;

    public ConfigManager(File configFile) {
        this.configuration = new Configuration(configFile);
    }

    public void load() {
        configuration.load();
        String modeValue = configuration.getString(
                "mode",
                "general",
                currentMode.name(),
                "Current filter mode: DESTROY_MATCHING, PICKUP_MATCHING, DISABLED"
        );

        try {
            currentMode = FilterMode.valueOf(modeValue);
        } catch (IllegalArgumentException ignored) {
            currentMode = FilterMode.DISABLED;
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public void save() {
        configuration.save();
    }

    public FilterMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(FilterMode mode) {
        this.currentMode = mode == null ? FilterMode.DISABLED : mode;
        configuration.get("general", "mode", FilterMode.DISABLED.name()).set(this.currentMode.name());
        save();
    }
}

