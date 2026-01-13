package com.fenglingyubing.pickupfilter.settings;

import com.fenglingyubing.pickupfilter.event.DropClearArea;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class CommonSettingsTest {

    @Test
    public void clearDropsChunkRadius_defaultsAndPersists() throws Exception {
        Path tempDir = Files.createTempDirectory("pickupfilter-common-settings");
        File file = tempDir.resolve("pickupfilter-common.properties").toFile();

        CommonSettings settings = new CommonSettings(file);
        settings.load();
        assertEquals(DropClearArea.DEFAULT_CHUNK_RADIUS, settings.getClearDropsChunkRadius());

        CommonSettings reloaded = new CommonSettings(file);
        reloaded.load();
        assertEquals(DropClearArea.DEFAULT_CHUNK_RADIUS, reloaded.getClearDropsChunkRadius());
    }

    @Test
    public void clearDropsChunkRadius_isClamped() throws Exception {
        Path tempDir = Files.createTempDirectory("pickupfilter-common-settings-clamp");
        Path file = tempDir.resolve("pickupfilter-common.properties");

        Files.write(
                file,
                ("clear_drops.chunk_radius=999\n").getBytes(StandardCharsets.UTF_8)
        );
        CommonSettings settings = new CommonSettings(file.toFile());
        settings.load();
        assertEquals(16, settings.getClearDropsChunkRadius());

        Files.write(
                file,
                ("clear_drops.chunk_radius=-10\n").getBytes(StandardCharsets.UTF_8)
        );
        CommonSettings settings2 = new CommonSettings(file.toFile());
        settings2.load();
        assertEquals(0, settings2.getClearDropsChunkRadius());
    }
}

