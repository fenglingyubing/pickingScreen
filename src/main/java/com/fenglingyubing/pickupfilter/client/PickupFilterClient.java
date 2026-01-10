package com.fenglingyubing.pickupfilter.client;

import com.fenglingyubing.pickupfilter.client.settings.ClientSettings;
import java.io.File;

public final class PickupFilterClient {
    private static volatile ClientSettings clientSettings;

    private PickupFilterClient() {
    }

    public static void init(File configDir) {
        if (clientSettings != null) {
            return;
        }

        File dir = configDir == null ? new File(".") : configDir;
        ClientSettings settings = new ClientSettings(new File(dir, "pickupfilter-client.properties"));
        settings.load();
        clientSettings = settings;
    }

    public static ClientSettings getClientSettings() {
        ClientSettings settings = clientSettings;
        if (settings == null) {
            settings = new ClientSettings(null);
            clientSettings = settings;
        }
        return settings;
    }
}

