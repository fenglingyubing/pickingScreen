package com.fenglingyubing.pickupfilter.config;

import java.util.Objects;
import java.util.function.Consumer;

public class ConfigModeSwitching implements ModeSwitching {
    private final ConfigManager configManager;
    private final Consumer<FilterMode> onModeChanged;

    public ConfigModeSwitching(ConfigManager configManager) {
        this(configManager, null);
    }

    public ConfigModeSwitching(ConfigManager configManager, Consumer<FilterMode> onModeChanged) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.onModeChanged = onModeChanged;
    }

    @Override
    public FilterMode getCurrentMode() {
        return configManager.getCurrentMode();
    }

    @Override
    public FilterMode switchTo(FilterMode mode) {
        FilterMode target = mode == null ? FilterMode.DISABLED : mode;
        configManager.setCurrentMode(target);
        configManager.saveConfig();
        if (onModeChanged != null) {
            onModeChanged.accept(target);
        }
        return target;
    }

    @Override
    public FilterMode cycleToNextMode() {
        return switchTo(FilterModeCycle.next(getCurrentMode()));
    }
}
