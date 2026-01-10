package com.fenglingyubing.pickupfilter.config;

public interface ModeSwitching {
    FilterMode getCurrentMode();

    FilterMode switchTo(FilterMode mode);

    FilterMode cycleToNextMode();
}
