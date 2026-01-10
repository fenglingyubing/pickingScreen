package com.fenglingyubing.pickupfilter.client.settings;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientSettingsTest {

    @Test
    public void introShown_persistsToDisk() throws Exception {
        Path tempDir = Files.createTempDirectory("pickupfilter-client-settings");
        File file = tempDir.resolve("pickupfilter-client.properties").toFile();

        ClientSettings settings = new ClientSettings(file);
        settings.load();
        assertFalse(settings.isIntroShown());

        settings.markIntroShown();
        assertTrue(settings.isIntroShown());

        ClientSettings reloaded = new ClientSettings(file);
        reloaded.load();
        assertTrue(reloaded.isIntroShown());
    }
}

