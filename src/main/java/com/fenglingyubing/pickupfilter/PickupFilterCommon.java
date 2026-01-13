package com.fenglingyubing.pickupfilter;

import com.fenglingyubing.pickupfilter.settings.CommonSettings;
import java.io.File;

public final class PickupFilterCommon {
    private static volatile CommonSettings commonSettings;

    private PickupFilterCommon() {
    }

    public static void init(File configDir) {
        if (commonSettings != null) {
            return;
        }

        File dir = configDir == null ? new File(".") : configDir;
        CommonSettings settings = new CommonSettings(new File(dir, "pickupfilter-common.properties"));
        settings.load();
        commonSettings = settings;
    }

    public static CommonSettings getCommonSettings() {
        CommonSettings settings = commonSettings;
        if (settings == null) {
            settings = new CommonSettings(null);
            commonSettings = settings;
        }
        return settings;
    }
}

