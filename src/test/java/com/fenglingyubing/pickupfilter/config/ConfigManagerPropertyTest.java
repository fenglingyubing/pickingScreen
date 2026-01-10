package com.fenglingyubing.pickupfilter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

public class ConfigManagerPropertyTest implements WithQuickTheories {

    @Test
    public void defaultConfigGenerated() throws Exception {
        Path tempDir = Files.createTempDirectory("pickupfilter-config-test");
        File configFile = tempDir.resolve("pickupfilter.cfg").toFile();

        ConfigManager configManager = new ConfigManager();
        configManager.loadConfig(configFile);

        assertEquals(FilterMode.DISABLED, configManager.getCurrentMode());
        assertTrue(configManager.getFilterRules().isEmpty());
    }

    @Test
    public void property6_configPersistenceRoundTrip() {
        qt().withExamples(100)
                .forAll(modes(), rulesLists())
                .check((mode, rules) -> {
                    Path tempDir;
                    try {
                        tempDir = Files.createTempDirectory("pickupfilter-config-roundtrip");
                    } catch (Exception exception) {
                        return false;
                    }

                    File configFile = tempDir.resolve("pickupfilter.cfg").toFile();

                    try {
                        ConfigManager configManager = new ConfigManager();
                        configManager.loadConfig(configFile);
                        configManager.setCurrentMode(mode);
                        configManager.setFilterRules(rules);
                        configManager.saveConfig();

                        ConfigManager reloaded = new ConfigManager();
                        reloaded.loadConfig(configFile);
                        return mode == reloaded.getCurrentMode()
                                && rules.equals(reloaded.getFilterRules());
                    } catch (Exception exception) {
                        return false;
                    } finally {
                        try {
                            Files.deleteIfExists(configFile.toPath());
                            Files.deleteIfExists(tempDir);
                        } catch (Exception ignored) {
                        }
                    }
                });
    }

    private static Gen<FilterMode> modes() {
        return SourceDSL.arbitrary().pick(FilterMode.values());
    }

    private static Gen<List<FilterRule>> rulesLists() {
        return SourceDSL.lists().of(rules()).ofSizeBetween(0, 20);
    }

    private static Gen<FilterRule> rules() {
        Gen<String> randomModId = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 8);
        Gen<String> modId = SourceDSL.integers()
                .between(0, 9)
                .zip(randomModId, (pick, generated) -> pick == 0 ? FilterRule.ANY : generated);

        Gen<Boolean> wildcardEnabled = SourceDSL.booleans().all();
        Gen<String> baseItemName = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 12);
        Gen<String> itemName = wildcardEnabled.zip(baseItemName, (wildcard, base) -> {
            if (!wildcard) {
                return base;
            }
            int starAt = Math.max(1, base.length() / 2);
            return base.substring(0, starAt) + FilterRule.ANY + base.substring(starAt);
        });

        Gen<Integer> metadata = SourceDSL.integers().between(FilterRule.ANY_METADATA, 15);

        return modId.zip(itemName, (pickedModId, pickedItemName) -> new Object[]{pickedModId, pickedItemName})
                .zip(metadata, (pair, pickedMetadata) -> new Object[]{pair[0], pair[1], pickedMetadata})
                .zip(wildcardEnabled, (triple, pickedWildcardEnabled) ->
                        new FilterRule(
                                (String) triple[0],
                                (String) triple[1],
                                (Integer) triple[2],
                                pickedWildcardEnabled
                        ));
    }
}
