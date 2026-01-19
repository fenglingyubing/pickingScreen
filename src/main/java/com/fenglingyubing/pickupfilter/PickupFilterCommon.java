package com.fenglingyubing.pickupfilter;

import com.fenglingyubing.pickupfilter.settings.CommonSettings;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PickupFilterCommon {
    public static final Logger LOGGER = LogManager.getLogger("pickupfilter");
    private static volatile CommonSettings commonSettings;

    private PickupFilterCommon() {
    }

    public static void init(File configDir) {
        if (commonSettings != null) {
            return;
        }
        synchronized (PickupFilterCommon.class) {
            if (commonSettings != null) {
                return;
            }
            File dir = configDir == null ? new File(".") : configDir;
            CommonSettings settings = new CommonSettings(new File(dir, "pickupfilter-common.properties"));
            settings.load();
            commonSettings = settings;
        }
    }

    public static CommonSettings getCommonSettings() {
        CommonSettings settings = commonSettings;
        if (settings == null) {
            synchronized (PickupFilterCommon.class) {
                settings = commonSettings;
                if (settings == null) {
                    settings = new CommonSettings(null);
                    commonSettings = settings;
                }
            }
        }
        return settings;
    }
}
