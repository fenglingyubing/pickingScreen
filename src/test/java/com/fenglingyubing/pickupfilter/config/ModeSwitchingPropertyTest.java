package com.fenglingyubing.pickupfilter.config;

import org.junit.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

import static org.junit.Assert.assertEquals;

public class ModeSwitchingPropertyTest implements WithQuickTheories {

    @Test
    public void property1_modeSwitchingConsistency() {
        qt().withExamples(250)
                .forAll(modes(), SourceDSL.integers().between(0, 10))
                .checkAssert((startingMode, cycles) -> {
                    ConfigManager configManager = new ConfigManager(null);
                    configManager.setCurrentMode(startingMode);
                    ModeSwitching modeSwitching = new ConfigModeSwitching(configManager);

                    FilterMode expected = startingMode;
                    for (int i = 0; i < cycles; i++) {
                        expected = FilterModeCycle.next(expected);
                        FilterMode actual = modeSwitching.cycleToNextMode();
                        assertEquals(expected, actual);
                        assertEquals(expected, modeSwitching.getCurrentMode());
                    }
                });
    }

    private static Gen<FilterMode> modes() {
        return SourceDSL.arbitrary().pick(FilterMode.values());
    }
}
