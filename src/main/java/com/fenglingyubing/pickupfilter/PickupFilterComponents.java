package com.fenglingyubing.pickupfilter;

import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.config.ConfigModeSwitching;
import com.fenglingyubing.pickupfilter.config.ModeSwitching;
import com.fenglingyubing.pickupfilter.event.CommonEventHandler;

import java.io.File;
import java.util.Objects;

public final class PickupFilterComponents {
    private final ConfigManager configManager;
    private final CommonEventHandler commonEventHandler;
    private final ModeSwitching modeSwitching;

    private PickupFilterComponents(
            ConfigManager configManager,
            CommonEventHandler commonEventHandler,
            ModeSwitching modeSwitching
    ) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.commonEventHandler = Objects.requireNonNull(commonEventHandler, "commonEventHandler");
        this.modeSwitching = Objects.requireNonNull(modeSwitching, "modeSwitching");
    }

    public static PickupFilterComponents bootstrap(File configFile) {
        ConfigManager configManager = new ConfigManager(configFile);
        configManager.load();

        ModeSwitching modeSwitching = new ConfigModeSwitching(configManager);
        CommonEventHandler commonEventHandler = new CommonEventHandler(configManager);
        return new PickupFilterComponents(configManager, commonEventHandler, modeSwitching);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommonEventHandler getCommonEventHandler() {
        return commonEventHandler;
    }

    public ModeSwitching getModeSwitching() {
        return modeSwitching;
    }
}
