package com.fenglingyubing.pickupfilter.settings;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CommonSettingsTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void loadsAndClampsAutoDestroySettings() throws Exception {
        File config = temp.newFile("pickupfilter-common.properties");

        Properties properties = new Properties();
        properties.setProperty("auto_destroy.scan_interval.min_ticks", "0");
        properties.setProperty("auto_destroy.scan_interval.max_ticks", "500");
        properties.setProperty("auto_destroy.scan.max_entities", "-1");
        properties.setProperty("auto_destroy.scan_backoff.empty_miss_threshold", "0");

        try (FileOutputStream out = new FileOutputStream(config)) {
            properties.store(out, "test");
        }

        CommonSettings settings = new CommonSettings(config);
        settings.load();

        assertEquals(1, settings.getAutoDestroyScanMinIntervalTicks());
        assertEquals(200, settings.getAutoDestroyScanMaxIntervalTicks());
        assertEquals(0, settings.getAutoDestroyScanMaxEntities());
        assertEquals(1, settings.getAutoDestroyEmptyBackoffMissThreshold());
    }

    @Test
    public void clampsMaxIntervalToBeAtLeastMinInterval() throws Exception {
        File config = temp.newFile("pickupfilter-common.properties");

        Properties properties = new Properties();
        properties.setProperty("auto_destroy.scan_interval.min_ticks", "10");
        properties.setProperty("auto_destroy.scan_interval.max_ticks", "5");

        try (FileOutputStream out = new FileOutputStream(config)) {
            properties.store(out, "test");
        }

        CommonSettings settings = new CommonSettings(config);
        settings.load();

        assertEquals(10, settings.getAutoDestroyScanMinIntervalTicks());
        assertEquals(10, settings.getAutoDestroyScanMaxIntervalTicks());
    }
}

