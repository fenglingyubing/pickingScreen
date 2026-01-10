package com.fenglingyubing.pickupfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.ModeSwitching;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class PickupFilterComponentsIntegrationTest {

    @Test
    public void bootstrapsComponentsAndPersistsModeSwitching() throws Exception {
        Path tempDir = Files.createTempDirectory("pickupfilter-components-it");
        File configFile = tempDir.resolve("pickupfilter.cfg").toFile();

        try {
            PickupFilterComponents components = PickupFilterComponents.bootstrap(configFile);
            assertNotNull(components.getConfigManager());
            assertNotNull(components.getCommonEventHandler());
            assertNotNull(components.getModeSwitching());

            ModeSwitching modeSwitching = components.getModeSwitching();
            assertEquals(FilterMode.DISABLED, modeSwitching.getCurrentMode());

            FilterMode nextMode = modeSwitching.cycleToNextMode();
            assertEquals(nextMode, components.getConfigManager().getCurrentMode());

            ConfigManager reloaded = new ConfigManager();
            reloaded.loadConfig(configFile);
            assertEquals(nextMode, reloaded.getCurrentMode());
        } finally {
            try {
                Files.deleteIfExists(configFile.toPath());
                Files.deleteIfExists(tempDir);
            } catch (Exception ignored) {
            }
        }
    }
}
