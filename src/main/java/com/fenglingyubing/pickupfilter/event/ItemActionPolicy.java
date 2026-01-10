package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.config.FilterMode;

public final class ItemActionPolicy {
    private ItemActionPolicy() {
    }

    public static boolean shouldCancelPickup(FilterMode mode, boolean matchesFilter) {
        return mode == FilterMode.PICKUP_MATCHING && !matchesFilter;
    }

    public static boolean shouldDestroyDrop(FilterMode mode, boolean matchesFilter) {
        return mode == FilterMode.DESTROY_MATCHING && matchesFilter;
    }
}
